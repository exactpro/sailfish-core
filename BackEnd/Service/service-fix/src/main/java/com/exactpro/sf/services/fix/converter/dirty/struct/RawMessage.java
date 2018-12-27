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
package com.exactpro.sf.services.fix.converter.dirty.struct;

import java.nio.charset.Charset;
import java.util.Objects;

import org.quickfixj.CharsetSupport;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.fix.FixMessageHelper;
import com.exactpro.sf.services.fix.converter.dirty.FieldConst;

public class RawMessage extends FieldList {

    public void calculateBodyLength(Charset charset) {
        Objects.requireNonNull(charset, "charset cannot be null");

        Field header = getField(FieldConst.HEADER);

        if(header == null || !header.isComponent()) {
            throw new EPSCommonException("Header is missing or not a component");
        }

        FieldList headerFields = header.getFields();
        if (headerFields.getField(FieldConst.BODY_LENGTH) == null) {
            String lengthString = String.valueOf(getLength(charset));

            headerFields.addField(new Field(FieldConst.BODY_LENGTH, lengthString));
        } else {
            throw new EPSCommonException("Header already contains body length");
        }
    }

    public void calculateBodyLength() {
        calculateBodyLength(CharsetSupport.getCharsetInstance());
    }

    public void calculateCheckSum(Charset charset) {
        Objects.requireNonNull(charset, "charset cannot be null");

        Field trailer = getField(FixMessageHelper.TRAILER);

        if(trailer == null || !trailer.isComponent()) {
            throw new EPSCommonException("Trailer is missing or not a component");
        }

        FieldList trailerFields = trailer.getFields();
        if (trailerFields.getField(FieldConst.CHECKSUM) == null) {
            String checksumString = String.format("%03d", getChecksum(charset));
    
            trailerFields.addField(new Field(FieldConst.CHECKSUM, checksumString));
        } else {
            throw new EPSCommonException("Trailer already contains checksum");
        }
    }

    public void calculateCheckSum() {
        calculateCheckSum(CharsetSupport.getCharsetInstance());
    }

    public byte[] getBytes() {
        return super.getBytes(CharsetSupport.getCharsetInstance());
    }
}
