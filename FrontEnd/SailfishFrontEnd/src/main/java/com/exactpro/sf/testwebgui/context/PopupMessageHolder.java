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
package com.exactpro.sf.testwebgui.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;


public class PopupMessageHolder {

    private Map<String, List<FacesMessageAdapter>> messageList;

    public PopupMessageHolder() {
        messageList = new TreeMap<String, List<FacesMessageAdapter>>();
    }

    public void addMessage(FacesMessage msg) {
        FacesMessageAdapter adapter = new FacesMessageAdapter(msg);
        addMessage(adapter);
    }

    public void addMessage(FacesMessageAdapter msg) {
        String uri = getCurrentUri();
        List<FacesMessageAdapter> msgs = messageList.get(uri);
        if(msgs == null) {
            msgs = new ArrayList<FacesMessageAdapter>();
        }
        msgs.add(msg);
        messageList.put(uri, msgs);
    }

    public List<FacesMessageAdapter> getMessagesList(String uri) {
        List<FacesMessageAdapter> result = messageList.get(uri);
        if(result == null) {
            result = new ArrayList<FacesMessageAdapter>();
        }
        return result;
    }

    public void clearMessagesList() {
        messageList.remove(getCurrentUri());
    }

    public List<FacesMessage> getRecentMessages(Long ms) {
        List<FacesMessage> result = new ArrayList<FacesMessage>();

        for(FacesMessageAdapter adapter : getMessagesList(getCurrentUri())) {
            if(new Date().getTime() - adapter.getDate().getTime() < ms) {
                result.add(adapter.getMessage());
            }
        }


        Collections.reverse(result);
        return result;
    }

    private String getCurrentUri() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String uri = request.getRequestURI();
        return uri;
    }
}
