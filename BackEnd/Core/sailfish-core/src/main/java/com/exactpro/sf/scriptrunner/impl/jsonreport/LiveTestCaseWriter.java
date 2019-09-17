/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.scriptrunner.impl.jsonreport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.Action;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.TestCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

public class LiveTestCaseWriter {
    private static final int LIVE_REPORT_ITEM_LIMIT_PER_FILE = 50;
    private static final Logger logger = LoggerFactory.getLogger(LiveTestCaseWriter.class);

    private static final String mainFileName = "root.js";
    private static final MessageFormat mainJsonpTemplate = new MessageFormat("window.loadLiveReport({0});");

    private static final MessageFormat fileNameTemplate = new MessageFormat("data{0}.js");
    private static final Map<Class<? extends IJsonReportNode>, MessageFormat> jsonpTemplates = ImmutableMap.of(

            // There is no way to live update messages right now.
            // Message.class, new MessageFormat("window.loadMessage({0});"),

            Action.class, new MessageFormat("window.loadAction({0}, {1});")
    );

    private final TestCase testCase;
    private final ObjectMapper mapper;
    private final Path liveReportDir;
    private final IWorkspaceDispatcher dispatcher;

    private final Map<String, Integer> dataFileNames = new HashMap<>();

    private int dataFileCounter = 0;
    private int currentFileItemCounter = 0;
    private int globalItemCounter = 0;
    private Path currentFilePath;
    private String currentFileName;

    public LiveTestCaseWriter(TestCase testcase, ObjectMapper mapper, Path liveReportDir, IWorkspaceDispatcher dispatcher) {
        this.testCase = testcase;
        this.mapper = mapper;
        this.liveReportDir = liveReportDir;
        this.dispatcher = dispatcher;

        cleanDir();
        switchToNextFile();
    }

    public void writeNode(IJsonReportNode node) {

        if (!jsonpTemplates.containsKey(node.getClass())) {
            logger.error("unable to write live report data - unexpected node type '{}', supported types: '{}'", node.getClass(), jsonpTemplates.keySet());
            return;
        }

        if (currentFileItemCounter >= LIVE_REPORT_ITEM_LIMIT_PER_FILE) {
            switchToNextFile();
        }

        try {
            increaseItemCounter();
            write(jsonpTemplates.get(node.getClass()).format(new Object[] { globalItemCounter, mapper.writeValueAsString(node) }));
            updateRootFile();
        } catch (JsonProcessingException e) {
            logger.error("unable to write a node", e);
        }
    }

    public void clearDirectory() {
        if (dispatcher.exists(FolderType.REPORT, liveReportDir.toString())) {
            try {
                dispatcher.removeFolder(FolderType.REPORT, liveReportDir.toString());
            } catch (IOException e) {
                logger.error("unable to remove live report dir");
            }
        }
    }

    private void write(String data) {
        try {
            Files.write(currentFilePath, data.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.error("unable to write data to a file", e);
        }
    }

    private void updateRootFile() {
        try {
            Path rootFilePath = dispatcher.createFile(FolderType.REPORT, true, liveReportDir.resolve(mainFileName).toString()).toPath();

            //noinspection unused
            String data = mainJsonpTemplate.format(new Object[] {
                    mapper.writeValueAsString(new Object() {
                        Instant startTime = testCase.getStartTime();
                        String name = testCase.getName();
                        String id = testCase.getId();
                        int hash = testCase.getHash();
                        String description = testCase.getDescription();
                        Map<String, Integer> dataFiles = dataFileNames;
                        Instant lastUpdate = Instant.now();
                    })
            });

            Files.write(rootFilePath, data.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException e) {
            logger.error("unable to update live report file", e);
        }
    }

    private void switchToNextFile() {
        try {
            dataFileCounter++;

            currentFilePath = dispatcher.createFile(
                    FolderType.REPORT,
                    true,
                    liveReportDir.resolve(fileNameTemplate.format(new Object[] { dataFileCounter })).toString()
            ).toPath();

            currentFileName = currentFilePath.getFileName().toString();
            dataFileNames.put(currentFileName, null);
            currentFileItemCounter = 0;

        } catch (IOException e) {
            logger.error("unable to get a live report data file", e);
        }
    }

    private void cleanDir() {
        try {
            if (dispatcher.exists(FolderType.REPORT, liveReportDir.toString())) {
                dispatcher.removeFolder(FolderType.REPORT, liveReportDir.toString());
            }

        } catch (IOException e) {
            logger.error("unable to clean live report dir");
        }
    }

    private void increaseItemCounter() {
        currentFileItemCounter++;
        globalItemCounter++;
        dataFileNames.put(currentFileName, globalItemCounter);
    }
}
