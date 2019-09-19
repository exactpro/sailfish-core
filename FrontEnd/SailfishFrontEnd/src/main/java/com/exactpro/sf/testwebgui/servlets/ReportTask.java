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
package com.exactpro.sf.testwebgui.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.scriptrunner.EnvironmentSettings.ReportOutputFormat;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.ZipReport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

public class ReportTask implements Runnable{

    private static final Logger logger = LoggerFactory.getLogger(ReportTask.class);

    private final Map<String, BiConsumer<HttpServletRequest, HttpServletResponse>> handlers = ImmutableMap.of(
            "view", this::viewAction,
            "pack", this::packAction,
            "zip", this::zipAction,
            "simplezip", this::simpleZipAction
    );

    private final AsyncContext ctx;

    private final IWorkspaceDispatcher workspaceDispatcher;

    private final ObjectMapper mapper;

    private final ISFContext context;

    public ReportTask(AsyncContext ctx, ISFContext context) {
        this.ctx = ctx;
        this.context = context;
        this.workspaceDispatcher = context.getWorkspaceDispatcher();
        this.mapper = new ObjectMapper();
    }

    // This servlet is high coupled with ReportServlet (+web.xml) and ScriptrunEventHTMLBuilder
    @Override
    public void run() {
        HttpServletRequest request = (HttpServletRequest) ctx.getRequest();
        HttpServletResponse response = (HttpServletResponse) ctx.getResponse();

        try {
            if("reports".equals(getReportPath(request))) {
                reportsAction(request, response);
                return;
            }

            String query = request.getParameter("action");
            handlers.get(query == null ? "view" : query).accept(request, response);

        } catch (Throwable e) {
            logger.error("failed to run ReportTask - unexpected exception", e);

            response.setStatus(500, e.toString());
        } finally {
            logger.debug("Stop processing request");
            ctx.complete();
        }
    }

    public static File getZip(long id, ISFContext context) throws FileNotFoundException {

        TestScriptDescription descr = context.getScriptRunner().getTestScriptDescription(id);
        String workFolder = descr.getWorkFolder();

        return context.getWorkspaceDispatcher().getFile(FolderType.REPORT, workFolder, workFolder + ZipReport.ZIP);
    }

    private String getExportZipName(String folder, long passed, long failed) {
        String prefix = folder.substring(0, folder.length() - ZipReport.ZIP.length());
        return prefix + "_p" + passed + "_f" + failed + ZipReport.ZIP;
    }

    private void packAction(HttpServletRequest request, HttpServletResponse response) {
        try {
            //Used only in ReportOutputFormat.FILES mode, also report automatically wrap into zip
            String script_id = request.getParameter("script_id");
            Long id = Long.parseLong(script_id);
            TestScriptDescription descr = context.getScriptRunner().getTestScriptDescription(id);
            String workFolder = descr.getWorkFolder();
            if (!workspaceDispatcher.exists(FolderType.REPORT, workFolder, workFolder + ZipReport.ZIP)) {
                ZipReport zipReport = new ZipReport(descr.getWorkFolder(), workspaceDispatcher, descr, ReportOutputFormat.ZIP_FILES);
                zipReport.createReport(null, null, null, id, null, null);
                zipReport.closeReport();
            }

            response.getWriter().close();
        } catch (IOException e) {
            throw new EPSCommonException("unable to execute packAction", e);
        }
    }

    private void zipAction(HttpServletRequest request, HttpServletResponse response) {
        try {
            File path = workspaceDispatcher.getFile(FolderType.REPORT, getReportPath(request));

            String script_id = request.getParameter("script_id");
            Long id = Long.parseLong(script_id);
            TestScriptDescription descr = context.getScriptRunner().getTestScriptDescription(id);

            if (descr != null) {

                long passed = descr.getContext().getScriptProgress().getPassed();
                long failed = descr.getContext().getScriptProgress().getFailed();
                response.setHeader("Content-Disposition", "inline; filename=" + getExportZipName(path.getName(), passed, failed));
            } else {
                logger.error("Unable to find test script description witch id='{}'. This might be a bug!", id);
                response.setHeader("Content-Disposition", "inline; filename=" + getExportZipName(path.getName(), 0, 0));
            }

            try (OutputStream output = response.getOutputStream();
                    InputStream input = new FileInputStream(path)) {
                IOUtils.copy(input, output);
            }

        } catch (IOException e) {
            throw new EPSCommonException("unable to execute zipAction", e);
        }
    }

    private void simpleZipAction(HttpServletRequest request, HttpServletResponse response) {
        String reportPath = getReportPath(request);

        try {
            File path;

            if (workspaceDispatcher.exists(FolderType.REPORT, reportPath + ZipReport.ZIP)) {
                path = workspaceDispatcher.getFile(FolderType.REPORT, reportPath + ZipReport.ZIP);
            } else {
                path = workspaceDispatcher.getFile(FolderType.REPORT, reportPath, reportPath + ZipReport.ZIP);
            }

            response.setHeader("Content-Disposition", "inline; filename=" + path.getName());

            try (OutputStream output = response.getOutputStream();
                    InputStream input = new FileInputStream(path)) {
                IOUtils.copy(input, output);
            }

        } catch (IOException e) {
            throw new EPSCommonException("unable to execute simpleZip action", e);
        }
    }

