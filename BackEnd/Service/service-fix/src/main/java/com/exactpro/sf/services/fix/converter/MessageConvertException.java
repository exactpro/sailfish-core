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
package com.exactpro.sf.services.fix.converter;

/**
 * @author nikita.smirnov
 *
 */
public class MessageConvertException extends Exception {

    private static final long serialVersionUID = 8091325017955678801L;
    
    private final Object sfMessage;
    
    public MessageConvertException(Object sfMessage) {
        super();
        this.sfMessage = sfMessage;
    }
    
    public MessageConvertException(Object sfMessage, String message) {
        super(message);
        this.sfMessage = sfMessage;
    }

    public MessageConvertException(Object sfMessage, Throwable cause) {
        super(cause);
        this.sfMessage = sfMessage;
    }

    public MessageConvertException(Object sfMessage, String message, Throwable cause) {
        super(message, cause);
        this.sfMessage = sfMessage;
    }
    
    public MessageConvertException() {
        this((Object)null);
    }
    
    public MessageConvertException(String message) {
        this(null, message);
    }

    public MessageConvertException(Throwable cause) {
        this(null, cause);
    }

    public MessageConvertException(String message, Throwable cause) {
        this(null, message, cause);
    }

    public Object getSfMessage() {
        return sfMessage;
    }
}
