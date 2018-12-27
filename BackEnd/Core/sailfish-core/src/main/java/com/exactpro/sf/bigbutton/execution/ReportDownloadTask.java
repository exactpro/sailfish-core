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
package com.exactpro.sf.bigbutton.execution;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.FileDownloadWrapper;
import com.exactpro.sf.SFAPIClient;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceLayerException;
import com.exactpro.sf.exceptions.APICallException;
import com.exactpro.sf.exceptions.APIResponseException;

public class ReportDownloadTask implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(ReportDownloadTask.class);
	
    private final IWorkspaceDispatcher workspaceDispatcher;

	private int scriptRunId;
	
	private SFAPIClient apiClient;
	
    private final String reportsFolderPath;
	
	private boolean downloadNeded;

    private final Long sfCurrentID;
	
    public ReportDownloadTask(IWorkspaceDispatcher workspaceDispatcher, int scriptRunId, String reportsFolder,
                              SFAPIClient apiClient, boolean downloadNeded, Long sfCurrentID) {
	
        this.workspaceDispatcher = workspaceDispatcher;
		this.scriptRunId = scriptRunId;
		this.apiClient = apiClient;
		this.reportsFolderPath = reportsFolder != null ? reportsFolder : "";
		this.downloadNeded = downloadNeded;
        this.sfCurrentID = sfCurrentID;
		
	}
	
	private FileDownloadWrapper downloadReport() throws APICallException, IOException, APIResponseException {
		
		FileDownloadWrapper fileDownload;
 		fileDownload = apiClient.getTestScriptRunReportZip(scriptRunId);
        File reportFile;

        try {
            reportFile = this.workspaceDispatcher.createFile(FolderType.REPORT, true, reportsFolderPath, fileDownload.getFileName());
        } catch (WorkspaceLayerException e) {
            throw new RuntimeException("Error while creating report file " + fileDownload.getFileName(), e);
        }

        Files.copy(fileDownload.getInputStream(), reportFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		return fileDownload;
	
	}
	
	private void deleteScriptRun() throws APICallException, APIResponseException {
		
		apiClient.deleteTestScriptRun(scriptRunId);
		
	}
	
	@Override
	public void run() {
		
		FileDownloadWrapper fileDownload = null;
		
		try {
			
			if(downloadNeded) {
				fileDownload = downloadReport();
                setSfCurrentID();
			}
			
			deleteScriptRun();
		
		} catch(Exception e) {
			
			// TODO: report error somewhere
			
			//throw new RuntimeException("Report could not be downloaded", e);
			logger.error(e.getMessage(), e);
			
		} finally {
			
			try {
				
				if(fileDownload != null) {
					fileDownload.getInputStream().close();
				}
				
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
			
			try {
				apiClient.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
			
		}
		
	}

    private void setSfCurrentID() throws APICallException, APIResponseException  {
        if (sfCurrentID != null) {
            apiClient.setSfCurrentID(scriptRunId, sfCurrentID);
        }
    }
}
