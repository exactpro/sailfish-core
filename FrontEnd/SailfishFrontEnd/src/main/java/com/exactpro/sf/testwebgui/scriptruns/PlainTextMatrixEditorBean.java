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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.primefaces.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.iomatrix.AdvancedMatrixReader;
import com.exactpro.sf.aml.iomatrix.AdvancedMatrixWriter;
import com.exactpro.sf.aml.iomatrix.CSVMatrixReader;
import com.exactpro.sf.aml.iomatrix.CSVMatrixWriter;
import com.exactpro.sf.aml.iomatrix.IMatrixReader;
import com.exactpro.sf.aml.iomatrix.IMatrixWriter;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.storage.IMatrix;
import com.exactpro.sf.testwebgui.BeanUtil;

@ViewScoped
@ManagedBean(name = "plainTextEditorBean", eager = true)
public class PlainTextMatrixEditorBean implements Serializable {

	private static final long serialVersionUID = 8163712043639871248L;

	private static final Logger logger = LoggerFactory.getLogger(MatrixEditorBean.class);

	private static final String charset = "UTF-8";

	@ManagedProperty(value = "#{testScriptsBean.matrixToEdit}")
	private MatrixAdapter matrixAdapter;

	private String textToSave = "";

	public PlainTextMatrixEditorBean() {
	}

	@PostConstruct
	public void initialize() {
		if (matrixAdapter == null) {
			logger.error("matrix not loaded from managed bean");
			throw new NullPointerException("matrix is null");
		}
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

	public void getText() {
		try {
			RequestContext context = RequestContext.getCurrentInstance();

			IMatrix matrix = matrixAdapter.getIMatrix();

            File matrixFile = new File(BeanUtil.getSfContext().getWorkspaceDispatcher().getFolder(FolderType.MATRIX), matrix.getFilePath());

			try (IMatrixReader reader = AdvancedMatrixReader.getReader(matrixFile);
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				CSVMatrixWriter writer = new CSVMatrixWriter(os)) {

				while (reader.hasNext()) {
					String[] line = reader.read();
					writer.write(line);
				}
				writer.close();
				context.addCallbackParam("text", new String(Base64.encodeBase64(os.toByteArray())));
			}


			BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Info", "Matrix has been loaded");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Matrix has not been loaded");
		}
	}

	public void saveText() {
		try {
			IMatrix matrix = matrixAdapter.getIMatrix();
            matrix.writeStream(BeanUtil.getSfContext().getWorkspaceDispatcher(), IOUtils.toInputStream(textToSave, charset));


            File matrixFile = new File(BeanUtil.getSfContext().getWorkspaceDispatcher().getFolder(FolderType.MATRIX), matrix.getFilePath());

			try (InputStream is = new ByteArrayInputStream(textToSave.getBytes());
				IMatrixReader reader = new CSVMatrixReader(is);
				IMatrixWriter writer = AdvancedMatrixWriter.getWriter(matrixFile)) {

				while (reader.hasNext()) {
					String[] line = reader.read();
					writer.write(line);
				}
			}

			BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "info", "Matrix has been saved");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", "Matrix has not been saved");
		}
	}

	public String getTextToSave() {
		return textToSave;
	}

	public void setTextToSave(String textToSave) {
		this.textToSave = textToSave;
	}
}
