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

import quickfix.FieldMap;
import quickfix.Message;

public final class SailfishQuickfixMessage extends Message {

    private static final long serialVersionUID = -6836383592612422329L;

    protected final SailfishQuickfixHeader header;
    protected final SailfishQuickfixTrailer trailer;
    
    public SailfishQuickfixMessage(int[] fieldOrderBody, int[] fieldOrderHeader, int[] fieldOrderTrailer) {
        super(fieldOrderBody);
        super.header = this.header = new SailfishQuickfixHeader(fieldOrderHeader);
        super.trailer = this.trailer = new SailfishQuickfixTrailer(fieldOrderTrailer);
    }
    
    @Override
    public Object clone() {
        final SailfishQuickfixMessage message = new SailfishQuickfixMessage(getFieldOrder(), getHeader().getFieldOrder(), getTrailer().getFieldOrder());
        message.initializeFrom(this);
        message.getSailfishHeader().initializeFrom(getHeader());
        message.getSailfishTrailer().initializeFrom(getTrailer());
        return message;
    }
    
    public SailfishQuickfixHeader getSailfishHeader() {
        return this.header;
    }
    
    public SailfishQuickfixTrailer getSailfishTrailer() {
        return this.trailer;
    }
    
    private final class SailfishQuickfixHeader extends Header {
        private static final long serialVersionUID = 3064845307807946486L;

        public SailfishQuickfixHeader(int[] fieldOrder) {
            super(fieldOrder);
        }
        
        @Override
        public void initializeFrom(FieldMap source) {
            super.initializeFrom(source);
        }
    }
    
    private final class SailfishQuickfixTrailer extends Trailer {
        private static final long serialVersionUID = 6084700548514306551L;

        public SailfishQuickfixTrailer(int[] fieldOrder) {
            super(fieldOrder);
        }
        
        @Override
        public void initializeFrom(FieldMap source) {
            super.initializeFrom(source);
        }
    }
}
