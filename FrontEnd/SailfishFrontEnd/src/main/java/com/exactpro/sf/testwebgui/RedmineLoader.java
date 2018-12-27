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
package com.exactpro.sf.testwebgui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ssl.SSLSocketFactory;

import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManagerFactory;
import com.taskadapter.redmineapi.WikiManager;
import com.taskadapter.redmineapi.bean.WikiPage;
import com.taskadapter.redmineapi.bean.WikiPageDetail;
import com.thoughtworks.xstream.XStream;

@SuppressWarnings("deprecation")
public class RedmineLoader {
    private static RedmineManager rm;
    private static WikiManager wm;

    private static final String XML_FILE_NAME = "RedminePages.xml";

    private static List<WikiPageDetail> wikiPages;
    private static String apiAccessKey;
    private static String folder;
    private static String rootPage;

    private static void saveWikiPagesToXML() throws IOException{

        try {
            FileUtils.forceMkdir(new File(folder));
            try (FileOutputStream fos = new FileOutputStream(folder + XML_FILE_NAME)) {
            	XStream xs = new XStream();
            	xs.toXML(wikiPages, fos);
            }
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException was occurred. " + e.getMessage());
        }
    }

    /**
     * Creates ClientConnectionManager with SSLSocketFactory that ignores certificate authority
     * @return
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     */
    private static ClientConnectionManager createConnectionManager() throws KeyManagementException, NoSuchAlgorithmException {
        SSLContext sslcontext = SSLContext.getInstance("TLS");

        sslcontext.init(null, new TrustManager[] { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                    throws CertificateException {
                // TODO Auto-generated method stub

            }

            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                    throws CertificateException {
                // TODO Auto-generated method stub

            }
        }}, null);

        SSLSocketFactory sslSocketFactory = new SSLSocketFactory(
                sslcontext,
                SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        return RedmineManagerFactory.createConnectionManager(sslSocketFactory);
    }

    private static void loadWikiPagesFromAPI(String projectName) {
        List<WikiPageDetail> result = new ArrayList<>();

        if(wm == null) {
            System.err.println("Redmine WikiManager is not defined");
            return;
        }

        try {
            List<WikiPage> pages = wm.getWikiPagesByProject(projectName);
            for(WikiPage page : pages) {
                String encoded = URLEncoder.encode(page.getTitle(), "UTF-8");
                WikiPageDetail wikiPageDetail = wm.getWikiPageDetailByProjectAndTitle(projectName, encoded);
                result.add(wikiPageDetail);
            }
        } catch (Exception e) {
            System.err.println("Error getting wiki pages from API. " + e.getMessage());
            result = null;
        }
        wikiPages = result;
    }


    private static boolean isPagesNamesEquals(String name1, String name2) {
        if(name1.equalsIgnoreCase(name2)) {
            return true;
        }

        name1 = name1.replaceAll("[\\./,]", "");
        name2 = name2.replaceAll("[\\./,]", "");

        if(name1.equalsIgnoreCase(name2)) {
            return true;
        }

        //fixes for "3. Индикатор номера тест-кейса ... " page
        name1 = name1.replaceAll("тест-", "");
        name2 = name2.replaceAll("тест-", "");

        return name1.equalsIgnoreCase(name2);
    }

    private static void createWikiXML(String uri, int timeout, String projectName) {

        if(validationParamFailed(uri, timeout, projectName)) {
            System.err.println(" Redmine wiki pages are not imported. ");
            return;
        }

        //check server availability
        if (isReachable(uri, 443, timeout)) {
            System.out.println("Redmine server(" + uri + ") is available");
        } else {
            System.err.println("Redmine server(" + uri + ") is unavailable");
            return;
        }

        String uriWithHttp = uri.startsWith("https") ? uri : "https://" + uri;

        //getting wiki page from server and put it into xml
        try {
            ClientConnectionManager connectionManager = createConnectionManager();

            try(AutoCloseable closeable = connectionManager::shutdown) {
                HttpClient httpClient = RedmineManagerFactory.getNewHttpClient(uriWithHttp, connectionManager);
                rm = RedmineManagerFactory.createWithApiKey(uriWithHttp, apiAccessKey, httpClient);
                wm = rm.getWikiManager();
                loadWikiPagesFromAPI(projectName);
                setRootPage();
                saveWikiPagesToXML();
                System.out.println("Saving completed.");
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getSimpleName() + " has occurred. " + e.getMessage());
            return;
        }
    }

    private static boolean isReachable(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.setReuseAddress(true);
            SocketAddress socketAddress = new InetSocketAddress(host, port);
            socket.connect(socketAddress, timeout);
            return true;
        } catch (IOException e) {
            System.err.println("Address [" + host + ":" + port + "] unreachable. " + e.getMessage());
        }
        return false;
    }

    private static void setRootPage() {
        ListIterator<WikiPageDetail> iterator = wikiPages.listIterator();
        while(iterator.hasNext()) {
            WikiPageDetail pd = iterator.next();
            if(isPagesNamesEquals(pd.getTitle(), rootPage)) {
                pd.setTitle(pd.getTitle().concat("_root"));
                iterator.set(pd);
                break;
            }
        }
    }

    private static boolean validationParamFailed(String uri, int timeout, String projectName) {
        if(uri.trim().length() == 0) {
            System.err.println(" Redmine server is empty. ");
            return true;
        }
        if(timeout < 0) {
            return true;
        }
        if(apiAccessKey.trim().length() == 0) {
            System.err.println(" Redmine API key is empty. ");
            return true;
        }
        if(projectName.trim().length() == 0) {
            System.err.println(" Redmine project is empty. ");
            return true;
        }
        if(rootPage.trim().length() == 0) {
            System.err.println(" Redmine root page is empty. ");
            return true;
        }
        if(folder.trim().length() == 0) {
            return true;
        }
        return false;
    }

    public static void main(String [] args) {

        if(args.length < 6) {
            System.err.println("Need more params to save wiki pages. Canceled");
            return;
        }

        String uri = args[0];
        int timeout = Integer.valueOf(args[1]);
        apiAccessKey = args[2];
        String projectName = args[3];
        folder = args[4];
        rootPage = args[5];

        createWikiXML(uri, timeout, projectName);
    }
}
