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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.context.RequestContext;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.configuration.recorder.FlightRecorderOptions;
import com.exactpro.sf.configuration.recorder.RecordedFile;
import com.exactpro.sf.testwebgui.BeanUtil;

@ManagedBean(name="flightRecorderBean")
@ViewScoped
@SuppressWarnings("serial")
public class FlightRecorderBean implements Serializable {
	
	private static final Logger logger = LoggerFactory.getLogger(FlightRecorderBean.class);
	
	private long duration = 300;
	
	private boolean continiously = false;
	
	private FlightRecorderOptions settings;
	
	@PostConstruct
	public void init() {
		
		copyActualSettings();
		
	}
	
	private void copyActualSettings() {
		
		FlightRecorderOptions actualSettings = BeanUtil.getSfContext().getFlightRecorderService().getSettings();
		
		this.settings = new FlightRecorderOptions();
		
		try {
			
			this.settings.fillFromMap(actualSettings.toMap());
			
		} catch (Exception e) {
			
			logger.error("Actual recorder settings could not be copied", e);
			throw new RuntimeException("Actual recorder settings could not be copied", e);
			
		}
		
	}
	
	public void applySettings() {
		
		BeanUtil.getSfContext().getFlightRecorderService().applySettings(settings);
		
		copyActualSettings();
		
		BeanUtil.addInfoMessage("Applied", "");
		
		RequestContext.getCurrentInstance().execute("PF('advancedRecOptDialog').hide();");
		
	}
	
	public String formatFileSize(long bytes) {
		
		int unit = 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    char pre = ("KMGTPE").charAt(exp-1);
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
		
	}
	
	public StreamedContent downloadFile(RecordedFile file) {
		
		File fsFile = new File(file.getPath());
		
		try {
			
			if(!fsFile.exists()) {
				logger.warn("File {} doesn't exists!", file.getPath());
				BeanUtil.addErrorMessage("File does not exists!", "");
				return null;
			}
			
			InputStream stream = new BufferedInputStream (new FileInputStream(fsFile) );
			
			return new DefaultStreamedContent(stream, "*/*", file.getName());
			
		} catch(IOException e) {
			logger.error(e.getMessage(), e);
			BeanUtil.addErrorMessage("Download failed", e.getMessage());
		}
		
		return null;
		
	}
	
	public void deleteFile(RecordedFile file) {
		
		File fsFile = new File(file.getPath());
		
		fsFile.delete();
		
	}
	
	public void startRecording() {
		
		try {
			
			if(this.continiously) {
				
				BeanUtil.getSfContext().getFlightRecorderService().startContiniousRecording(duration);
				
			} else {
				
				BeanUtil.getSfContext().getFlightRecorderService().startRecording(duration);
				
			}
			
			BeanUtil.addInfoMessage("Recording started", "");
			
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			BeanUtil.addErrorMessage("Error", e.getMessage());
		}
		
	}
	
	public void stopContiniousRecording() {
		
		try {
			
			BeanUtil.getSfContext().getFlightRecorderService().stopContiniousRecording();
			
			BeanUtil.addInfoMessage("Recording stoped", "");
			
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			BeanUtil.addErrorMessage("Error", e.getMessage());
		}
		
	}
	
	public boolean isContiniousRecordingStarted() {
		
		return BeanUtil.getSfContext().getFlightRecorderService().isContiniousRecordingStarted();
		
	}
	
	public boolean isRecordingFinished(RecordedFile file) {
		
		return file.getTo().before(new Date());
		
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public boolean isContiniously() {
		return continiously;
	}

	public void setContiniously(boolean continiously) {
		this.continiously = continiously;
	}

	public FlightRecorderOptions getSettings() {
		return settings;
	}

	public void setSettings(FlightRecorderOptions settings) {
		this.settings = settings;
	}
	
}
