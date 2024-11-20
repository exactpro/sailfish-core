/*
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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
 */
package com.exactpro.sf.util;

import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IKnownBug;
import com.exactpro.sf.aml.scriptutil.StaticUtil.KnownBugFilter;
import com.exactpro.sf.comparison.Convention;

import java.util.Collections;
import java.util.Set;

public abstract class AbstractBugsChecker implements IKnownBug {
    private static final BugDescription EXPECTED_ONLY_BUG_DESCRIPTION = new BugDescription("Expected only bug description");
    protected static final Set<BugDescription> EXPECTED_ONLY_BUG_DESCRIPTION_SET = Collections.singleton(EXPECTED_ONLY_BUG_DESCRIPTION);

    @Override
    public IKnownBug BugEmpty(String subject, String... categories) {
        return Bug(subject, Convention.CONV_MISSED_OBJECT, categories);
    }

    @Override
    public IKnownBug BugExistence(String subject, String... categories) {
        return Bug(subject, Convention.CONV_EXISTENCE_OBJECT, categories);
    }

    public IFilter toFilter() {
        return new KnownBugFilter(0, StringUtils.EMPTY, this);
    }
}
