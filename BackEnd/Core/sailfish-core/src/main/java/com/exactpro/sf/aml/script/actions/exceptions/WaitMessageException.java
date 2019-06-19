/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.sf.aml.script.actions.exceptions;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.ObjectUtils;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;

public class WaitMessageException extends EPSCommonException {
    private static final long serialVersionUID = 8951980266755385168L;

    private final Collection<IMessage> similarMessages;

    public WaitMessageException(Collection<IMessage> similarMessages) {
        this.similarMessages = similarMessages;
    }

    public WaitMessageException(String message, Collection<IMessage> similarMessages) {
        super(message);
        this.similarMessages = similarMessages;
    }

    public WaitMessageException(Throwable cause, Collection<IMessage> similarMessages) {
        super(cause);
        this.similarMessages = similarMessages;
    }

    public WaitMessageException(String message, Throwable cause, Collection<IMessage> similarMessages) {
        super(message, cause);
        this.similarMessages = similarMessages;
    }

    public WaitMessageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Collection<IMessage> similarMessages) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.similarMessages = similarMessages;
    }

    public Collection<IMessage> getSimilarMessages() {
        return defaultIfNull(similarMessages, Collections.emptyList());
    }
}
