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

package com.exactpro.sf.aml.scriptutil;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.exactpro.sf.util.BugDescription;

public class ExpressionResult {

    public static final ExpressionResult EXPRESSION_RESULT_FALSE = new ExpressionResult(false);
    public static final ExpressionResult EXPRESSION_RESULT_TRUE = new ExpressionResult(true);

    private final boolean result;
    
    private final List<?> embeddedListFilter;

    private final String description;

    private final Throwable cause;

    private final Set<BugDescription> actualDescriptions;

    private final Set<BugDescription> potentialDescriptions;

    public ExpressionResult(boolean result, List<?> embeddedListFilter, String description, Throwable cause, Set<BugDescription> actualDescriptions, Set<BugDescription> potentialDescriptions) {
        this.result = result;
        this.embeddedListFilter = embeddedListFilter;
        this.description = description;
        this.cause = cause;
        this.actualDescriptions = ObjectUtils.defaultIfNull(actualDescriptions, Collections.emptySet());
        this.potentialDescriptions = ObjectUtils.defaultIfNull(potentialDescriptions, Collections.emptySet());
    }

    public ExpressionResult(boolean result, String description, Throwable cause, Set<BugDescription> actualDescriptions, Set<BugDescription> potentialDescriptions) {
        this(result, null, description, cause, actualDescriptions, potentialDescriptions);
    }
    
    public ExpressionResult(boolean result, String description) {
        this(result, description, null, null, null);
    }

    private ExpressionResult(boolean result) {
        this(result, null);
    }

    public static ExpressionResult create(boolean result) {
        return result ? EXPRESSION_RESULT_TRUE : EXPRESSION_RESULT_FALSE;
    }

    public boolean getResult() {
        return result;
    }

    public List<?> getEmbeddedListFilter() {
        return embeddedListFilter;
    }

    public String getDescription() {
        return description;
    }

    public Throwable getCause() {
        return cause;
    }

    public Set<BugDescription> getActualDescriptions() {
        return actualDescriptions;
    }

    public Set<BugDescription> getPotentialDescriptions() {
        return potentialDescriptions;
    }

    public boolean hasBugsInfo() {
        return !potentialDescriptions.isEmpty();
    }

    public boolean hasKnownBugsInfo() {
        return !actualDescriptions.isEmpty();
    }

    public boolean equals(Object o) {
        if(o == this) {
            return true;
        }

        if(!(o instanceof ExpressionResult)) {
            return false;
        }

        ExpressionResult that = (ExpressionResult) o;
        return new EqualsBuilder()
                .append(this.result, that.result)
                .append(this.description, that.description)
                .append(this.actualDescriptions, that.actualDescriptions)
                .append(this.potentialDescriptions, that.potentialDescriptions)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.result)
                .append(this.description)
                .append(this.actualDescriptions)
                .append(this.potentialDescriptions)
                .toHashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder()
                .append("Result: ").append(result).append(", ")
                .append("Description: ").append(description).append(", ");
        if (embeddedListFilter != null) {
            builder.append("Embedded list filter: ").append(embeddedListFilter.size()).append(", ");
        }
        if (cause != null) {
            builder.append("Root cause: ").append(ExceptionUtils.getRootCause(cause).getMessage()).append(", ");
        }
        if (!potentialDescriptions.isEmpty()) {
            builder.append("Potential bugs: ").append(potentialDescriptions).append(", ");
        }
        if (!actualDescriptions.isEmpty()) {
            builder.append("Actual bugs: ").append(actualDescriptions).append(", ");
        }

        return builder.toString();
    }
}
