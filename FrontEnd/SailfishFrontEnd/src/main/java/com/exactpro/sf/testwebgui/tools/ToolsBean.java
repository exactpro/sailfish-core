/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 ******************************************************************************/

package com.exactpro.sf.testwebgui.tools;

import com.exactpro.sf.aml.iomatrix.MergeMatrix;
import com.exactpro.sf.testwebgui.BeanUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.entity.ContentType;
import org.jetbrains.annotations.NotNull;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@ManagedBean(name = "toolsBean")
@ViewScoped
public class ToolsBean {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));

    private List<File> uploadedFiles = new ArrayList<>();

    public void handleFileUpload(FileUploadEvent event) {
        logger.info("{} has been uploaded", event.getFile().getFileName());
        uploadedFiles.add(getFile(event.getFile()));
    }

    public StreamedContent getConverted() {

        try {
            File outputFile = Files.createTempFile("merged", ".csv").toFile();
            MergeMatrix.mergeMatrix(outputFile, uploadedFiles);
            for (File matrixFile: uploadedFiles) {
                matrixFile.delete();
            }
            uploadedFiles.clear();

            return new DefaultStreamedContent(new FileInputStream(outputFile), ContentType.DEFAULT_BINARY.toString(), outputFile.getName());
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

    public List<File> getUploadedFiles() {
        return uploadedFiles;
    }

    public void clearUploads() {
        uploadedFiles.clear();
    }

    @NotNull
    private File getFile(UploadedFile uploadedFile) {
        try {
            Path tmpFile = Files.createTempFile(FilenameUtils.getBaseName(uploadedFile.getFileName()), "." + FilenameUtils.getExtension(uploadedFile.getFileName()));
            Files.copy(uploadedFile.getInputstream(), tmpFile, StandardCopyOption.REPLACE_EXISTING);
            return tmpFile.toFile();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), ExceptionUtils.getStackTrace(e));
            return null;
        }
    }
}