    private void reportsAction(HttpServletRequest request, HttpServletResponse response) {
        try {
            List<String> names = mapper.readValue(request.getParameter("reports"), new TypeReference<ArrayList<String>>() {});
            File temp = File.createTempFile("reports_download", ZipReport.ZIP);
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(temp));

            for (String reportFile : names) {
                File report;

                try {
                    if (workspaceDispatcher.exists(FolderType.REPORT, reportFile + ZipReport.ZIP)) {
                        report = workspaceDispatcher.getFile(FolderType.REPORT, reportFile + ZipReport.ZIP);
                    } else {
                        report = workspaceDispatcher.getFile(FolderType.REPORT, reportFile, reportFile + ZipReport.ZIP);
                    }
                } catch (Throwable e) {
                    logger.error("Can't read report file {}", reportFile);
                    continue;
                }

                ZipEntry entry = new ZipEntry(FilenameUtils.getName(report.getName()));
                zos.putNextEntry(entry);

                try (FileInputStream fis = new FileInputStream(report)) {
                    IOUtils.copy(fis, zos);
                }

                zos.closeEntry();
            }

            zos.close();

            response.setHeader("Content-Disposition", "inline; filename=scriptRunsReports.zip");

            try (OutputStream output = response.getOutputStream();
                    InputStream input = new FileInputStream(temp)) {
                IOUtils.copy(input, output);
            }

        } catch (IOException e) {
            throw new EPSCommonException("unable to execute reportsAction", e);
        }
    }

    private void viewAction(HttpServletRequest request, HttpServletResponse response) {
        try {
            String reportPath = getReportPath(request);

            if (workspaceDispatcher.exists(FolderType.REPORT, reportPath)) {
                File path = workspaceDispatcher.getFile(FolderType.REPORT, reportPath);
                response.setHeader("Content-Disposition", "inline; filename=" + path.getName());

                try (OutputStream output = response.getOutputStream();
                        InputStream input = new FileInputStream(path)) {
                    int l = 0;
                    byte[] buf = new byte[65535];
                    while ((l = input.read(buf)) > 0) {
                        output.write(buf, 0, l);
                    }
                }
            } else {
                tryExtractFromZip(response, reportPath);
            }

        } catch (Exception e) {
            throw new EPSCommonException("unable to execute viewAction", e);
        }
    }

    private void tryExtractFromZip(HttpServletResponse response, String requestUrl) throws WorkspaceSecurityException, IOException {

        logger.debug("Extract file from zip [{}]", requestUrl);

        String[] pathComponents = requestUrl.split(Matcher.quoteReplacement("/")); // url separator, not file

        boolean found = false;

        for(int i = 0; i < pathComponents.length; i++) {

            if(StringUtils.isEmpty(pathComponents[i])) {
                continue;
            }

            String currentPath = StringUtils.join(Arrays.copyOf(pathComponents, i + 1), File.separator);
            currentPath = StringUtils.appendIfMissing(currentPath, ZipReport.ZIP);

            if(workspaceDispatcher.exists(FolderType.REPORT, currentPath)) {

                logger.debug("{} zip exists", currentPath);

                try (OutputStream output = response.getOutputStream()) {

                    getEntryFromReportZip(workspaceDispatcher.getFile(FolderType.REPORT, currentPath),
                            StringUtils.join(Arrays.copyOfRange(pathComponents, i + 1, pathComponents.length), File.separator), output);

                }

                found = true;
                break;

            } else {
                logger.debug("{} zip NOT exists", currentPath);
                continue;
            }
        }

        if(!found){
            response.setStatus(404);
        }
    }

    private void getEntryFromReportZip(File zipFile, String entryToWrite, OutputStream out) throws IOException {

        logger.debug("Seeking [{}]", entryToWrite);

        ZipEntry ze;

        try(ZipFile zif = new ZipFile(zipFile)) { //ZipInputStream zin = new ZipInputStream(bin)

            Enumeration<? extends ZipEntry> entries = zif.entries();

            String fs = System.getProperty("file.separator");

            while (entries.hasMoreElements()) {

                ze = entries.nextElement();

                String entryName = FilenameUtils.normalize(ze.getName());
                String trueEntryPath = entryName.substring( entryName.indexOf(fs) + 1, entryName.length() );

                logger.debug("Zip entry {}, {}", entryName, trueEntryPath);

                if (trueEntryPath.equals(entryToWrite)) {

                    logger.debug("Found");

                    byte[] buffer = new byte[8192];
                    int len;

                    try (InputStream zin = zif.getInputStream(ze)) {

                        while ((len = zin.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                        out.close();

                    }
                    break;
                }
            }
        }
    }

    private static String getReportPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();

        return Paths.get(pathInfo.startsWith("/")
                ? pathInfo.replaceFirst("/", "")
                : pathInfo
        ).toString();
    }
}
