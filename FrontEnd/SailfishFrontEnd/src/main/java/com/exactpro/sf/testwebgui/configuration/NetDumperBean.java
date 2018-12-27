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
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.configuration.netdumper.NetDumperOptions;
import com.exactpro.sf.testwebgui.BeanUtil;

@ManagedBean(name="netDumperBean")
@ViewScoped
@SuppressWarnings("serial")
public class NetDumperBean implements Serializable {
	
	private static final Logger logger = LoggerFactory.getLogger(NetDumperBean.class);
	private static final String[] fileSizeSuffixes = new String[] { "B", "KB", "MB", "GB", "TB" };
	
    private static final String AVAILABLE = "Available";
    private static final String UNAVAILABLE = "Unavailable";

    private transient NetDumperOptions options;
    private transient List<File> filteredFiles = new ArrayList<>();

    private String status;
	
	@PostConstruct
	private void init() {
		copyActualOptions();
        this.status = null;
	}
	
	private void copyActualOptions() {
		NetDumperOptions actual = BeanUtil.getSfContext().getNetDumperService().getOptions();
        this.options = new NetDumperOptions();
		
		try {
		    
            this.options.fillFromMap(actual.toMap());
			
        } catch (Exception e) {
			throw new RuntimeException("Actual NetDumper options could not be copied", e);
		}
	}
	
    public String getStatus() {
        if (this.status == null) {
			forceStatusUpdate();
		}
        return this.status;
	}
	
	public void forceStatusUpdate() {
        this.status = BeanUtil.getSfContext().getNetDumperService().checkAvailability()
                ? AVAILABLE
                : UNAVAILABLE;
	}
	
	public NetDumperOptions getOptions() {
		return options;
	}
	
	public void setOptions(NetDumperOptions value) {
		this.options = value;
	}
	
	public void applyOptions() {
		BeanUtil.getSfContext().getNetDumperService().applyOptions(options);
		copyActualOptions();
		BeanUtil.addInfoMessage("Options applied", "");
		forceStatusUpdate();
	}
	
	public StreamedContent downloadFile(File file) {

		if (!file.exists()) {
			logger.info("File {} does not exist", file.getPath());
			BeanUtil.addErrorMessage("File does not exist", "");
			return null;
		}
		
		try {

            InputStream stream = new BufferedInputStream(new FileInputStream(file));

			return new DefaultStreamedContent(stream, "*/*", file.getName());

        } catch (IOException e) {
			logger.error(e.getMessage(), e);
			BeanUtil.addErrorMessage("Download failed", e.getMessage());
		}
		
		return null;
	}
	
	public String getFormattedFileSize(File f) {
		float len = f.length();
		int i = 0;
		while (len > 1024 && i < fileSizeSuffixes.length - 1) {
			i++;
			len /= 1024;
		}
		return String.format("%.2f %s", len, fileSizeSuffixes[i]);
	}

	public List<File> getFilteredFiles() {
		return filteredFiles;
	}

    public void setFilteredFiles(List<File> filteredFiles) {
        this.filteredFiles = filteredFiles;
    }
}