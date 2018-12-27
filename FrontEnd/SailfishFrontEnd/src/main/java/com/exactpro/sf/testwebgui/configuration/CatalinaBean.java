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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.io.IOUtils;
import org.primefaces.event.FileUploadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.testwebgui.BeanUtil;

@ManagedBean(name = "catalinaBean")
@ViewScoped
public class CatalinaBean implements Serializable {

    private static final long serialVersionUID = 1197075113915763236L;
    private static final Logger logger = LoggerFactory.getLogger(CatalinaBean.class);

    private String catalinaHost = "localhost";
    private int catalinaPort = 4443;
    private String catalinaURL = "http://" + catalinaHost + ":" + catalinaPort + "/";
    private File uploadedFile;
    String boundary = Long.toHexString(System.currentTimeMillis());
    String CRLF = "\r\n";

    public boolean getReady() {

        if (checkDaemonAvailable()) {
            return Boolean.valueOf(sendRequest0("ready"));
        }

        return false;
    }

    public String getCatalinaURL() {
        return catalinaURL;
    }

    public void setCatalinaURL(String catalinaURL) {
        this.catalinaURL = catalinaURL;
    }

    public void restart() {

        sendRequest("restart");
    }

    public void stop() {

        sendRequest("stop");
    }

    public void start() {

        sendRequest("start");
    }

    public void deploy() {
        if (uploadedFile == null) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "War file not uploaded",
                    "Upload the file!");
            return;
        }
        try {
            URL url = new URL(catalinaURL + "deploy");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            // con.setRequestProperty("Content-Disposition", "form-data;
            // name=\"warFile\"; filename=\"" + uploadedFile.getName() + "\"");
            try (OutputStream os = con.getOutputStream();
                    InputStream in = new FileInputStream(uploadedFile);) {
                os.write(("--" + boundary + CRLF).getBytes());
                os.write(("Content-Disposition: form-data; name=\"file\"; filename=\""
                        + uploadedFile.getName() + "\"" + CRLF).getBytes());
                byte[] cluster = new byte[1024];
                int len;
                while ((len = in.read(cluster)) != -1) {
                    os.write(cluster, 0, len);
                }
                os.write((CRLF + "--" + boundary + "--" + CRLF).getBytes());
                os.flush();
                StringBuilder sb = new StringBuilder();

                InputStream resp = con.getInputStream();
                for (int ch = resp.read(); ch != -1; ch = resp.read()) {
                    sb.append((char) ch);
                }
                resp.close();

                BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Response from daemon",
                        sb.toString());
            } finally {
                uploadedFile.delete();
                uploadedFile = null;
            }
        } catch (IOException e) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage());
            logger.error("{}", e);
        }
    }

    public void handleFileUpload(FileUploadEvent event) {
        if (event.getFile() == null || event.getFile().getFileName().equals("")) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "WarUpload", "WAR file is null.");
            logger.error("uploaded war file is null");
            return;
        }

        if (!event.getFile().getFileName().toLowerCase().matches("^.+\\.war$")) {
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "WarUpload",
                    "The attached file has an incorrect extension. Must be *.war");
            logger.error("uploaded file not a war archive for deploying");
            return;
        }
        try {
            uploadedFile = SFLocalContext.getDefault().getWorkspaceDispatcher().createFile(
                    FolderType.ROOT, true, "uploads", "dist", event.getFile().getFileName());
        } catch (WorkspaceSecurityException | WorkspaceStructureException e) {
            logger.error("cannot create file for uploads", e);
            BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "File upload",
                    "Creating file " + event.getFile().getFileName() + " failed!");
            return;
        }

        try (OutputStream o = new FileOutputStream(uploadedFile);
                InputStream i = event.getFile().getInputstream();) {
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

    private void sendRequest(String command) {

        BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Response from daemon",
                sendRequest0(command));
    }

    private String sendRequest0(String command) {
        String ret = null;
        try {
            URL url = new URL(catalinaURL + command);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            StringBuilder response = new StringBuilder();
            try (InputStream in = con.getInputStream()) {
                for (int ch = in.read(); ch != -1; ch = in.read()) {
                    response.append((char) ch);
                }
            } finally {
                ret = response.toString();
            }
        } catch (IOException e) {
            logger.error("{}", e);
        }
        return ret;
    }

    private boolean checkDaemonAvailable() {

        try (Socket daemon = new Socket("localhost", 4443)) {
            return daemon.isConnected();
        } catch (IOException e) {
            return false;
        }
    }

}
