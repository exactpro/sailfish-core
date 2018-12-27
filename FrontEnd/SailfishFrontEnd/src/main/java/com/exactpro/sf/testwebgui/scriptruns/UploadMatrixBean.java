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
package com.exactpro.sf.testwebgui.scriptruns;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.primefaces.event.FileUploadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.iomatrix.MatrixFileTypes;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.matrixhandlers.IMatrixProviderFactory;
import com.exactpro.sf.matrixhandlers.MatrixProviderHolder;
import com.exactpro.sf.storage.IMatrix;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;

@ManagedBean(name = "uploadMatrixBean")
@ViewScoped
public class UploadMatrixBean implements Serializable {

    private static final long serialVersionUID = -3447265343472567614L;

    private static final Logger logger = LoggerFactory.getLogger(UploadMatrixBean.class);

    private SailfishURI defaultProviderURI;

    @ManagedProperty("#{testScriptsBean}")
    private TestScriptsBean testScriptsBean;

    private Long matrixId;

    private Map<SailfishURI, String> linkTypes;

    private List<MatrixWrapper> wrappers;

    public UploadMatrixBean() {
        wrappers = Collections.synchronizedList(new ArrayList<MatrixWrapper>());
    }

    private MatrixProviderHolder getMatrixProviderHolder() {
        return BeanUtil.findBean(BeanUtil.MATRIX_PROVIDER_HOLDER, MatrixProviderHolder.class);
    }

    @PostConstruct
    public void init() {

        MatrixProviderHolder matrixProviderHolder = getMatrixProviderHolder();
        Set<SailfishURI> matrixProviderURIs = matrixProviderHolder.getProviderURIs();
        defaultProviderURI = matrixProviderURIs.isEmpty() ? null : matrixProviderURIs.iterator().next();

        // Google spreadsheet, Local, FTP, Internet link...
    	linkTypes = new LinkedHashMap<>();
    	for (SailfishURI providerURI : matrixProviderURIs) {
    		IMatrixProviderFactory factory = matrixProviderHolder.getMatrixProviderFactory(providerURI);
    		linkTypes.put(providerURI, factory.getHumanReadableName());
    	}
    }

    public void uploadWrappers() {

        if (wrappers.isEmpty()) {
            return;
        }

        List<MatrixWrapper> locals = new ArrayList<>();
        List<MatrixWrapper> linked = new ArrayList<>();

        synchronized (wrappers) {
            for (MatrixWrapper wrapper : wrappers) {
                if (wrapper.getPath() != null) {
                    if (wrapper.getType() == MatrixType.TYPE_LOCAL) {
                        locals.add(wrapper);
                    } else if (wrapper.getType() == MatrixType.TYPE_LINKED) {
                        linked.add(wrapper);
                    } else {
                        logger.warn("Unknown matrix type: {}, upload skipped", wrapper.getType());
                    }
                }
            }

            wrappers.clear();
        }

        if (!locals.isEmpty()) {
            logger.debug("Upload local matrix invoked {}", BeanUtil.getUser());

            List<String> names = new ArrayList<>();
            Map<String, Exception> uploadProblems = new HashMap<>();

            for (MatrixWrapper wrapper : locals) {
                try (InputStream wrapperInputStream = wrapper.getMatrixInputStream()) {
                    TestToolsAPI.getInstance().uploadMatrix(wrapperInputStream,
                            wrapper.getMatrixName(), null, "Unknown creator", null, null, null);
                    names.add(wrapper.getMatrixName());
                } catch (IOException e) {
                    logger.warn("Matrix input stream closing failed, matrix name: {}", wrapper.getMatrixName());
                    uploadProblems.put(wrapper.getMatrixName(), e);
                } catch (RuntimeException e) {
                    logger.error("Matrix upload failed, matrix name: {}", wrapper.getMatrixName(), e);
                    uploadProblems.put(wrapper.getMatrixName(), e);
                }
            }

            names.removeAll(uploadProblems.keySet());
            logger.info("Upload local matrix finished successfully. User : {}, Names : {}", BeanUtil.getUser(), names);
            uploadProblems.forEach((k, v) ->  BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Got a problem while uploading " + k, v.getMessage()));
        }

        if (!linked.isEmpty()) {
            logger.debug("Upload linked matrix invoked {}", BeanUtil.getUser());
            List<String> links = new ArrayList<>();

            for (MatrixWrapper wrapper : linked) {
                try {
                    MatrixUtil.addMatrixByLink(getMatrixProviderHolder(), wrapper.getPath(), wrapper.getProviderURI());
                    links.add(wrapper.getPath());
                } catch (Exception e) {
                    String message = "Could not store uploaded file: " + e.getMessage();
                    BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, message, "Check path spelling or access permissions");
                    logger.error(message, e);
                    return;
                }
            }

            logger.info("Upload linked matrix finished successfully. User : {}, Links : {}", BeanUtil.getUser(), links);
        }
    }

