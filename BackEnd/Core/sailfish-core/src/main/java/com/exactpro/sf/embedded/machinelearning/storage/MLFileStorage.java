/*******************************************************************************
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

package com.exactpro.sf.embedded.machinelearning.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.embedded.machinelearning.entities.FailedAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class MLFileStorage {

    private final Logger logger = LoggerFactory.getLogger(getClass().getName() + "@" + Integer.toHexString(hashCode()));

    private ObjectWriter jsonWriter = new ObjectMapper().writer();
    private final IWorkspaceDispatcher workspaceDispatcher;

    public MLFileStorage(IWorkspaceDispatcher workspaceDispatcher) throws  IOException {

        this.workspaceDispatcher = workspaceDispatcher;
    }

    public void storeDocument(String json) throws IOException {
        File target = workspaceDispatcher.getOrCreateFile(FolderType.ML, UUID.randomUUID() + ".json");
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(target))) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    public void storeFailedAction(FailedAction failedAction) throws IOException {
        storeDocument(jsonWriter.writeValueAsString(failedAction));
    }

    public void zipDocumentsToStream(OutputStream target, int compressionLevel) throws IOException {

        try (ZipOutputStream zipOut = new ZipOutputStream(target)) {
            zipOut.setLevel(compressionLevel);

            for (File document : getDocuments()) {
                ZipEntry zipEntry = new ZipEntry(document.getName());
                zipOut.putNextEntry(zipEntry);
                try (InputStream is = new BufferedInputStream(new FileInputStream(document))) {
                    IOUtils.copy(is, zipOut);
                }
                zipOut.closeEntry();
            }
        }
    }

    public List<File> getDocuments() throws FileNotFoundException {

        Function<String, File> supplier = (file) -> {
            try {
                return workspaceDispatcher.getFile(FolderType.ML, file);
            } catch (FileNotFoundException e) {
                logger.error(e.getMessage(), e);
                throw new EPSCommonException(e.getMessage(), e);
            }
        };

        return workspaceDispatcher.listFiles(it -> it.getName().endsWith("json"), FolderType.ML)
                .stream().map(supplier).collect(Collectors.toList());
    }

}
