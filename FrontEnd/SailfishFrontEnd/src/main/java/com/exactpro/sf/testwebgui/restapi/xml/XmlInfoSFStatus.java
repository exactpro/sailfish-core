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
package com.exactpro.sf.testwebgui.restapi.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement (name = "sfstatus")
@XmlAccessorType (XmlAccessType.FIELD)
public class XmlInfoSFStatus {
    
    private String error;
    private XmlInfoModuleDescription core;
    @XmlElementWrapper(name ="plugins")
    @XmlElement(name = "plugin")
    private List<XmlInfoModuleDescription> plugin; 
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public XmlInfoModuleDescription getCore() {
        return core;
    }
    
    public void setCore(XmlInfoModuleDescription core) {
        this.core = core;
    }
    
    public List<XmlInfoModuleDescription> getPlugin() {
        return plugin;
    }
    
    public void setPlugin(List<XmlInfoModuleDescription> plugin) {
        this.plugin = plugin;
    }
}
