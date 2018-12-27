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

package com.exactpro.sf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class SerializeUtil {
    private static final Logger logger = LoggerFactory.getLogger(SerializeUtil.class);

    public static <T> T deserializeBase64Obj(String encodedCause, Class<T> clazz){
        if (encodedCause == null) {
            return null;
        }

        byte[] data = DatatypeConverter.parseBase64Binary(encodedCause);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {

            Object o = ois.readObject();
            return clazz.cast(o);

        } catch (IOException | ClassNotFoundException e) {
            logger.error("Object deserializing error", e);
            return null;
        }

    }

    public static String serializeToBase64(Serializable o) {
        if (o == null) {
            return null;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(o);
            }
           return DatatypeConverter.printBase64Binary(baos.toByteArray());
        }catch (IOException e) {
            logger.error("Object serializing error", e);
            return null;
        }
    }

}
