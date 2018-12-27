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
package com.exactpro.sf.testwebgui.servlets.filters;

import java.io.IOException;

import javax.faces.application.ResourceHandler;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.exactpro.sf.testwebgui.SFWebApplication;

public class ApplicationErrorFilter implements Filter {
	
	private static final String FATAL_ERROR_PAGE = "fatal_error.xhtml";
	
	private static final String ERROR_PAGES_FOLDER = "error";
	
	private static final String INTERNAL_ERROR_PAGE = "internal_error.xhtml";
	
	private static final String PAGES_EXTENSION = ".xhtml";
	
	private boolean isPage(String toCheck) {
		return toCheck.endsWith(PAGES_EXTENSION);
	}
	
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        request.getSession();
        
        if(request.getRequestURI().startsWith(request.getContextPath() + ResourceHandler.RESOURCE_IDENTIFIER)) {        	
        	chain.doFilter(request, response);
        	return;
        }
        
        if(SFWebApplication.getInstance().isFatalError()) {        	        	
        	
        	if(request.getServletPath().endsWith(FATAL_ERROR_PAGE)) {
        		chain.doFilter(request, response);		
        		return;        		
        	} 
        	if(request.getServletPath().endsWith(INTERNAL_ERROR_PAGE)) {
        		response.sendRedirect(FATAL_ERROR_PAGE);
        		
        		return;
        	}       	
        	 
        	if(isPage(request.getServletPath())) {
	        	response.sendRedirect(ERROR_PAGES_FOLDER + "/" +FATAL_ERROR_PAGE);
	    		
	    		return;
        	}
        }

        chain.doFilter(request, response);		
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}

}
