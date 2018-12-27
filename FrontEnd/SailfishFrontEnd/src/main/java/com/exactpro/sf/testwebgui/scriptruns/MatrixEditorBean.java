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
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.apache.commons.codec.binary.Base64;
import org.primefaces.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.storage.IMatrix;
import com.exactpro.sf.testwebgui.BeanUtil;

@ViewScoped
@ManagedBean(name = "matrixEditorBean", eager = true)
public class MatrixEditorBean implements Serializable {

	private static final long serialVersionUID = -8163737043634631248L;

	private static final Logger logger = LoggerFactory.getLogger(MatrixEditorBean.class);

	@ManagedProperty(value = "#{testScriptsBean.matrixToEdit}")
	private MatrixAdapter matrixAdapter;

	private String testcaseToEdit = "";
	private String testcaseName = "";
	private String textToSave = "";
	private List<String> testcaseNames = new ArrayList<>();
	private transient JSONMatrixEditor editor = null;
	
	
	public MatrixEditorBean() {
	}

	@PostConstruct
	public void initialize() {
		if (matrixAdapter == null) {
			logger.error("matrix not loaded from managed bean");
			throw new NullPointerException("matrix is null");
		}
		
		// Init:
		IMatrix matrix = matrixAdapter.getIMatrix();
		this.editor = new JSONMatrixEditor(matrix);
		updateTestscriptsList();
		
		logger.debug("MatrixEditorBean for {} created", matrixAdapter.getMatrixId());
	}

	@PreDestroy
	public void destroy() {
		logger.debug("MatrixEditorBean for {} destroyed", matrixAdapter.getMatrixId());
	}

	public MatrixAdapter getMatrixAdapter() {
		return matrixAdapter;
	}

	public void setMatrixAdapter(MatrixAdapter matrixAdapter) {
		this.matrixAdapter = matrixAdapter;
	}

	public Long getMatrixId() {
		return matrixAdapter.getMatrixId();
	}
	
	public void loadTestCase() throws Exception {
		try {
			RequestContext context = RequestContext.getCurrentInstance();
			context.addCallbackParam(
					"text",
					new String(Base64.encodeBase64(editor.toJSON(testcaseToEdit).getBytes()))
			);
			BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Info", "Matrix has been loaded");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Matrix has not been loaded");
		}
	}
	
	public void saveTestCase() throws Exception {
    	try {
    	    editor.fromJSON(textToSave, testcaseToEdit);
    		editor.flush();
    		  BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "info", "Matrix has been saved");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Matrix has not been saved");
        }
	}

	public void createTestcase() {
		try {
			editor.addTestcase(testcaseName);
			updateTestscriptsList();
			testcaseName = "";
			BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Info", "Test case has been added");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Test case has not been added");
		}
	}
	
	public void removeTestcase() {
		try {
			editor.removeTestcase(testcaseToEdit);
			updateTestscriptsList();
			BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Info", "Test case has been removed");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Test case has not been removed");
		}
	}
	
	private void updateTestscriptsList() {
		testcaseNames.clear();
		testcaseNames.addAll(editor.getTestcaseNames());
	}

	/* Getters/Setters */
	public String getTestcaseToEdit() {
		return testcaseToEdit;
	}

	public void setTestcaseToEdit(String testcaseToEdit) {
		this.testcaseToEdit = testcaseToEdit;
	}
	
	public String getTestcaseName() {
		return testcaseName;
	}

	public void setTestcaseName(String testcaseName) {
		this.testcaseName = testcaseName;
	}

	public List<String> getTestcaseNames() {
		return testcaseNames;
	}

	public String getTextToSave() {
		return textToSave;
	}

	public void setTextToSave(String textToSave) {
		this.textToSave = textToSave;
	}

}
