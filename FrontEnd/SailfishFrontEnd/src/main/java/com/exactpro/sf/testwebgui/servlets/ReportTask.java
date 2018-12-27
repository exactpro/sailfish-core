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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
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
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.scriptrunner.EnvironmentSettings;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.ZipReport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReportTask implements Runnable{

	private static final Logger logger = LoggerFactory.getLogger(ReportTask.class);

	private AsyncContext ctx;

	private IWorkspaceDispatcher workspaceDispatcher;

	private ObjectMapper mapper;

	private ISFContext context;

	public ReportTask(AsyncContext ctx, ISFContext context) {
		this.ctx = ctx;
        this.context = context;
		this.workspaceDispatcher = context.getWorkspaceDispatcher();
		this.mapper = new ObjectMapper();
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

		ZipEntry ze = null;

		try(ZipFile zif = new ZipFile(zipFile)) { //ZipInputStream zin = new ZipInputStream(bin)

			final Enumeration<? extends ZipEntry> entries = zif.entries();

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

	@Override
	public void run() {
		try {
			// This servlet is high coupled with ReportServlet (+web.xml) and ScriptrunEventHTMLBuilder

			HttpServletRequest request = (HttpServletRequest) ctx.getRequest();
			HttpServletResponse response = (HttpServletResponse) ctx.getResponse();

			String requestUrl = request.getRequestURI();

			logger.debug("Start processing request {}", requestUrl);

			try {
				requestUrl = URLDecoder.decode(requestUrl, "UTF-8");
			} catch(UnsupportedEncodingException e) {
				logger.error("Failed to unescape request string", e);
			}

			requestUrl = requestUrl.substring(request.getContextPath().length()); // remove ContextPath
            requestUrl = requestUrl.substring(ReportServlet.REPORT_URL_PREFIX.length() + 2); // remove
                                                                                             // 'report/'-prefix

            if (requestUrl.equals("reports")) {
                List<String> names = this.mapper.readValue(request.getParameter("reports"), new TypeReference<ArrayList<String>>() {});
                File temp = File.createTempFile("reports_download", ZipReport.ZIP);
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(temp));

                for (String name : names) {
                    File report;
                    String reportFile = name;

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

                ctx.complete();
                return;
            }

                        String query = request.getParameter("action");


			if (query == null || query.equals("view")) {



				if(workspaceDispatcher.exists(FolderType.REPORT, requestUrl)) {

					File path = workspaceDispatcher.getFile(FolderType.REPORT, requestUrl);

					try (OutputStream output = response.getOutputStream();
						 InputStream input = new FileInputStream(path)) {
						int l = 0;
						byte[] buf = new byte[65535];
						while((l = input.read(buf)) > 0) {
							output.write(buf, 0, l);
						}
					}

				} else {

					tryExtractFromZip(response, requestUrl);

				}

				ctx.complete();

			} else if (query.equals("pack")) {
                //Used only in ReportOutputFormat.FILES mode, also report automatically wrap into zip
                String script_id = request.getParameter("script_id");
                Long id = Long.parseLong(script_id);
                TestScriptDescription descr = context.getScriptRunner().getTestScriptDescription(id);
                String workFolder = descr.getWorkFolder();
                if(!workspaceDispatcher.exists(FolderType.REPORT, workFolder, workFolder + ZipReport.ZIP)){
                    ZipReport zipReport = new ZipReport(descr.getWorkFolder(), workspaceDispatcher,  descr, EnvironmentSettings.ReportOutputFormat.ZIP_FILES);
                    zipReport.createReport(null, null, null, id, null, null);
                    zipReport.closeReport();
                }

				response.getWriter().close();
				ctx.complete();
			} else if (query.equals("zip")) {

				String script_id = request.getParameter("script_id");
				Long id = Long.parseLong(script_id);

				TestScriptDescription descr = context.getScriptRunner().getTestScriptDescription(id);

				long passed = descr.getContext().getScriptProgress().getPassed();
				long failed = descr.getContext().getScriptProgress().getFailed();

				File path = workspaceDispatcher.getFile(FolderType.REPORT, requestUrl);
				String displayZipName = getExportZipName(path.getName(), passed, failed);
				response.setHeader("Content-Disposition", "inline; filename=" + displayZipName);


				try (OutputStream output = response.getOutputStream();
					 InputStream input = new FileInputStream(path)) {
                    IOUtils.copy(input, output);
				}

				ctx.complete();
			} else if (query.equals("simplezip")) {

			    File path;

			    if (workspaceDispatcher.exists(FolderType.REPORT, requestUrl + ZipReport.ZIP)) {
			        path = workspaceDispatcher.getFile(FolderType.REPORT, requestUrl + ZipReport.ZIP);
                } else {
                    path = workspaceDispatcher.getFile(FolderType.REPORT, requestUrl, requestUrl + ZipReport.ZIP);
                }

				response.setHeader("Content-Disposition", "inline; filename=" + path.getName());

				try (OutputStream output = response.getOutputStream();
					 InputStream input = new FileInputStream(path)) {
                                    IOUtils.copy(input, output);
				}

				ctx.complete();
			}

		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			ctx.complete();
		} finally {
			logger.debug("Stop processing request");
		}
	}

	public static File getZip(long id, ISFContext context) throws FileNotFoundException {

		TestScriptDescription descr = context.getScriptRunner().getTestScriptDescription(id);
		String workFolder = descr.getWorkFolder();
        File path = context.getWorkspaceDispatcher().getFile(FolderType.REPORT, workFolder, workFolder + ZipReport.ZIP);

		return path;
	}

	private String getExportZipName(String folder, long passed, long failed) {
		String prefix = folder.substring(0, folder.length() - ZipReport.ZIP.length());
		return prefix + "_p" + passed + "_f" + failed + ZipReport.ZIP;
	}
}