    public boolean isUploadButtonDisabled() {
        return wrappers.isEmpty();
    }

    public void removeWrapper(int index) {
        wrappers.remove(index);
    }

    public void clearWrappers() {
        wrappers.clear();
    }

    public void addFewLocalMatrix(FileUploadEvent event) {

        MatrixWrapper wrapper = new MatrixWrapper();

        try {
            String fileName = event.getFile().getFileName();
            MatrixFileTypes fileType = MatrixFileTypes.detectFileType(fileName);
            if (fileType == MatrixFileTypes.CSV || fileType == MatrixFileTypes.XLS
                    || fileType == MatrixFileTypes.XLSX) {
                wrapper.setMatrixInputStream(event.getFile().getInputstream(), fileName);
                wrappers.add(0, wrapper);
            } else {
                String message = fileName + " - Invalid file type";
                BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, message, "Only csv|xls|xlsx file allowed.");
            }
        } catch (IOException e) {
            wrapper.setPath(null);
            String message = "Could not store uploaded file: " + e.getMessage();
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, message, "Check path spelling or access permissions");
            logger.error(message, e);
        }
    }

    public void addWrapperForLinkedMatrix() {

        this.wrappers.add(0, new MatrixWrapper(MatrixType.TYPE_LINKED, defaultProviderURI));
    }

    public void reloadMatrixByLink() {
        logger.info("Reload started for matrix with [{}] id", this.matrixId);

        IMatrix matrix = BeanUtil.getMatrixHolder().getMatrixById(this.matrixId);

        if(matrix == null) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Matrix not found", "");
            return;
        }
        try {
            MatrixUtil.reloadMatrixByLink(BeanUtil.getSfContext().getMatrixStorage(), getMatrixProviderHolder(), matrix);
        } catch (Exception e) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), "");
            logger.error(e.getMessage(), e);
        }
    }


    public void setMatrixId(Long matrixId) {
        this.matrixId = matrixId;
    }

    public Map<SailfishURI, String> getLinkTypes() {
        return linkTypes;
    }

    public String getWarningValue(int index) {
        String message = "";
        SailfishURI providerURI = wrappers.get(index).getProviderURI();
        if (providerURI != null) {
            IMatrixProviderFactory factory = getMatrixProviderHolder().getMatrixProviderFactory(providerURI);
            if (factory != null) {
                message = factory.getNotes();
            }
        }
        return message;
    }

    public TestScriptsBean getTestScriptsBean() {
        return testScriptsBean;
    }

    public void setTestScriptsBean(TestScriptsBean testScriptsBean) {
        this.testScriptsBean = testScriptsBean;
    }

    public List<MatrixWrapper> getWrappers() {
        return wrappers;
    }

    public void setWrappers(List<MatrixWrapper> wrappers) {
        this.wrappers = wrappers;
    }

}
