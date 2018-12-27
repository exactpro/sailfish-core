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
package com.exactpro.sf.aml.script;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.exactpro.sf.common.messages.MessageUtil;

public class CheckPoint {
    private final boolean smart;
    private final long timestamp;

    private final long id;

    public CheckPoint() {
        this(false, MessageUtil.generateId());
    }

    public CheckPoint(boolean smart) {
        this(smart, MessageUtil.generateId());
    }

    public CheckPoint(boolean smart, long id) {
        this.smart = smart;
        this.id = id;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isSmart() {
        return smart;
    }

    public long getId(){
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("smart", smart);
        builder.append("timestamp", timestamp);

        return builder.toString();
    }
}
