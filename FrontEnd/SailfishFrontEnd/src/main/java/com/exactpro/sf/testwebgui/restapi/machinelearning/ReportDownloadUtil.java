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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.ws.rs.core.HttpHeaders;
import java.io.File;
import java.net.URL;
import java.util.UUID;
import java.util.function.Function;

public class ReportDownloadUtil {

    private static final String FILENAME_HEADER_PARAM = "filename";
    private static final String DEFAULT_REPORT_NAME_ZIP = "defaultReportName.zip";

    public static File download(URL reportLink, Function<String, File> fileProvider) {

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(reportLink.toURI());
            return httpclient.execute(get, response -> {
                String attachmentName = DEFAULT_REPORT_NAME_ZIP;

                Header header = response.getFirstHeader(HttpHeaders.CONTENT_DISPOSITION);
                if (header != null) {
                    for (HeaderElement headerElement : header.getElements()) {
                        NameValuePair parameter = headerElement.getParameterByName(FILENAME_HEADER_PARAM);
                        if (parameter != null) {
                            attachmentName = ObjectUtils.defaultIfNull(parameter.getValue(), UUID.randomUUID().toString());
                            break;
                        }
                    }
                }
                File zipTarget = fileProvider.apply(attachmentName);
                if (zipTarget.length() != response.getEntity().getContentLength()) {
                    FileUtils.copyInputStreamToFile(response.getEntity().getContent(), zipTarget);
                }
                return zipTarget;
            });
        } catch (Exception e) {
            throw new EPSCommonException("Unable to download report by specified link - " + reportLink, e);
        }
    }
}
