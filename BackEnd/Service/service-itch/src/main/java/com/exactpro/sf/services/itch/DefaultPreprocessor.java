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
package com.exactpro.sf.services.itch;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.itch.configuration.Preprocessor;
import com.exactpro.sf.services.itch.configuration.Preprocessors;
import com.exactpro.sf.util.DateTimeUtility;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;

public class DefaultPreprocessor implements IITCHPreprocessor {
    private static final Logger logger = LoggerFactory.getLogger(DefaultPreprocessor.class);

    @Override
    public void process(IMessage message, IoSession session, IMessageStructure structure) {
        addMessageTimeField(message, structure, session);
    }

    protected void addMessageTimeField(IMessage message, IMessageStructure msgStructure, IoSession session) {
        try {
            String secondsField = getSecondsField();
            String nanosField = getNanoField();
            if (isTimeMessage(msgStructure)) {
                if (msgStructure.getFieldNames().contains(secondsField)) {
                    session.setAttribute(ITCHMessageHelper.FIELD_SECONDS, message.getField(secondsField));
                } else {
                    logger.warn("Message {} [{}] not contains field {}", message.getName(), message.getFieldNames(),
                            secondsField);
                }
            } else if (msgStructure.getFieldNames().contains(ITCHMessageHelper.FAKE_FIELD_MESSAGE_TIME)
                    && session.containsAttribute(ITCHMessageHelper.FIELD_SECONDS)
                    && msgStructure.getFieldNames().contains(nanosField)) {
                long seconds = ((Number) ObjectUtils.defaultIfNull(session.getAttribute(ITCHMessageHelper.FIELD_SECONDS), -1l)).longValue();
                if (seconds != -1l) {
                    long nanoSeconds = ObjectUtils.defaultIfNull(message.getField(nanosField), 0L);
                    LocalDateTime dateTime = toLocalDateTime(seconds, nanoSeconds);
                    message.addField(ITCHMessageHelper.FAKE_FIELD_MESSAGE_TIME, dateTime);
                }
            }
        } catch (RuntimeException e) {
            logger.warn("{} field has not been add to message {} ", ITCHMessageHelper.FAKE_FIELD_MESSAGE_TIME, message.getName(), e);
        }
    }

    protected boolean isTimeMessage(IMessageStructure structure) {
        return ITCHMessageHelper.MESSAGE_TYPE_TIME.equals(
                structure.getAttributeValueByName(ITCHMessageHelper.ATTRIBUTE_MESSAGE_TYPE));
    }

    protected String getSecondsField() {
        return ITCHMessageHelper.FIELD_SECONDS;
    }

    protected String getNanoField() {
        return ITCHMessageHelper.FIELD_NANOSECOND;
    }

    protected LocalDateTime toLocalDateTime(long seconds, long nanoSeconds) {
        return DateTimeUtility.toLocalDateTime(seconds * 1000).plusNanos(nanoSeconds);
    }

    protected int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    public static IITCHPreprocessor loadPreprocessor(IServiceContext serviceContext, SailfishURI dictURI,
                                                     SailfishURI preprocessorURI, ClassLoader loader) {
        if (dictURI == null || serviceContext == null || !serviceContext.getDataManager().exists(preprocessorURI)) {
            return new DefaultPreprocessor();
        }

        try (InputStream stream = serviceContext.getDataManager().getDataInputStream(preprocessorURI)) {
            JAXBContext jc = JAXBContext.newInstance(Preprocessors.class);
            Unmarshaller u = jc.createUnmarshaller();

            JAXBElement<Preprocessors> element = u.unmarshal(new StreamSource(stream), Preprocessors.class);

            for (Preprocessor p : element.getValue().getPreprocessor()) {
                if (dictURI.getResourceName().equals(p.getTitle())) {
                    Class<?> cls = loader.loadClass(p.getResource());
                    return (IITCHPreprocessor) cls.newInstance();
                }
            }
        } catch (IOException | JAXBException | ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException e) {
            logger.error("Failed to load IITCHPreprocessor", e);
            throw new RuntimeException("Failed to load IITCHPreprocessor", e);
        }
        return new DefaultPreprocessor();
    }

}
