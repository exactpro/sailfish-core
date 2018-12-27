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
package com.exactpro.sf.scriptrunner;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;

public class ScriptRunnerSettings {
    
    /**
     * Compiler thread priority from {@link Thread#MIN_PRIORITY} to {@link Thread#MAX_PRIORITY} 
     */
    private int compilerPriority = Thread.NORM_PRIORITY;
    /**
     * Exclude messages from information block about all messages in report
     */
    private Set<String> excludedMessages = Collections.emptySet();
    
    public int getCompilerPriority() {
        return compilerPriority;
    }
    
    public void setCompilerPriority(int priority) {
        if (priority > Thread.MAX_PRIORITY) {
            priority = Thread.MAX_PRIORITY;
        } else if (priority < Thread.MIN_PRIORITY) {
            priority = Thread.MIN_PRIORITY;
        }
        
        this.compilerPriority = priority;
    }
    
    public Set<String> getExcludedMessages() {
        return excludedMessages;
    }
    
    public void setExcludedMessages(Set<String> excludedMessages) {
        this.excludedMessages = ObjectUtils.defaultIfNull(excludedMessages, Collections.emptySet());
    }
}
