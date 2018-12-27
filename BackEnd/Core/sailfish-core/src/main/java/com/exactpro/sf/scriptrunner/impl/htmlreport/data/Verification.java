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
package com.exactpro.sf.scriptrunner.impl.htmlreport.data;

import com.exactpro.sf.scriptrunner.StatusDescription;
import com.exactpro.sf.scriptrunner.impl.htmlreport.HtmlReport.ContextType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Verification extends BaseEntity {
    private static final Logger logger = LoggerFactory.getLogger(Verification.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final int NEXT_AFTER_ROOT = 1;
    private int id;
    private long messageId;
    private StatusDescription statusDescription;
    private List<VerificationParameter> parameters;
    private ContextType context;
    private boolean hasHeaders;

    static {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Verification.class, new VerificationSerializer(Verification.class));
        mapper.registerModule(module);
    }

    public Verification() {
        super();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public StatusDescription getStatusDescription() {
        return statusDescription;
    }

    public void setStatusDescription(StatusDescription statusDescription) {
        this.statusDescription = statusDescription;
    }

    public List<VerificationParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<VerificationParameter> parameters) {
        this.parameters = parameters;
        if (parameters != null) {
            for (int i = NEXT_AFTER_ROOT; i < parameters.size(); i++) {
                if (parameters.get(i).isHeader()) {
                    this.hasHeaders = true;
                    break;
                }
            }
        }
    }

    public ContextType getContext() {
        return context;
    }

    public void setContext(ContextType context) {
        this.context = context;
    }

    public boolean isHasHeaders() {
        return hasHeaders;
    }

    public void setHasHeaders(boolean hasHeaders) {
        this.hasHeaders = hasHeaders;
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("statusDescription", statusDescription);
        builder.append("parameters", parameters.size());

        return builder.toString();
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public String toJson(){
        if(parameters == null){
            return null;
        }

        try {
            return mapper.writeValueAsString(this);
        } catch (Exception e){
            logger.error("Failed serialize Verification to json", e);
            return null;
        }
    }
}
