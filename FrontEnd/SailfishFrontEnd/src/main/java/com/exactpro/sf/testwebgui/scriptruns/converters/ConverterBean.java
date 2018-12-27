/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.exactpro.sf.testwebgui.scriptruns.converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.beanutils.BeanUtils;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.event.CellEditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.converter.ConversionMonitor;
import com.exactpro.sf.aml.converter.IMatrixConverter;
import com.exactpro.sf.aml.converter.IMatrixConverterSettings;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;
import com.exactpro.sf.testwebgui.scriptruns.MatrixConverterFeature;

@ManagedBean(name = "converterBean")
@ViewScoped
@SuppressWarnings("serial")
public class ConverterBean implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ConverterBean.class);

    private MatrixConverterFeature task;

    private Long matrixId;

    private List<String> envNames;

    private ServiceName[] serviceNames; // All service names, for converter dialog

    private List<String> envServices;

    private SailfishURI converterUri;

    private String environment = "default";

    private String outputMatrixName;

    private String defaultPrefix = "";

    private ConverterModel model;

    private ConverterNode currentSettings;

    public ConverterBean() {
        logger.debug("EnvironmentBean creation started.");

        this.serviceNames = BeanUtil.getSfContext().getConnectionManager().getServiceNames();

        envServices = new ArrayList<>();
        updateServicesNames(null);

        TestToolsAPI testToolsAPI = TestToolsAPI.getInstance();

        model = new ConverterModel(testToolsAPI.getMatrixConverters());
        this.envNames = testToolsAPI.getEnvNames();

        Arrays.sort(this.serviceNames);
    }

    public void addToMapping(ConverterFormMapAdapter adapter) {
        adapter.add(new MappingContainer());
    }

    public void removeRow(ConverterFormMapAdapter adapter, MappingContainer container) {
        adapter.remove(container);
    }

    public void updateServicesNames(ValueChangeEvent event) {
        envServices.clear();
        if(event != null){
            environment = (String) event.getNewValue();
        }

        for (ServiceName serviceName : serviceNames) {
            if (serviceName.getEnvironment().equals(environment)) {
                envServices.add(serviceName.getServiceName());
            }
        }
    }

    public void changeConverterSettings(ValueChangeEvent event){
        this.converterUri = (SailfishURI) event.getNewValue();
        currentSettings = model.getSettings(converterUri);
        defaultPrefix = converterUri.getResourceName();
        this.outputMatrixName = defaultPrefix +  BeanUtil.getSfContext().getMatrixStorage().getMatrixById(matrixId).getName();
    }

    private static final class ConversionTask implements Callable<Boolean> {
        private final IMatrixConverter converter;
        private final IMatrixConverterSettings settings;
        private final ConversionMonitor monitor;

        public ConversionTask(IMatrixConverter converter, IMatrixConverterSettings settings, ConversionMonitor monitor) {
            this.converter = converter;
            this.settings = settings;
            this.monitor = monitor;
        }

        @Override
        public Boolean call() throws Exception {
            return this.converter.convert(settings, monitor);
        }

    }

    public void convertMatrix() {
        logger.debug("convert started");
        try {
            TestToolsAPI testToolsAPI = TestToolsAPI.getInstance();
            IMatrixConverter converter = testToolsAPI.getMatrixConverter(converterUri);
            ConversionMonitor monitor = new ConversionMonitor();
            IMatrixConverterSettings settings = testToolsAPI
                    .prepareConverterSettings(matrixId, environment, converterUri, outputMatrixName);
            BeanMap beanMap = new BeanMap(settings);

            for (ConverterNode setting : currentSettings.getNodes()){
                if(beanMap.get(setting.getName()) instanceof Map){
                    ConverterFormMapAdapter adapter = (ConverterFormMapAdapter) setting.getValue();
                    adapter.toMap();
                    BeanUtils.setProperty(settings, setting.getName(), adapter);
                } else {
                    BeanUtils.setProperty(settings, setting.getName(), setting.getValue());
                }
            }

            Future<Boolean> converterTask = BeanUtil.getSfContext().getTaskExecutor().addTask(new ConversionTask(converter, settings, monitor));
            task = new MatrixConverterFeature(monitor, converterTask, settings.getOutputFile());
        } catch(RuntimeException | FileNotFoundException | WorkspaceStructureException | IllegalAccessException | InvocationTargetException e) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), "");
            logger.error(e.getMessage(), e);
        }
    }

    public Integer getProgress() {
        if (task != null) {
            if (task.getFuture().isDone()) {
                return 100;
            }
            return task.getProgress();
        } else {
            return 0;
        }
    }

    public void cancelConvertMatrix() {
        if (task != null) {
            logger.debug("convert cancel");
            try {
                task.getFuture().cancel(true);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            task = null;
        }
    }

    public void onComplete() {
        if (task != null) {
            Set<String> errors = new HashSet<>();
            try {
                task.getFuture().get();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                errors.add(e.getMessage());
            } finally {
                errors.addAll(task.errors());
            }
            for (String errorMessage : errors) {
                BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, errorMessage, "");
            }
            File newMatrixFile = this.task.getOutputFile();
            if (newMatrixFile.exists()) {
                String newName = newMatrixFile.getName();
                try (InputStream matrixInputStream = new FileInputStream(newMatrixFile)) {
                    TestToolsAPI.getInstance()
                            .uploadMatrix(matrixInputStream, newName, null, "Converter", SailfishURI.unsafeParse("AML_v3"), null, null);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            task = null;
            logger.debug("Convert finished {}", newMatrixFile.getName());
        }
    }

    public void onCellEdit(CellEditEvent event) {
        event.getNewValue();
        DataTable table = (DataTable) event.getSource();
        MappingContainer container = (MappingContainer) table.getRowData();
        if(container.haveError()){
            BeanUtil.addErrorMessage("Invalid mapping selection", "Select from/to must be different");
            container.revertValue((String) event.getOldValue(), table.getColumns().indexOf(event.getColumn()) == 0);
        }
    }

    public boolean hasMatrixConverter() {
        return TestToolsAPI.getInstance().hasConverter();
    }

    public Set<SailfishURI> getConverters() {
        return TestToolsAPI.getInstance().getMatrixConverters();
    }

    public void setMatrixId(Long matrixId) {
        this.matrixId = matrixId;
        if(currentSettings != null){
            this.outputMatrixName = defaultPrefix +  BeanUtil.getSfContext().getMatrixStorage().getMatrixById(matrixId).getName();
        }
    }

    public SailfishURI getConverterUri() {
        return converterUri;
    }

    public void setConverterUri(SailfishURI converterUri) {
        this.converterUri = converterUri;
    }

    public List<String> getEnvNames() {
        return envNames;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public List<String> getEnvServices() {
        return envServices;
    }

    public void setEnvServices(List<String> envServices) {
        this.envServices = envServices;
    }

    public ConverterModel getModel() {
        return model;
    }

    public ConverterNode getCurrentSettings() {
        return currentSettings;
    }

    public String getOutputMatrixName() {
        return outputMatrixName;
    }

    public void setOutputMatrixName(String outputMatrixName) {
        this.outputMatrixName = outputMatrixName;
    }
}
