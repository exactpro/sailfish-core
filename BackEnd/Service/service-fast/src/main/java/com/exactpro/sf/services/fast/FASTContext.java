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
package com.exactpro.sf.services.fast;

import org.openfast.Context;
import org.openfast.debug.BasicDecodeTrace;
import org.openfast.debug.BasicEncodeTrace;

/**
 * @author nikita.smirnov
 *
 */
//FIXME: update openfast lib to 1.1.1
public class FASTContext extends Context {
    
    public void startTrace() {
        if (isTraceEnabled()) {
            if (getEncodeTrace() == null) {
                setEncodeTrace(new BasicEncodeTrace());
            }
            if (getDecodeTrace() == null) {
                setDecodeTrace(new BasicDecodeTrace());
            }
        }
    }
}
