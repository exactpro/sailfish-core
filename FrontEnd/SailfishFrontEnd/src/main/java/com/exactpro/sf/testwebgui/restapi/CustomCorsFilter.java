/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.testwebgui.restapi;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ObjectUtils;

// FIXME: This is a workaround for the new tomcat 9 cors filter, which does not support allow-origin wildcards together with support-credentials=true
public class CustomCorsFilter implements Filter {
    private Set<String> allowedMethods = Collections.emptySet();
    private Set<String> allowedHeaders = Collections.emptySet();
    private Set<String> allowedOrigins = Collections.emptySet();
    private boolean supportCredentials;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        allowedMethods = getListNotNull(filterConfig,"cors.allowed.methods");
        allowedHeaders = getListNotNull(filterConfig,"cors.allowed.headers");
        allowedOrigins = getListNotNull(filterConfig,"cors.allowed.origins");
        supportCredentials = getNotNull(filterConfig,"cors.support.credentials").toLowerCase().equals("true");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {

            HttpServletResponse httpResponse = (HttpServletResponse) response;
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            String origin = httpRequest.getHeader("Origin");

            httpResponse.setHeader("Access-Control-Allow-Methods", String.join(", ", allowedMethods));
            httpResponse.setHeader("Access-Control-Allow-Headers", String.join(", ", allowedHeaders));

            if (supportCredentials && allowedOrigins.contains("*")) {
                httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
                httpResponse.setHeader("Access-Control-Allow-Origin", origin);
            } else {
                httpResponse.setHeader("Access-Control-Allow-Origin", String.join(", ", allowedOrigins));
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    private static String getNotNull(FilterConfig config, String key) {
        return ObjectUtils.defaultIfNull(config.getInitParameter(key), "");
    }

    private static Set<String> getListNotNull(FilterConfig config, String key) {
        return Arrays.stream(getNotNull(config, key).split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }
}
