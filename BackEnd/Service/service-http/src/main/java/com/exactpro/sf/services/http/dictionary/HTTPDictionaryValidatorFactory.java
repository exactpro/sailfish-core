/******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.http.dictionary;

import com.exactpro.sf.configuration.dictionary.impl.DefaultDictionaryValidator;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidatorFactory;
import com.exactpro.sf.services.http.dictionary.impl.HTTPDictionaryValidator;

/**
 * @author oleg.smirnov
 *
 */
public class HTTPDictionaryValidatorFactory implements IDictionaryValidatorFactory {

    /* (non-Javadoc)
     * @see com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidatorFactory#createDictionaryValidator()
     */
    @Override
    public IDictionaryValidator createDictionaryValidator() {
        return new HTTPDictionaryValidator(new DefaultDictionaryValidator());
    }

}
