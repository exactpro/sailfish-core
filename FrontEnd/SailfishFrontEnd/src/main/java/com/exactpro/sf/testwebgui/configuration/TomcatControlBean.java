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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.primefaces.event.FileUploadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.embedded.configuration.ServiceStatus;
import com.exactpro.sf.testwebgui.BeanUtil;

@ManagedBean(name = "tomcatControlBean")
@SuppressWarnings("serial")
@SessionScoped
public class TomcatControlBean implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(TomcatControlBean.class);
    private File uploadedFile;

    private final String currentContext = FacesContext.getCurrentInstance().getExternalContext()
            .getRequestContextPath();

    //private IWorkspaceDispatcher wd = null;

    private int port;
    private String host;
    private String user = "admin";
    private String pwd = "admin";
    private String tmpDir = System.getProperty("java.io.tmpdir");
    private String backupDir = System.getProperty("user.home");
    private String tomcatControl;
    private boolean tomcatControlFound;
    
    //refresh state after deserialization
    private Object readResolve()  {
        init();
        return this;
    }

    @PostConstruct
	public void init() {

        FacesContext fctx = FacesContext.getCurrentInstance();
        HttpServletRequest rq = (HttpServletRequest) fctx.getExternalContext().getRequest();
        port = rq.getServerPort();
        try {
            InetAddress thisIp = InetAddress.getLocalHost();
            host = thisIp.getHostAddress() + ":" + port;
        } catch (UnknownHostException e) {
            logger.error(e.getLocalizedMessage(), e);
            host = null;
        }
        
        
		try {
			tomcatControl = findTomcatControl(BeanUtil.getSfContext().getWorkspaceDispatcher()).getCanonicalPath();
			tomcatControlFound = true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

    public void onRestartSailfish() {
        BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "TomcatControl", "Trying to reload application");

        execJar("java", "-jar",
                tomcatControl,
                "reload", "-H" + host, "-u" + getUser(), "-p" + getPwd(), "-n" + currentContext);
    }

    public void handleFileUpload(FileUploadEvent event) {
        if (event.getFile() == null || event.getFile().getFileName().equals("")) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "TomcatControl", "WAR file is null.");
            logger.error("uploaded war file is null");
            return;
        }

        if (!event.getFile().getFileName().toLowerCase().matches("^.+\\.war$")) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "TomcatControl",
                    "The attached file has an incorrect extension. Must be *.war");
            logger.error("uploaded file not a war archive for deploying");
            return;
        }

        try {
			uploadedFile = BeanUtil.getSfContext().getWorkspaceDispatcher().createFile(FolderType.ROOT, true, "uploads", "dist", event.getFile().getFileName());
		} catch (WorkspaceSecurityException | WorkspaceStructureException e) {
			logger.error("cannot create file for uploads", e);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "File upload",
                    "Creating file " + event.getFile().getFileName() + " failed!");
            return;
		}

        try (OutputStream o = new FileOutputStream(uploadedFile); InputStream i = event.getFile().getInputstream();) {
            IOUtils.copy(i, o);
        } catch (IOException e) {
        	logger.error(e.getMessage(), e);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "File upload",
                    "Error occurred: " + e.getLocalizedMessage());
            return;
        }

        BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "File upload",
                "File " + event.getFile().getFileName() + " uploaded successfully!");
    }

    public void handle() {
        try {
			execJar("java", "-jar",
			        tomcatControl,
			        "upgrade", "-H" + host, "-u" + getUser(), "-p" + getPwd(), "-n" + currentContext, "-d" + BeanUtil.getSfContext().getWorkspaceDispatcher().getFolder(FolderType.ROOT),
			        "-t" + getTmpDir(), "-f" + uploadedFile.getAbsolutePath());
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Execute jar",
                    "Error occurred: " + e.getLocalizedMessage());
		}
    }

    public void reloadApplication() {
        BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Application restarting", "Please wait.");
        execJar("java", "-jar",
                tomcatControl,
                "reload", "-H" + host, "-u" + getUser(), "-p" + getPwd(), "-n" + currentContext);
    }

    public void stopApplication() {
        execJar("java", "-jar",
                tomcatControl,
                "stop", "-H" + host, "-u" + getUser(), "-p" + getPwd(), "-n" + currentContext);
    }

    public void makeBackup() {
        BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Backup", "Please wait.");
		try {
			boolean err = execJar("java", "-jar",
			        tomcatControl,
			        "backup", "-H" + host, "-u" + getUser(), "-p" + getPwd(), "-n" + currentContext, "-b" + getBackupDir(),
			        "-d" + BeanUtil.getSfContext().getWorkspaceDispatcher().getFolder(FolderType.ROOT));
			if (!err) {
	            BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Backup", "Bakup created.");
	        } else {
	            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Backup", "Creating failed");
	        }
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Execute jar",
                    "Error occurred: " + e.getLocalizedMessage());
		}

    }

    public void testTmpDir() {
        File tmpDir = new File(getTmpDir());
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
        if (tmpDir.canWrite()) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Test temporary directory.",
                    "Directory exists and writable.");
        } else {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Test temporary directory.",
                    "Directory not exists and not writable.");
        }
    }

    public void testBackupDir() {
        File tmpDir = new File(getBackupDir());
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
        if (tmpDir.canWrite()) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Test backup directory.",
                    "Directory exists and writable.");
        } else {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Test backup directory.",
                    "Directory not exists and not writable.");
        }
    }

    public void testAllDirs() {
        testTmpDir();
        testBackupDir();
    }

    public void testLogPass() {
        boolean hasErr = false;
        hasErr = execJar("java", "-jar",
                tomcatControl, "list",
                "-H" + host, "-u" + getUser(), "-p" + getPwd());
        if (!hasErr) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Test user and password for manager",
                    "Login/password correct");
        } else {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Test user and password for manager",
                    "Login/password incorrect");
        }
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getTmpDir() {
        return tmpDir;
    }

    public void setTmpDir(String tmpDir) {
        this.tmpDir = tmpDir;
    }

    public String getBackupDir() {
        return backupDir;
    }

    public void setBackupDir(String backupDir) {
        this.backupDir = backupDir;
    }

    public boolean getTomcatControlFound() {
        return tomcatControlFound;
    }

    public ServiceStatus getTomcatSpan() {
        if (tomcatControlFound) {
            return ServiceStatus.Connected;
        } else {
            return ServiceStatus.Error;
        }
    }

    private boolean execJar(final String... args) {
        boolean hasErr = false;
        try {
            Process prc = Runtime.getRuntime().exec(args);
            prc.waitFor();

            try (InputStream in = prc.getInputStream(); InputStream err = prc.getErrorStream();) {
                byte[] buffIn = new byte[1024];
                byte[] buffErr = new byte[1024];
                while (in.available() > 0) {
                    in.read(buffIn);
                    logger.info(new String(buffIn));
                    // System.out.println(new String(buffErr));
                }
                while (err.available() > 0) {
                    hasErr = true;
                    err.read(buffErr);
                    logger.error(new String(buffErr));
                    // System.out.println(new String(buffErr));
                }
                buffIn = null;
                buffErr = null;
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error occured while executing TomcatControl! Reason: {}", e.getLocalizedMessage());
        }
        return hasErr;
    }

    private File findTomcatControl(IWorkspaceDispatcher dispatcher) throws FileNotFoundException {
    	Set<String> fileNames = dispatcher.listFiles(new WildcardFileFilter("tomcatcontrol*.jar"), FolderType.ROOT, "tools", "tomcatcontrol");
        if (fileNames == null || fileNames.size() == 0) {
        	throw new FileNotFoundException("TomcatControl tool not found!");

        } else if (fileNames.size() > 1){
        	throw new RuntimeException("To many TomcatControl executable jar's. Using 1st jar " + fileNames);
        }

        String name = fileNames.iterator().next();

        return dispatcher.getFile(FolderType.ROOT, "tools", "tomcatcontrol", name);
    }
}
