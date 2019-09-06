/******************************************************************************
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
package com.exactpro.sf.services.fast;

import com.exactpro.sf.common.messages.IMessage;

public class DecodeResult {
    private final IMessage decodedMessage;
    private final int processedDataLength;
    private final Status status;

    private DecodeResult(Status status, IMessage decodedMessage, int processedDataLength) {
        this.status = status;
        this.decodedMessage = decodedMessage;
        this.processedDataLength = processedDataLength;
    }

    public static DecodeResult createSuccessResult(IMessage message, int processedDataLength) {
        return new DecodeResult(Status.SUCCESS, message, processedDataLength);
    }

    public static DecodeResult createNotEnoughDataResult() {
        return new DecodeResult(Status.NOT_ENOUGH_DATA, null, 0);
    }

    public IMessage getDecodedMessage() {
        return decodedMessage;
    }

    public int getProcessedDataLength() {
        return processedDataLength;
    }

    public boolean isDataProcessed() {
        return status != Status.NOT_ENOUGH_DATA;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    private enum Status {
        SUCCESS, NOT_ENOUGH_DATA
    }
}
