/******************************************************************************
 * Copyright 2009-2022 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.testwebgui.tools;

import com.exactpro.sf.aml.iomatrix.ConvertMatrix;
import com.exactpro.sf.aml.iomatrix.MatrixFileTypes;
import com.exactpro.sf.testwebgui.BeanUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.util.StringUtil;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.UploadedFile;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@ManagedBean(name = "convertMatrixBean")
@SessionScoped
public class ConvertMatrixBean {

    private static final List<MatrixFileTypes> ALLOWED_TYPES;
    private static final String ALLOWED_TYPES_REGEX;
    private static final String ALLOWED_TYPES_MESSAGE;

    static {
        ALLOWED_TYPES = new ArrayList<MatrixFileTypes>(MatrixFileTypes.SUPPORTED_MATRIX_FILE_TYPES) {{
            Collections.sort(this);
        }};
        List<String> allowedTypesNames = ALLOWED_TYPES.stream()
                .map(MatrixFileTypes::getExtension)
                .collect(Collectors.toList());
        ALLOWED_TYPES_REGEX = String.format("/(\\.|\\/)(%s)$/", String.join("|", allowedTypesNames));
        ALLOWED_TYPES_MESSAGE = String.format("Allowed file types: %s", String.join(", ", allowedTypesNames));
    }

    private final Map<Long, ConvertibleFileWrapper> uploadedFiles = new TreeMap<>();
    private final List<ConvertibleFileWrapper> temporaryFiles = new ArrayList<>();
    private final List<ConvertibleFileWrapper> selectedFiles = new ArrayList<>();


    public String getAllowedTypesRegex() {
        return ALLOWED_TYPES_REGEX;
    }

    public String getAllowedTypesMessage() {
        return ALLOWED_TYPES_MESSAGE;
    }

    public void handleTemporaryFileUpload(FileUploadEvent event) {
        try {
            temporaryFiles.add(new ConvertibleFileWrapper(event.getFile()));
        } catch (IOException e) {
            showErrorMessage("Can't Upload a Matrix", e.getMessage());
        }
    }

    public boolean isTemporaryFilesEmpty() {
        return temporaryFiles.isEmpty();
    }

    public void uploadTemporaryFiles() {
        for(ConvertibleFileWrapper fileWrapper: temporaryFiles) {
            uploadedFiles.put(fileWrapper.getIndex(), fileWrapper);
        }
        temporaryFiles.clear();
    }

    public void removeTemporaryFile(ConvertibleFileWrapper fileWrapper) {
        temporaryFiles.remove(fileWrapper);
    }

    public void clearTemporaryFiles() {
        temporaryFiles.clear();
    }

    public List<ConvertibleFileWrapper> getTemporaryFiles() {
        return temporaryFiles;
    }

    public List<ConvertibleFileWrapper> getUploadedFiles() {
        return new ArrayList<>(uploadedFiles.values());
    }

    public void setSelectedFiles(List<ConvertibleFileWrapper> selectedFiles) {
        this.selectedFiles.clear();
        this.selectedFiles.addAll(selectedFiles);
    }

    public List<ConvertibleFileWrapper> getSelectedFiles() {
        return selectedFiles;
    }

    public boolean isSelectedFilesEmpty() {
        return selectedFiles.isEmpty();
    }

    public void deleteSelectedFiles() {
        for(ConvertibleFileWrapper fileWrapper: selectedFiles) {
            uploadedFiles.remove(fileWrapper.getIndex());
        }
        selectedFiles.clear();
    }

    public List<MatrixFileTypes> getCommonTypes() {
        if(selectedFiles.isEmpty()) {
            return Collections.emptyList();
        }

        Set<MatrixFileTypes> commons = new HashSet<>(ALLOWED_TYPES);
        for(ConvertibleFileWrapper fileWrapper: selectedFiles) {
            commons.retainAll(fileWrapper.getPossibleFileTypes());
        }
        return new ArrayList<MatrixFileTypes>(commons) {{
            Collections.sort(this);
        }};
    }

    public DefaultStreamedContent convertToZip() {
        try {
            Map<String, Map<MatrixFileTypes, int[]>> frequency = new HashMap<>();
            String prefix = String.format("converted-%s.zip", getDateForFileName());
            File zipFile = Files.createTempFile(prefix, null).toFile();
            ZipOutputStream zipStream = new ZipOutputStream(Files.newOutputStream(zipFile.toPath()));

            for (ConvertibleFileWrapper file : selectedFiles) {
                String fileName = file.getNewFileName();
                MatrixFileTypes fileType = file.getNewFileType();
                if (!frequency.containsKey(fileName)) {
                    frequency.put(fileName, new HashMap<>());
                }
                if (!frequency.get(fileName).containsKey(fileType)) {
                    frequency.get(fileName).put(fileType, new int[]{0});
                }

                int[] freq = frequency.get(fileName).get(fileType);
                StringBuilder entryNameBuilder = new StringBuilder(fileName);
                if (freq[0] != 0) {
                    entryNameBuilder.append(" (").append(freq[0]).append(")");
                }
                freq[0]++;
                entryNameBuilder.append('.').append(fileType.getExtension());
                zipStream.putNextEntry(new ZipEntry(entryNameBuilder.toString()));
                zipStream.write(readBytes(file.convert()));
                zipStream.closeEntry();
            }

            zipStream.close();
            return fileToDefaultStreamedContent(zipFile, prefix, StringUtil.UTF8.toString());
        } catch (Exception e) {
            showErrorMessage("Can't Create a Zip File", e.getMessage());
            return null;
        }
    }

    private static byte[] readBytes(File file) throws IOException {
        try(FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fileInputStream.read(data);
            return data;
        }
    }

    private static DefaultStreamedContent fileToDefaultStreamedContent(File file, String outFileName) throws IOException {
        return fileToDefaultStreamedContent(file, outFileName, null);
    }

    private static DefaultStreamedContent fileToDefaultStreamedContent(File file, String outFileName, String encoding) throws IOException {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        String mimeType = externalContext.getMimeType(outFileName);
        return new DefaultStreamedContent(Files.newInputStream(file.toPath()), mimeType, outFileName, encoding);
    }

    private static void showErrorMessage(String message, String details) {
        BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, message, details);
    }

    private static String getDateForFileName() {
        LocalDateTime dateTime = LocalDateTime.now();
        return dateTime.format(DateTimeFormatter.ofPattern("dd-MM-uuuu HH mm ss"));
    }

    public static class ConvertibleFileWrapper implements Serializable {
        private static long indexSequence = 1;
        private final long index;
        private final String sourceFileName;
        private final List<MatrixFileTypes> possibleFileTypes;
        private final File sourceTmpFile;
        private String newFileName;
        private MatrixFileTypes newFileType;

        public ConvertibleFileWrapper(UploadedFile uploadedFile) throws IOException {
            index = indexSequence++;
            sourceFileName = uploadedFile.getFileName();
            MatrixFileTypes originalFileType = MatrixFileTypes.detectFileType(sourceFileName);
            newFileName = FilenameUtils.getBaseName(sourceFileName);
            possibleFileTypes = ALLOWED_TYPES.stream()
                    .filter(t -> t != originalFileType)
                    .collect(Collectors.toList());
            newFileType = possibleFileTypes.get(0);
            Path tmpPath = Files.createTempFile(sourceFileName, "." + originalFileType.getExtension());
            Files.copy(uploadedFile.getInputstream(), tmpPath, StandardCopyOption.REPLACE_EXISTING);
            sourceTmpFile = tmpPath.toFile();
        }

        public long getIndex() {
            return index;
        }

        public String getSourceFileName() {
            return sourceFileName;
        }

        public String getNewFileName() {
            return newFileName;
        }

        public void setNewFileName(String newFileName) {
            this.newFileName = newFileName;
        }

        public MatrixFileTypes getNewFileType() {
            return newFileType;
        }

        public void setNewFileType(MatrixFileTypes newFileType) {
            this.newFileType = newFileType;
        }

        public File getSourceTmpFile() {
            return sourceTmpFile;
        }

        public List<MatrixFileTypes> getPossibleFileTypes() {
            return Collections.unmodifiableList(possibleFileTypes);
        }

        public File convert() throws Exception {
            File newTmpFile = Files.createTempFile(null, "." + newFileType.getExtension()).toFile();
            ConvertMatrix.convertMatrices(sourceTmpFile, newTmpFile);
            return newTmpFile;
        }

        public DefaultStreamedContent convertWithDownload() {
            try {
                String fileName = newFileName + '.' + newFileType.getExtension();
                return fileToDefaultStreamedContent(convert(), fileName);
            } catch (Exception e) {
                showErrorMessage("Can't Convert the Matrix", e.getMessage());
                return null;
            }
        }
    }

}
