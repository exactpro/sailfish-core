/******************************************************************************
 * Copyright 2009-2023 Exactpro (Exactpro Systems Limited)
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
public class MatrixRunTagId implements Serializable {

    @Column(name = "mr_id")
    private long mrId;

    @Column(name = "tag_id")
    private long tagId;

    public MatrixRunTagId() {
        this(0, 0);
    }

    public MatrixRunTagId(long mrId, long tagId) {
        this.mrId = mrId;
        this.tagId = tagId;
    }

    public long getMrId() {
        return mrId;
    }

    public void setMrId(long mrId) {
        this.mrId = mrId;
    }

    public long getTagId() {
        return tagId;
    }

    public void setTagId(long tagId) {
        this.tagId = tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MatrixRunTagId)) {
            return false;
        }
        MatrixRunTagId that = (MatrixRunTagId) o;
        return mrId == that.mrId && tagId == that.tagId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mrId, tagId);
    }
}
