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
import com.exactpro.sf.testwebgui.restapi.machinelearning.model.PredictionResultEntry;
import com.exactpro.sf.testwebgui.restapi.machinelearning.model.ReportMessageDescriptor;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SessionStorage {

    private final MLPersistenceManager persistenceManager;

    private final File sessionTempDir = Files.createTempDir();

    private volatile Cache<Integer, List<PredictionResultEntry>> cache = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .maximumSize(32)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();
    private final String reportLink;

    public SessionStorage(String reportLink, MLPersistenceManager mlPersistenceManager) {
        this.reportLink = reportLink;
        persistenceManager = mlPersistenceManager;
    }

    public List<PredictionResultEntry> getPredictions(Integer testCaseId) {

        if (testCaseId == null) {
            return new ArrayList<>(getPredictions0(testCaseId));
        }

        try {
            return cache.get(testCaseId, () -> getPredictions0(testCaseId));
        } catch (ExecutionException e) {
            throw new EPSCommonException(e);
        }
    }

    private List<PredictionResultEntry> getPredictions0(Integer testCaseId) {
        MLWorker worker = new MLWorker();

        InputStream reportStream = persistenceManager.getReport(reportLink);
        File reportArchive = null;
        if (reportStream == null) {
            reportArchive = ReportDownloadUtil.download(reportLink, (attachmentName) -> new File(sessionTempDir, attachmentName));
        }

        try (InputStream is = (reportStream != null ? reportStream : new FileInputStream(reportArchive))) {
            return worker.processReport(testCaseId, is);
        } catch (IOException e) {
            throw new EPSCommonException("Can't parse report file", e);
        } finally {
            FileUtils.deleteQuietly(reportArchive);
        }
    }

    public List<ReportMessageDescriptor> getCheckedMessages() {
        return new ArrayList<>(persistenceManager.get(reportLink));
    }

    public String getReportLink() {
        return reportLink;
    }

    public void removeUserMark(Integer actionId, Integer messageId) {
        persistenceManager.remove(reportLink, actionId, messageId);
    }

    public void addUserMark(List<ReportMessageDescriptor> descriptors) {
        persistenceManager.add(reportLink, descriptors);
    }

}
