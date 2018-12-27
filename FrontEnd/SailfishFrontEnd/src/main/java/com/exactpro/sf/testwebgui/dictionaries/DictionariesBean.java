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
package com.exactpro.sf.testwebgui.dictionaries;

import java.io.Serializable;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.primefaces.event.CloseEvent;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import com.exactpro.sf.testwebgui.BeanUtil;

@SuppressWarnings("serial")
@ManagedBean(name="dictBean")
@SessionScoped
public class DictionariesBean implements Serializable {

	private transient UploadedFile uploadFile;
	
    private transient DictionaryEditorModel model;

    public UploadedFile getUploadFile() {
        return uploadFile;
    }

    public void setUploadFile(UploadedFile uploadFile) {
        this.uploadFile = uploadFile;
    }

    public void preUploadClick() {
        this.uploadFile = null;

        getModel().preUploadClick();
    }

    public void handleFileUpload(FileUploadEvent event) {

        this.uploadFile = event.getFile();

        if (!getModel().handleFileUpload(this.uploadFile)) {
            this.uploadFile = null;
        }
    }

    public void handleFileUpload() {
        getModel().handleFileUploadProcess(this.uploadFile);
    }

    public void uploadDialogClose(CloseEvent event) {
        this.uploadFile = null;
        this.model.uploadDialogClose();
    }

    public DictionaryEditorModel getModel() {

        if (model == null) {
            model = BeanUtil.getSessionModelsMapper().getDictionaryEditorModel(
                    (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true));
        }

        return model;
    }
    
    @PreDestroy
    public void preDestroy() {
        BeanUtil.getSessionModelsMapper().destroyModel(this.model);
    }
}