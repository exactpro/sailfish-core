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

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "sttcrtags")
public class TestCaseRunTag implements Serializable {

    @EmbeddedId
    private TestCaseRunTagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tcr_id")
    @MapsId("tcrId")
    private TestCaseRun testCaseRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    @MapsId("tagId")
    private Tag tag;

    private Boolean custom;

    private TestCaseRunTag() {}

    public TestCaseRunTag(TestCaseRun testCaseRun, Tag tag) {
        this.testCaseRun = testCaseRun;
        this.tag = tag;
        this.id = new TestCaseRunTagId(testCaseRun.getId(), tag.getId());
    }

    public TestCaseRun getTestCaseRun() {
        return testCaseRun;
    }

    public void setTestCaseRun(TestCaseRun testCaseRun) {
        this.testCaseRun = testCaseRun;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public Boolean getCustom() {
        return custom;
    }

    public void setCustom(Boolean custom) {
        this.custom = custom;
    }

    public TestCaseRunTagId getId() {
        return id;
    }

    public void setId(TestCaseRunTagId id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestCaseRunTag that = (TestCaseRunTag) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
