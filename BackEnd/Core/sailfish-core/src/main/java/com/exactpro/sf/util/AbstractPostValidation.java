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

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.IPostValidation;

public abstract class AbstractPostValidation implements IPostValidation {

    private final IPostValidation parentValidator;

    public AbstractPostValidation() {
        this.parentValidator = null;
    }

    public AbstractPostValidation(IPostValidation parentValidator) {
        this.parentValidator = parentValidator;
    }

    @Override
    public void doValidate(IMessage message, IMessage filter, ComparatorSettings settings, ComparisonResult result) {
        if (parentValidator != null) {
            parentValidator.doValidate(message, filter, settings, result);
        }
    }

}
