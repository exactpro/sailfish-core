/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.sf.services.util;

import com.exactpro.sf.services.ServiceException;

public class CloseSessionException extends ServiceException {
    private static final long serialVersionUID = -1336216893108240799L;

    public CloseSessionException() {}

    public CloseSessionException(String message) {
        super(message);
    }

    public CloseSessionException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloseSessionException(Throwable cause) {
        super(cause);
    }
}
