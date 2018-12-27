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
package com.exactpro.sf.configuration.recorder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.services.ITaskExecutor;
import com.exactpro.sf.storage.IOptionsStorage;

public class FlightRecorderService {
	
	private static final Logger logger = LoggerFactory.getLogger(FlightRecorderService.class);
	
	private static final String JCMD_INVOKE_COMMAND = 
			" %s JFR.start duration=%ss name=MyRecording filename='%s' settings=profile";
	
	private volatile boolean recordingStarted = false;
	
	private volatile boolean continiousRecordingStarted = false;
	
	private String errorMessage;
	
	private boolean canRecord = true;
	
	private final String[] requiredOptions = new String[] {"-XX:+UnlockCommercialFeatures", "-XX:+FlightRecorder"};
	
	private final List<RecordedFile> recordFiles = new ArrayList<RecordedFile>();
	
	private volatile Date busyTill = null;
	
	private ITaskExecutor taskExecutor;
	
	private ContiniousRecordTask continiousRecordingTask;
	
	private volatile FlightRecorderOptions settings;
	
	private IOptionsStorage optionsStorage;
	
	public FlightRecorderService(ITaskExecutor taskExecutor, IOptionsStorage optionsStorage) {
		
		this.taskExecutor = taskExecutor;
		
		this.optionsStorage = optionsStorage;
		
		this.settings = new FlightRecorderOptions();
		
		try {
			
			this.settings.fillFromMap(optionsStorage.getAllOptions());
			
		} catch (Exception e) {
			
			logger.error("Settings could not be read. Using defaults.");
			
		}
		
		initDefaultSettings();
		
		checkRequiredJvmOptions();
		
	}
	
	private void initDefaultSettings() {
		
		if(this.settings.getJdkPath() == null) {
			
			this.settings.setJdkPath(getJdkFolder());
			
		}
		
		if(this.settings.getRecordsFolder() == null) {
			
			this.settings.setRecordsFolder(System.getProperty("java.io.tmpdir"));
			
		}
		
	}
	
	private void setError(String text) {
		
		this.errorMessage = text;
		
		this.canRecord = false;
		
	}
	
