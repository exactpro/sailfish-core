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
package com.exactpro.sf.util;

import java.util.Set;

import com.google.common.collect.Sets;

public class FieldKnownBugException extends KnownBugException {

    /**
     * 
     */
    private static final long serialVersionUID = 804079508424747567L;
    private final Set<BugDescription> actualDescriptions;

    /**
     * @param message
     * @param actualDescriptions
     * @param cause
     */
    public FieldKnownBugException(String message, Set<BugDescription> actualDescriptions, Throwable cause, Set<BugDescription> potentialDescriptions) {
        super(message, cause, potentialDescriptions);
        this.actualDescriptions = prepareBugDescriptions(actualDescriptions);
    }

    public FieldKnownBugException(String message, BugDescription actualDescriptions, Throwable cause, Set<BugDescription> potentialDescriptions) {
        this(message, Sets.newHashSet(actualDescriptions), cause, potentialDescriptions);
    }
    
    public FieldKnownBugException() {
        this(null, (Set<BugDescription>) null, null, null);
    }

    /**
     * @param message
     */
    public FieldKnownBugException(String message) {
        this(message, (Set<BugDescription>) null, null, null);
    }

    /**
     * @param message
     * @param actualDescriptions
     */
    public FieldKnownBugException(String message, Set<BugDescription> actualDescriptions) {
        this(message, actualDescriptions, null, null);
    }

    public FieldKnownBugException(String message, BugDescription actualDescriptions) {
        this(message, actualDescriptions, null, null);
    }
    
    /**
     * @param actualDescriptions
     * @param cause
     */
    public FieldKnownBugException(Set<BugDescription> actualDescriptions, Throwable cause) {
        this(null, actualDescriptions, cause, null);
    }

    public Set<BugDescription> getActualDescriptions() {
        return this.actualDescriptions;
    }
    
    public boolean isKnownBug() {
        return !this.actualDescriptions.isEmpty(); 
    }
}
