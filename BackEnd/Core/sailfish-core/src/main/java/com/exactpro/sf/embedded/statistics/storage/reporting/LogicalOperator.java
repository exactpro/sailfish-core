/*******************************************************************************
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

package com.exactpro.sf.embedded.statistics.storage.reporting;

import com.exactpro.sf.embedded.statistics.storage.utils.Conditions;
import com.exactpro.sf.embedded.statistics.storage.utils.ICondition;

public enum LogicalOperator {
    OR {
        @Override
        public ICondition create(ICondition left, ICondition right) {
            return Conditions.or(left, right);
        }
    },
    AND {
        @Override
        public ICondition create(ICondition left, ICondition right) {
            return Conditions.and(left, right);
        }
    };

    public abstract ICondition create(ICondition left, ICondition right);
}
