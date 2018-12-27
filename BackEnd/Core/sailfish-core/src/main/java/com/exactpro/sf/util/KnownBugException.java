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

import java.util.Collections;
import java.util.Set;

/**
 * @author sergey.vasiliev
 *
 */
public class KnownBugException extends RuntimeException {
    /**
     * 
     */
    private static final long serialVersionUID = -8369154089701068162L;

    private final Set<BugDescription> potentialDescriptions;

    public KnownBugException(String message, Throwable cause, Set<BugDescription> potentialDescriptions) {
        super(message, cause, false, false);
        this.potentialDescriptions = prepareBugDescriptions(potentialDescriptions);
    }

    public KnownBugException() {
        this(null, null, null);
    }

    /**
     * @param message
     */
    public KnownBugException(String message) {
        this(message, null, null);
    }

    /**
     * @param description
     * @param cause
     */
    public KnownBugException(Throwable cause) {
        this(null, cause, null);
    }

    /**
     * @param message
     * @param descriptions
     */
    public KnownBugException(String message, Set<BugDescription> descriptions) {
        this(message, null, descriptions);
    }

    /**
     * @param message
     * @param cause
     */
    public KnownBugException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public Set<BugDescription> getPotentialDescriptions() {
        return potentialDescriptions;
    }
    
    /**
     * Please use {@link #getPotentialDescriptions()} method
     * @return
     */
    @Deprecated
    public Set<BugDescription> getDescriptions() {
        return potentialDescriptions;
    }

    protected static Set<BugDescription> prepareBugDescriptions(Set<BugDescription> descriptions) {
        return descriptions != null && !descriptions.isEmpty() ? Collections.unmodifiableSet(descriptions) : Collections.emptySet();
    }
}
