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

package com.exactpro.sf.testwebgui.restapi.machinelearning;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.testwebgui.restapi.machinelearning.model.ReportMessageDescriptor;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

public class MLPersistenceManager {

    static final String ML_SUBMITS_FOR_REPORT = "checked.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<?> MESSAGE_DESCRIPTORS_SET_TYPE_REFERENCE = new TypeReference<SubmitMetadataDTO>(){};

    private final IWorkspaceDispatcher workspaceDispatcher;

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public MLPersistenceManager(IWorkspaceDispatcher workspaceDispatcher) {
        this.workspaceDispatcher = workspaceDispatcher;
    }

    public void add(String reportLink, List<ReportMessageDescriptor> descriptors) {

        Function<SubmitMetadataDTO, SubmitMetadataDTO> decorate = (dto) -> {
            dto.getCheckedMessages().addAll(descriptors);
            return dto;
        };

        CompletableFuture.runAsync(() -> updateMetadataOnDisk(decorate, reportLink));
    }

    public Collection<ReportMessageDescriptor> get(String reportLink) {

        try {
            rwLock.readLock().lock();

            return readExplanations(getMlFolder(reportLink)).getCheckedMessages();
        } finally {
            rwLock.readLock().unlock();
        }
    }


    public void remove(String reportLink, long actionId, long messageId) {

        Function<SubmitMetadataDTO, SubmitMetadataDTO> decorate = (dto) -> {

            dto.getCheckedMessages().removeIf(descriptor ->
                    Objects.equals(descriptor.getActionId(), actionId) &&
                            Objects.equals(descriptor.getMessageId(), messageId)
            );

            return dto;

        };

        CompletableFuture.runAsync(() -> updateMetadataOnDisk(decorate, reportLink));
    }

    private void updateMetadataOnDisk(Function<SubmitMetadataDTO, SubmitMetadataDTO> updater, String reportLink) {
        try {
            rwLock.writeLock().lock();

            String mlFolder = getMlFolder(reportLink);
            SubmitMetadataDTO old = readExplanations(mlFolder);
            if (old.getFilename() == null) {
                File report = ReportDownloadUtil.download(reportLink, (attachmentName) -> provideReportFromMLDir(mlFolder, attachmentName));
                old.setFilename(report.getName());
            }

            writeExplanations(mlFolder, updater.apply(old));
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private File provideReportFromMLDir(String mlFolder, String attachmentName) {

        try {
            return workspaceDispatcher.getOrCreateFile(FolderType.ML, mlFolder, attachmentName);
        } catch (WorkspaceStructureException | FileNotFoundException e) {
            throw new EPSCommonException("Can't read report file " + attachmentName, e);
        }
    }

    public InputStream getReport(String reportLink) {
        try {
            SubmitMetadataDTO submitMetadataDTO = readExplanations(getMlFolder(reportLink));
            return new FileInputStream(
                    workspaceDispatcher.getFile(FolderType.ML, getMlFolder(reportLink), submitMetadataDTO.getFilename()));
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    private String getMlFolder(String reportLink) {
        return DigestUtils.md5Hex(reportLink);
    }

    private SubmitMetadataDTO readExplanations(String mlFolder) {

        try {
            File explanations = workspaceDispatcher.getFile(FolderType.ML, mlFolder, ML_SUBMITS_FOR_REPORT);
            return OBJECT_MAPPER.readValue(explanations, MESSAGE_DESCRIPTORS_SET_TYPE_REFERENCE);
        } catch (FileNotFoundException e) {
            return new SubmitMetadataDTO(null, new HashSet<>());
        } catch (IOException e) {
            throw new EPSCommonException("Can't read submitted by user marks to " + mlFolder, e);
        }
    }

    private void writeExplanations(String mlFolder, SubmitMetadataDTO descriptors) {

        try {
            File explanations = workspaceDispatcher.getOrCreateFile(FolderType.ML, mlFolder, ML_SUBMITS_FOR_REPORT);
            OBJECT_MAPPER.writeValue(explanations, descriptors);
        } catch (IOException e) {
            throw new EPSCommonException("Can't write user marks for " + mlFolder, e);
        }
    }

    private static class SubmitMetadataDTO {
        private String filename;
        private Set<ReportMessageDescriptor> checkedMessages;

        @JsonCreator
        SubmitMetadataDTO(@JsonProperty("filename") String filename, @JsonProperty("checkedMessages") Set<ReportMessageDescriptor> checkedMessages) {
            this.filename = filename;
            this.checkedMessages = checkedMessages;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public Set<ReportMessageDescriptor> getCheckedMessages() {
            return checkedMessages;
        }

        public void setCheckedMessages(Set<ReportMessageDescriptor> checkedMessages) {
            this.checkedMessages = checkedMessages;
        }
    }
}