	private void execShell(String command) {
		
        Runtime runtime = Runtime.getRuntime();
		
		try {
			
            Process process = runtime.exec(command);
            if (logger.isDebugEnabled()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = null;

                    while ((line = reader.readLine()) != null) {
                        logger.debug(line);
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            process.waitFor();
		
		} catch(Exception e) {
			
			logger.error(e.getMessage(), e);
			
		}
		
	}
	
	private void checkRequiredJvmOptions() {
		
		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		List<String> arguments = runtimeMxBean.getInputArguments();
		List<String> missingArguments = new ArrayList<>();
		
		logger.info("{}", arguments);
		
		for(String option : this.requiredOptions) {
			
			if(!arguments.contains(option)) {
				
				missingArguments.add(option);
				
			}
			
		}
		
		if(!missingArguments.isEmpty()) {
			
			setError("Application was started without options: <span class=\"required-options\">" + getRequiredOptionsString(missingArguments) + "</span>");
			
		}
		
	}
	
	private String getJdkFolder() {
		
		String environmentValue = System.getenv("JAVA_HOME");
		
		if(StringUtils.isNotEmpty(environmentValue)) {
			return environmentValue;
		}
		
		return System.getProperty("java.home");
		
	}
	
	private String getProcessId() {
	    // Note: may fail in some JVM implementations

	    // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
	    final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
	    
	    logger.info("JVM name: {}", jvmName);
	    
	    final int index = jvmName.indexOf('@');

	    if (index < 1) {
	        // part before '@' empty (index = 0) / '@' not found (index = -1)
	        return null;
	    }

	    try {
	    	
	        return Long.toString(Long.parseLong(jvmName.substring(0, index)));
	        
	    } catch (NumberFormatException e) {
	    	
	        return null;
	        
	    }
	    
	}
	
	private void refreshFilesList() {
		
		List<RecordedFile> toRemove = new ArrayList<>();
		
		for(RecordedFile file : this.recordFiles) {
			
			File fsFile = new File(file.getPath());
			
			if(!fsFile.exists()) {
				
				toRemove.add(file);
				
			}
			
			file.setSize(fsFile.length());
			
		}
		
		if(!toRemove.isEmpty()) {
			
			this.recordFiles.removeAll(toRemove);
			
		}
		
	}
	
	private File createRecordFile(String name) {
		
		try {
			
			File recordsFolder = new File(this.settings.getRecordsFolder());
			
			if(!recordsFolder.exists()) {
				
				throw new FileNotFoundException(this.settings.getRecordsFolder());
				
			}
			
			File result = new File(recordsFolder, name);
			
			result.createNewFile();
			
			return result;
			
		} catch (IOException e) {
			
			logger.error(e.getMessage(), e);
			
			throw new RuntimeException("Can not create output file! ", e);
			
		}
		
	}
	
	public void applySettings(FlightRecorderOptions settings) {
		
		this.settings = settings;
		
		this.recordFiles.clear();
		
		try {
			
			for(Map.Entry<String, String> option : this.settings.toMap().entrySet()) {
				
				this.optionsStorage.setOption(option.getKey(), option.getValue());
				
			}
			
		} catch (Exception e) {
			
			throw new RuntimeException(e);
			
		}
		
	}
	
	private String createRecordFileName(Date from, Date to) {
		
		DateFormat format = new SimpleDateFormat("dd_MM_yyyy_hh_mm_ss");
		
		return "SF_fl_record_ " + format.format(from) + "--" + format.format(to) + ".jfr";
		
	}
	
	public synchronized void startRecording(long duration) {
		
		if(this.recordingStarted) {
			
			if(this.busyTill.after(new Date())) {
				
				throw new IllegalStateException("Recording already in progress");
				
			}
			
		}
		
		String jvmPath =this.settings.getJdkPath();
		
		if(StringUtils.isEmpty(jvmPath)) {
			
			throw new RuntimeException("JAVA_HOME environment variable is not set");
			
		}
		
		String ownPid = getProcessId();
		
		if(StringUtils.isEmpty(ownPid)) {
			
			throw new RuntimeException("Fatal: Process Id can not be resolved");
			
		}
		
		Date recordedFrom = new Date();
		
		Date recordedTo = new Date(recordedFrom.getTime() + duration * 1000l);
		
		String fileName = createRecordFileName(recordedFrom, recordedTo);
		
		File resultFile = createRecordFile(fileName);
		
		File binFolder = new File(jvmPath, "bin");
		
		if (!binFolder.exists()) {
			throw new RuntimeException("Directory does not exists " + binFolder);
		}
		
		File jcmdFile = null;
		
		for (File file : binFolder.listFiles()) {
			if (file.getName().equals("jcmd") || file.getName().startsWith("jcmd.")) {
				jcmdFile = file;
				break;
			}
		}
		
		if (jcmdFile == null) {
			
			throw new RuntimeException("File does not exists " + new File(binFolder, "jcmd").getAbsolutePath());
			
		}
		
		String command = String.format(jcmdFile.getAbsolutePath() + JCMD_INVOKE_COMMAND, 
				ownPid, Long.toString(duration), resultFile.getAbsoluteFile());
		
		execShell(command);
		
		this.recordingStarted = true;
		
		this.busyTill = recordedTo;
		
		this.recordFiles.add(0, new RecordedFile(fileName, 
				resultFile.getAbsolutePath(), recordedFrom, recordedTo) );
		
	}
	
	public synchronized void startContiniousRecording(long durationChank) {
		
		this.continiousRecordingTask = new ContiniousRecordTask(durationChank);
		
		this.taskExecutor.addRepeatedTask(continiousRecordingTask, 0, durationChank, TimeUnit.SECONDS);
		
		this.continiousRecordingStarted = true;
		
	}
	
	public synchronized void stopContiniousRecording() {
		
		if(this.continiousRecordingTask == null) {
			throw new IllegalStateException("Recording is not started");
		}
		
		this.continiousRecordingTask.stop();
		
		this.continiousRecordingStarted = false;
		
	}
	
	public synchronized List<RecordedFile> getRecordedFiles() {
		
		refreshFilesList();
		
		return Collections.unmodifiableList(this.recordFiles);
		
	}
	
	public String getRequiredOptionsString(List<String> args) {
		
		StringBuilder sb = new StringBuilder();
		
		for(int i =0; i < args.size(); i++ ) {
			
			sb.append(args.get(i));
			
			if(i != args.size() -1) {
				sb.append(" ");
			}
			
		}
		
		return sb.toString();
		
	}

	public boolean isRecordingStarted() {
		return recordingStarted;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public boolean isCanRecord() {
		return canRecord;
	}
	
	public boolean isContiniousRecordingStarted() {
		return continiousRecordingStarted;
	}
	
	private class ContiniousRecordTask implements Runnable {
		
		private final long duration;
		
		private volatile boolean running = true; 
		
		public ContiniousRecordTask(long duration) {
			
			this.duration = duration;
			
		}
		
		public void stop() {
			
			this.running = false;
			
		}
		
		@Override
		public void run() {
			
			if(running) {
				
				logger.info("Continious recording: executing record");
				
				startRecording(duration);
				
			} else {
				
				logger.info("Continious recording: interrupting");
				
				throw new RuntimeException("Task was stopped");
				
			}
			
		}
		
	}

	public FlightRecorderOptions getSettings() {
		return settings;
	}	
	
}
