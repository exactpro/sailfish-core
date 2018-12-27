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
package com.exactpro.sf.aml.scriptutil;

import com.exactpro.sf.common.util.EPSCommonException;

public class MvelException extends EPSCommonException {
    private static final long serialVersionUID = 5075793278105122056L;

    private final long line;
    private final String column;

    public MvelException(long line, String column) {
        this.line = line;
        this.column = column;
    }

    public MvelException(long line, String column, String message) {
        super(message);

        this.line = line;
        this.column = column;
    }

    public MvelException(long line, String column, Throwable cause) {
        super(cause);

        this.line = line;
        this.column = column;
    }

    public MvelException(long line, String column, String message, Throwable cause) {
        super(message, cause);

        this.line = line;
        this.column = column;
    }

    public MvelException(long line, String column, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);

        this.line = line;
        this.column = column;
    }

    public long getLine() {
        return line;
    }

    public String getColumn() {
        return column;
    }

    @Override
    public String getMessage() {
        return String.format("Line: %s, Column: %s, Error: %s", line, column, super.getMessage());
    }
}
