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
package com.exactpro.sf.testwebgui.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import java.time.Instant;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.AbstractScriptRunner;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.storage.IMatrix;
import com.exactpro.sf.storage.ITestScriptStorage;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;

public enum ResourceCleaner {
    REPORTS("reports") {
        @Override
        public void clean(Instant olderThan, ISFContext context) {
            long epochMillis = olderThan.toEpochMilli();
            AbstractScriptRunner scriptRunner = context.getScriptRunner();
            ITestScriptStorage scriptStorage = context.getTestScriptStorage();
            List<TestScriptDescription> descriptions = scriptRunner.getDescriptions();

            List<Long> ids = descriptions.stream()
                    .filter(description -> description.getTimestamp().getTime() < epochMillis)
                    .map(description -> description.getId())
                    .collect(Collectors.toList());

            scriptRunner.removeTestScripts(true, ids);

            descriptions = scriptStorage.getTestScriptList()
                    .stream()
                    .filter(description -> description.getTimestamp().getTime() < epochMillis)
                    .collect(Collectors.toList());

            scriptStorage.remove(true, descriptions);
        }
    },
    MATRICES("matrices") {
        @Override
        public void clean(Instant olderThan, ISFContext context) {
            long epochMillis = olderThan.toEpochMilli();
            List<IMatrix> matrices = context.getMatrixStorage().getMatrixList();

            for(IMatrix matrix : matrices) {
                if(matrix.getDate().getTime() < epochMillis) {
                    TestToolsAPI.getInstance().deleteMatrix(matrix);
                }
            }
        }
    },
    MESSAGES("messages") {
        @Override
        public void clean(Instant olderThan, ISFContext context) {
            context.getMessageStorage().removeMessages(olderThan);
        }
    },
    EVENTS("events") {
        @Override
        public void clean(Instant olderThan, ISFContext context) {
            context.getEnvironmentManager().getServiceStorage().removeServiceEvents(olderThan);
        }
    },
    TRAFFIC_DUMP("trafficdump") {
        @Override
        public void clean(Instant olderThan, ISFContext context) {
            long epochMillis = olderThan.toEpochMilli();
            IWorkspaceDispatcher dispatcher = context.getWorkspaceDispatcher();

            try {
                getFiles(dispatcher, FolderType.TRAFFIC_DUMP, epochMillis).forEach(File::delete);
            } catch(IOException e) {
                throw new EPSCommonException(e);
            }
        }
    },
    LOGS("logs") {
        @Override
        public void clean(Instant olderThan, ISFContext context) {
            long epochMillis = olderThan.toEpochMilli();
            IWorkspaceDispatcher dispatcher = context.getWorkspaceDispatcher();

            try {
                getFiles(dispatcher, FolderType.LOGS, epochMillis).filter(file -> {
                    String extension = FilenameUtils.getExtension(file.getName());
                    return FilenameUtils.wildcardMatch(extension, "****-**-**");
                }).forEach(File::delete);
            } catch(IOException e) {
                throw new EPSCommonException(e);
            }
        }
    },
    ML("ml") {
        @Override
        public void clean(Instant olderThan, ISFContext context) {
            IWorkspaceDispatcher wd = context.getWorkspaceDispatcher();
            long epochMillis = olderThan.toEpochMilli();
            try {
                ResourceCleaner.getFiles(wd, FolderType.ML, epochMillis).filter(it -> it.getName().endsWith("json")).forEach(File::delete);
            } catch (IOException e) {
                throw new EPSCommonException(e);
            }
        }
    };
    
    public abstract void clean(Instant olderThan, ISFContext context);

    private final String name;

    private ResourceCleaner(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ResourceCleaner value(String name) {
        for(ResourceCleaner cleaner : values()) {
            if(cleaner.name.equals(name)) {
                return cleaner;
            }
        }

        throw new EPSCommonException("No cleaner for: " + name);
    }

    private static Stream<File> getFiles(IWorkspaceDispatcher dispatcher, FolderType folderType, long olderThan) throws IOException {
        Path folderPath = dispatcher.getFolder(folderType).toPath();

        return Files.walk(folderPath, FileVisitOption.FOLLOW_LINKS)
                .filter(Files::isRegularFile)
                .map(path -> path.toFile())
                .filter(file -> file.lastModified() < olderThan);
    }
}