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
package com.exactpro.sf.center.impl;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.exactpro.sf.center.IVersion;

/**
 * @author nikita.smirnov
 *
 */
public abstract class AbstractVersion implements IVersion {

    @Override
    public String buildShortVersion() {
        return new StringBuilder()
                .append(getMajor() != -1 ? getMajor() : 0)
                .append('.')
                .append(getMinor() != -1 ? getMinor() : 0)
                .append('.')
                .append(getMaintenance() != -1 ? getMaintenance() : 0)
                .toString();
    }

    @Override
    public String buildVersion() {
        StringBuilder builder = new StringBuilder()
                .append(getMajor() != -1 ? getMajor() : 0)
                .append('.')
                .append(getMinor() != -1 ? getMinor() : 0)
                .append('.')
                .append(getMaintenance() != -1 ? getMaintenance() : 0)
                .append('.')
                .append(getBuild() != -1 ? getBuild() : 0);
                if (getRevision() != null) {
                    builder.append('-')
                    .append(getRevision());
                }
        return builder.toString();
    }
    
    @Override
    public boolean isGeneral() {
        return GENERAL.equals(this.getAlias());
    }

    @Override
    public String getArtifactName() {
        return null;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("Major", getMajor())
                .append("Minor", getMinor())
                .append("Maintenance", getMaintenance())
                .append("Build", getBuild())
                .append("Alias", getAlias())
                .append("Branch", getBranch()).toString();
    }

    @Override
    public int hashCode() {

        return new HashCodeBuilder()
                .append(getMajor())
                .append(getMinor())
                .append(getMaintenance())
                .append(getBuild())
                .append(getAlias())
                .append(getBranch()).toHashCode();
    }

    @Override
    public boolean equals(Object o) {

        if (!(o instanceof AbstractVersion)) {
            return false;
        }

        AbstractVersion that = (AbstractVersion) o;
        return new EqualsBuilder()
                .append(this.getMajor(), that.getMajor())
                .append(this.getMinor(), that.getMinor())
                .append(this.getMaintenance(), that.getMaintenance())
                .append(this.getBuild(), that.getBuild())
                .append(this.getAlias(), that.getAlias()).isEquals();
    }

    @Override
    public int getMajor() {
        return -1;
    }

    @Override
    public int getMinor() {
        return -1;
    }

    @Override
    public int getMaintenance() {
        return -1;
    }

    @Override
    public int getBuild() {
        return -1;
    }

    @Override
    public String getAlias() {
        return null;
    }

    @Override
    public String getBranch() {
        return null;
    }

    @Override
    public String getRevision() {
        return null;
    }

    @Override
    public int getMinCoreRevision() {
        return -1;
    }
}
