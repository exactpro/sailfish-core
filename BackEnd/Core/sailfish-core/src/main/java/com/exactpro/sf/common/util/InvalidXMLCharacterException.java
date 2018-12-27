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
package com.exactpro.sf.common.util;

public class InvalidXMLCharacterException extends RuntimeException {
    private static final long serialVersionUID = -5923651470322794577L;

    private String content;
    private int invalidCharacterLine;
    private int invalidCharacterColumn;

    public InvalidXMLCharacterException() {
    }

    public InvalidXMLCharacterException(String message) {
        super(message);
    }

    public InvalidXMLCharacterException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidXMLCharacterException(Throwable cause) {
        super(cause);
    }

    public InvalidXMLCharacterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InvalidXMLCharacterException(String message, Throwable cause, String content, int columnNumber, int lineNumber) {
        this(message, cause);
        this.content = content;
        this.invalidCharacterLine = columnNumber;
        this.invalidCharacterColumn = lineNumber;
    }

    public InvalidXMLCharacterException(String message, Throwable cause, String content) {
        this(message, cause, content, -1, -1);
    }

    public String getContent() {
        return content;
    }

    public int getInvalidCharacterColumn() {
        return invalidCharacterColumn;
    }

    public int getInvalidCharacterLine() {
        return invalidCharacterLine;
    }

    public String getInfo() {
        return new StringBuilder(this.getMessage())
                .append(", column: ")
                .append(invalidCharacterColumn)
                .append(", line: ")
                .append(invalidCharacterLine)
                .append(", content: ")
                .append(content)
                .toString();
    }
}
