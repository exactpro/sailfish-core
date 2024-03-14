/******************************************************************************
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationError;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorLevel;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.tools.validator.ErrorsGroupByLevel;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.model.SelectItem;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.exactpro.sf.testwebgui.tools.validator.DictionaryValidators.*;

@ManagedBean(name = "dictionaryValidatorBean")
@SessionScoped
public class DictionaryValidatorBean {
    private static final String DICTIONARY_FILE_EXT = "xml";
    private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryValidatorBean.class);

    private static final IDictionaryStructureLoader DICTIONARY_STRUCTURE_LOADER;
    private static final List<SelectItem> VALIDATOR_URIS;

    static {
        DICTIONARY_STRUCTURE_LOADER = new XmlDictionaryStructureLoader();
        VALIDATOR_URIS = loadValidatorsURIs().stream()
                .map(uri -> new SelectItem(uri, uri.toString()))
                .collect(Collectors.toList());
    }

    private UploadedTmpFile uploadedFile = null;
    private boolean validated = false;
    private ExceptionWrapper validationException = null;
    private SailfishURI validatorSFUri = getDefaultValidatorURI();

    private final List<DictionaryValidationError> validationErrors = new ArrayList<>();

    public String getAllowedTypesRegex() {
        return String.format("/.*\\.(%s)$/", DICTIONARY_FILE_EXT);
    }

    public String getAllowedTypesMessage() {
        return "Allowed file types: " + DICTIONARY_FILE_EXT;
    }

    public void handleFileUpload(FileUploadEvent event) {
        try {
            clearState();
            uploadedFile = new UploadedTmpFile(event.getFile());
        } catch (IOException e) {
            BeanUtil.addErrorMessage("Can't upload a file", e.getMessage());
        }
    }

    public String getFileName() {
        return (isFileChosen())? uploadedFile.fileName: null;
    }

    public boolean isFileChosen() {
        return uploadedFile != null;
    }

    public void clearState() {
        if(isFileChosen()) {
            uploadedFile.delete();
            uploadedFile = null;
            clearValidation();
        }
    }

    public List<SelectItem> getValidatorSelections() {
        return VALIDATOR_URIS;
    }

    public SailfishURI getDefaultValidatorUri() {
        return getDefaultValidatorURI();
    }

    public SailfishURI getValidatorSelection() {
        return validatorSFUri;
    }

    public void setValidatorSelection(SailfishURI validatorSFUri) {
        LOGGER.info("Selected '{}' validator", validatorSFUri);
        this.validatorSFUri = validatorSFUri;
        clearValidation();
    }

    public void validatorSelectionListener() {
        clearValidation();
    }

    public void validate() {
        if(!isFileChosen() || validated) {
            return;
        }
        try {
            IDictionaryStructure dictionary = loadDictionary(uploadedFile.tmpFile);
            IDictionaryValidator validator = createValidator(validatorSFUri);
            LOGGER.info("Validating '{}' with {}", uploadedFile.fileName, validator.getClass().getName());
            validationErrors.addAll(validator.validate(dictionary, true, null));
        } catch (IOException e) {
            BeanUtil.addErrorMessage("Can't load the dictionary", e.getMessage());
            LOGGER.error("Can't load the dictionary: {}", uploadedFile.fileName, e);
        } catch (EPSCommonException e) {
            validationException = new ExceptionWrapper(e);
            LOGGER.error("Error detected during validating the dictionary", e);
        } finally {
            validated = true;
        }
    }

    public boolean isValidated() {
        return validated;
    }

    public ExceptionWrapper getValidationException() {
        return validationException;
    }

    public boolean isValidationErrorsExist() {
        return !validationErrors.isEmpty();
    }

    public List<ErrorsGroupByLevel> getValidationErrorsByLevel() {
        return validationErrors.stream()
                .collect(Collectors.groupingBy(DictionaryValidationError::getLevel))
                .entrySet()
                .stream()
                .sorted((entry0, entry1) -> {
                    return errorLevelToInt(entry0.getKey()) - errorLevelToInt(entry1.getKey());
                })
                .map(entry -> new ErrorsGroupByLevel(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public boolean success() {
        return validationException == null && validationErrors.isEmpty();
    }

    private void clearValidation() {
        validated = false;
        validationException = null;
        validationErrors.clear();
    }

    private static int errorLevelToInt(DictionaryValidationErrorLevel level) {
        switch(level) {
            case FIELD: return -1;
            case MESSAGE: return 0;
            case DICTIONARY: return 1;
        }
        throw new IllegalArgumentException("Unhandled type of DictionaryValidationErrorLevel");
    }

    private static IDictionaryStructure loadDictionary(File file) throws IOException {
        try(InputStream dictionaryStream = Files.newInputStream(file.toPath())) {
            return DICTIONARY_STRUCTURE_LOADER.load(dictionaryStream);
        }
    }

    public static class ExceptionWrapper {
        private final Exception origin;
        private final String fullMessage;
        private final String stacktrace;

        public ExceptionWrapper(Exception e) {
            origin = e;
            fullMessage = e.toString();
            stacktrace = ExceptionUtils.getStackTrace(e);
        }

        public Exception getOrigin() {
            return origin;
        }

        public String getFullMessage() {
            return fullMessage;
        }

        public String getStacktrace() {
            return stacktrace;
        }
    }

    private static class UploadedTmpFile {
        public final String fileName;
        public final File tmpFile;

        public UploadedTmpFile(UploadedFile uploadedFile) throws IOException {
            fileName = uploadedFile.getFileName();
            tmpFile = createTmpFile(uploadedFile);
        }

        public void delete() {
            if(tmpFile.exists()) {
                tmpFile.delete();
            }
        }

        private static File createTmpFile(UploadedFile uploadedFile) throws IOException {
            Path newPath = Files.createTempFile(uploadedFile.getFileName(), null);
            Files.copy(uploadedFile.getInputstream(), newPath, StandardCopyOption.REPLACE_EXISTING);
            return newPath.toFile();
        }
    }
}
