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
package com.exactpro.sf.embedded.statistics.entities;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TestCaseRunTagId implements Serializable {

    @Column(name = "tcr_id")
    private Long tcrId;

    @Column(name = "tag_id")
    private Long tagId;

    public TestCaseRunTagId() {}

    public TestCaseRunTagId(Long tcrId, Long tagId) {
        this.tcrId = tcrId;
        this.tagId = tagId;
    }

    public Long getTcrId() {
        return tcrId;
    }

    public void setTcrId(Long tcrId) {
        this.tcrId = tcrId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestCaseRunTagId that = (TestCaseRunTagId) o;
        return Objects.equals(tcrId, that.tcrId) &&
                Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tcrId, tagId);
    }
}
