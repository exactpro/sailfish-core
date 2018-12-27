/*******************************************************************************
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

package com.exactpro.sf.scriptrunner.impl.htmlreport.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.List;

public class VerificationSerializer extends StdSerializer<Verification> {

    public VerificationSerializer(Class<Verification> clazz) {
        super(clazz);
    }

    @Override
    public void serialize(Verification value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException, JsonProcessingException {

        List<VerificationParameter> parameters = value.getParameters();
        int level = parameters.get(0).getLevel();

        int lastIndex = parameters.size() - 1;

        gen.writeStartObject();
        for (int i = 0; i < parameters.size(); i++) {
            VerificationParameter parameter = parameters.get(i);

            int newLvl = parameter.getLevel();

            int nextLvl;

            if (i < lastIndex) {
                nextLvl = parameters.get(i + 1).getLevel();
            } else {
                nextLvl = newLvl;
            }

            if (newLvl > level) {
                gen.writeStartObject();
            } else {
                while (newLvl < level) { // write end of repeating group
                    gen.writeEndObject();
                    level--;
                }
            }

            if (parameter.getStatus() == null || newLvl < nextLvl) { // not write  status of repeating group
                gen.writeFieldName(parameter.getName());
            } else {
                gen.writeStringField(parameter.getName(), parameter.getStatus().name());
            }

            level = newLvl;
        }
        gen.writeEndObject();
    }
}
