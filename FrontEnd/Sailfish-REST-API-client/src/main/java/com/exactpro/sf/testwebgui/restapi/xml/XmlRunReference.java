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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "runReference")
public class XmlRunReference {

    private List<SwitchOption> switchOptions = new ArrayList<>();
    
    private List<SetOption> oneItemOption = new ArrayList<>();
    
    private List<SetOption> multiItemOption = new ArrayList<>();

    public XmlRunReference addSwitchOption(SwitchOption switchOption) {
        this.switchOptions.add(switchOption);
        return this;
    }
    
    public XmlRunReference addOneItemOption(SetOption setOption) {
        this.oneItemOption.add(setOption);
        return this;
    }
    
    public XmlRunReference addMultiItemOption(SetOption setOption) {
        this.multiItemOption.add(setOption);
        return this;
    }
    
    public List<SwitchOption> getSwitchOptions() {
        return switchOptions;
    }

    public void setSwitchOptions(List<SwitchOption> switchOptions) {
        this.switchOptions = switchOptions;
    }

    public List<SetOption> getOneItemOption() {
        return oneItemOption;
    }

    public void setOneItemOption(List<SetOption> oneItemOption) {
        this.oneItemOption = oneItemOption;
    }

    public List<SetOption> getMultiItemOption() {
        return multiItemOption;
    }

    public void setMultiItemOption(List<SetOption> multiItemOption) {
        this.multiItemOption = multiItemOption;
    }

    public static abstract class AbstractOption {
        
        private String name;

        public AbstractOption() {
            // TODO Auto-generated constructor stub
        }

        public AbstractOption(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class SwitchOption extends AbstractOption {

        private String description;

        public SwitchOption() {}
        
        public SwitchOption(String name, String description) {
            super(name);
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class SetOption extends AbstractOption {

        private List<String> values;

        public SetOption() {}
        
        public SetOption(String name, List<String> values) {
            super(name);
            this.values = values;
        }
        
        public List<String> getValues() {
            return values;
        }

        public void setValues(List<String> values) {
            this.values = values;
        }
    }

}