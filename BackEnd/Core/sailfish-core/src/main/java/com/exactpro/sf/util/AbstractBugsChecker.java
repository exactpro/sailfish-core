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

import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.aml.scriptutil.StaticUtil;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.aml.scriptutil.StaticUtil.IKnownBug;
import com.exactpro.sf.comparison.Convention;

public abstract class AbstractBugsChecker implements IKnownBug {

    @Override
    public IKnownBug BugEmpty(String subject, String... categories) {
        return Bug(subject, Convention.CONV_MISSED_OBJECT, categories);
    }

    public IFilter toFilter() {
        return new StaticUtil.KnownBugFilter(0, StringUtils.EMPTY, this);
    }
}
