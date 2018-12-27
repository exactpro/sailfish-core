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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.scriptrunner.languagemanager.AutoLanguageFactory;
import com.exactpro.sf.storage.IMatrix;
import com.exactpro.sf.testwebgui.BeanUtil;

@ManagedBean(name="scriptRunsBean")
@SessionScoped
@SuppressWarnings("serial")
public class ScriptRunsBean implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ScriptRunsBean.class);

    private volatile boolean continueOnFailed;
    private volatile boolean autoStart;
    private volatile boolean ignoreAskForContinue;
    private volatile boolean tryReloadBeforeStart;
    private volatile boolean runNetDumper;
    private volatile boolean skipOptional;
    private volatile String selectedEnvironment;
    private volatile SailfishURI selectedLanguageURI;
    private volatile String selectedEncoding;

    // store matrixId in SessionScoped bean (survive page reload)
    private volatile Long matrixToEditId;
    private List<MatrixAdapter> matrixAdapterList;
    private Map<String, String> environmentValues;
    private volatile List<Tag> tags = new ArrayList<>();

    private volatile String testResultFilterString;
    private volatile String sortBy;
    private volatile Set<String> selectedStatuses;
    private volatile Date dateFrom;
    private volatile Date dateTo;

    public ScriptRunsBean() {
        this.selectedLanguageURI = AutoLanguageFactory.URI;
        this.selectedEnvironment = ServiceName.DEFAULT_ENVIRONMENT;
    }

    @PostConstruct
    public void init() {
        matrixAdapterList = new ArrayList<>();
        MatrixHolder holder = getMatrixHolder();

        if(holder != null) {
            List<IMatrix> matrixList = holder.getMatrices();
            for(IMatrix matrix: matrixList){
                MatrixAdapter matrixAdapter = new MatrixAdapter(matrix.getId());
                matrixAdapterList.add(matrixAdapter);
            }
        }
    }

    public boolean isContinueOnFailed() {
        return continueOnFailed;
    }

    public void setContinueOnFailed(boolean continueOnFailed) {
        this.continueOnFailed = continueOnFailed;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public boolean isIgnoreAskForContinue() {
        return ignoreAskForContinue;
    }

    public void setIgnoreAskForContinue(boolean ignoreAskForContinue) {
        this.ignoreAskForContinue = ignoreAskForContinue;
    }

    public boolean isTryReloadBeforeStart() {
        return tryReloadBeforeStart;
    }

    public void setTryReloadBeforeStart(boolean tryReloadBeforeStart) {
        this.tryReloadBeforeStart = tryReloadBeforeStart;
    }


    public String getSelectedEnvironment() {
        return selectedEnvironment;
    }

    public void setSelectedEnvironment(String selectedEnvironment) {
        this.selectedEnvironment = selectedEnvironment;
    }

    public SailfishURI getSelectedLanguageURI() {
        return selectedLanguageURI;
    }

    public void setSelectedLanguageURI(SailfishURI selectedLanguageURI) {
        this.selectedLanguageURI = selectedLanguageURI;
    }

    public String getSelectedEncoding() {
        return selectedEncoding;
    }

    public void setSelectedEncoding(String selectedEncoding) {
        this.selectedEncoding = selectedEncoding;
    }

    public synchronized List<MatrixAdapter> getMatrixAdapterList() {
        syncMatrixAdapterList();
        List<MatrixAdapter> newMatrixAdapterList = new ArrayList<MatrixAdapter>();
        for(MatrixAdapter matrixAdapter: matrixAdapterList){
            try {
                newMatrixAdapterList.add(matrixAdapter.clone());
            } catch (CloneNotSupportedException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return newMatrixAdapterList;
    }

    public synchronized void onChangeMatrixAdapter(MatrixAdapter matrixAdapter){
        logger.debug("onChangeMatrixAdapter started with {}", matrixAdapter);
        Collections.replaceAll(matrixAdapterList, matrixAdapter, matrixAdapter);
    }

    public synchronized void onRowReorder(int from, int to) {
        int delta = from < to ? 0 : 1;
        MatrixAdapter matrixAdapter = matrixAdapterList.get(from);
        matrixAdapterList.add(to + Math.abs(delta - 1), matrixAdapter);
        matrixAdapterList.remove(from + delta);
    }

    protected void syncMatrixAdapterList(){
        //is deleted
        MatrixHolder holder = getMatrixHolder();
        if(holder == null) {
            return;
        }

        List<IMatrix> matrixList = holder.getMatrices();
        Iterator<MatrixAdapter> iterator = matrixAdapterList.iterator();
        while(iterator.hasNext()){
            MatrixAdapter matrixAdapter = iterator.next();
            IMatrix matrix = holder.getMatrixById(matrixAdapter.getMatrixId());
            if(matrix == null){
                iterator.remove();
            }
        }
        //is added
        for(IMatrix matrix: matrixList){
            if(!containsMatrixAdapter(matrix.getId())){
                MatrixAdapter matrixAdapter = new MatrixAdapter(matrix.getId());
                matrixAdapterList.add(0, matrixAdapter);
            }
        }
    }

    protected boolean containsMatrixAdapter(Long id){
        for(MatrixAdapter matrixAdapter: matrixAdapterList){
            if(matrixAdapter.getMatrixId().equals(id)){
                return true;
            }
        }
        return false;
    }

    public MatrixHolder getMatrixHolder() {
        return BeanUtil.getMatrixHolder();
    }

    public Long getMatrixToEditId() {
        return matrixToEditId;
    }

    public void setMatrixToEditId(Long matrixToEditId) {
        this.matrixToEditId = matrixToEditId;
    }

    public Map<String, String> getEnvironmentValues() {
        return environmentValues;
    }

    public void setEnvironmentValues(Map<String, String> environmentValues) {
        this.environmentValues = environmentValues;
    }

	public List<Tag> getTags() {
		return tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

    public boolean isRunNetDumper() {
		return runNetDumper;
	}
	public void setRunNetDumper(boolean runNetDumper) {
		this.runNetDumper = runNetDumper;
	}

    public boolean isSkipOptional() {
        return skipOptional;
    }

    public void setSkipOptional(boolean skipOptional) {
        this.skipOptional = skipOptional;
    }

    public String getTestResultFilterString() {
        return testResultFilterString;
    }

    public void setTestResultFilterString(String testResultFilterString) {
        this.testResultFilterString = testResultFilterString;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public Set<String> getSelectedStatuses() {
        return selectedStatuses;
    }

    public void setSelectedStatuses(Set<String> selectedStatuses) {
        this.selectedStatuses = selectedStatuses;
    }

    public Date getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Date dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public void setDateTo(Date dateTo) {
        this.dateTo = dateTo;
    }
}
