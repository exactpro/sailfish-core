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

package com.exactpro.sf.testwebgui.scriptruns.converters;

import org.apache.commons.lang3.StringUtils;

public class MappingContainer {
    private String from;
    private String to;


    boolean error = false;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        if(from.equals(to)){
            error = true;
            return;
        }

        error = false;
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        if(to.equals(from)){
            error = true;
            return;
        }
        error = false;
        this.to = to;
    }

    public void revertValue(String oldValue, boolean isFrom){
        if(isFrom){
            from = oldValue;
        }else {
            to = oldValue;
        }
    }

    public boolean haveError(){
        return error;
    }

    public boolean isEmpty(){
        return StringUtils.isEmpty(from) && StringUtils.isEmpty(to);
    }
}
