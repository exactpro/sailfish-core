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

import com.exactpro.sf.common.util.Utils;
import com.exactpro.sf.scriptrunner.StatusDescription;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TestCase extends BaseEntity {
    private String id;
    private int order;
    private Date startTime;
    private Date finishTime;
    private int hash;
    private StatusDescription status;
    private Map<String, String> tags = Collections.emptyMap(); //maps tag name to css class

    public TestCase() {
        super();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    public int getHash() {
        return hash;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }
    
    public void setStatus(StatusDescription status) {
    	this.status = status;
    }
    
    public StatusDescription getStatus() {
    	return status;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags.stream().collect(Collectors.toMap(Function.identity(), tagName -> Utils.getTagColorClass(tagName)));
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("id", id);
        builder.append("order", order);
        builder.append("startTime", startTime);
        builder.append("finishTime", finishTime);
        builder.append("hash", hash);
        builder.append("tags", tags);
        builder.append(super.toString());

        return builder.toString();
    }
}
