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
package com.exactpro.sf.scriptrunner.actionmanager.exceptions;

import com.exactpro.sf.common.util.EPSCommonException;

public class ActionCallException extends EPSCommonException {

    private static final long serialVersionUID = -5660769792925034607L;

    public ActionCallException() {
        // TODO Auto-generated constructor stub
    }

    public ActionCallException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public ActionCallException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public ActionCallException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

}
