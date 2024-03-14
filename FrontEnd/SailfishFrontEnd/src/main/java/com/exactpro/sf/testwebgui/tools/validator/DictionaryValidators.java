/******************************************************************************
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
 ******************************************************************************/
package com.exactpro.sf.testwebgui.tools.validator;

import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.configuration.dictionary.DefaultDictionaryValidatorFactory;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidatorFactory;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.services.IStaticServiceManager;
import com.exactpro.sf.testwebgui.BeanUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DictionaryValidators {
    private static final IStaticServiceManager SERVICE_MANAGER;
    private static final List<SailfishURI> VALIDATOR_SF_URIS;
    private static final IDictionaryValidatorFactory DEFAULT_VALIDATOR_FACTORY;
    private static final SailfishURI DEFAULT_SF_URI;

    static {
        SERVICE_MANAGER = BeanUtil.getSfContext().getStaticServiceManager();
        VALIDATOR_SF_URIS = Arrays.stream(SERVICE_MANAGER.getServiceURIs())
                .sorted(ExtendedSailfishURIComparator.INSTANCE)
                .collect(Collectors.toList());
        DEFAULT_VALIDATOR_FACTORY = new DefaultDictionaryValidatorFactory();
        DEFAULT_SF_URI = SailfishURI.unsafeParse("default");
    }

    public static List<SailfishURI> loadValidatorsURIs() {
        return Collections.unmodifiableList(VALIDATOR_SF_URIS);
    }

    public static SailfishURI getDefaultValidatorURI() {
        return DEFAULT_SF_URI;
    }

    public static IDictionaryValidator createValidator(SailfishURI uri) {
        if(uri == null || uri.equals(DEFAULT_SF_URI)) {
            return DEFAULT_VALIDATOR_FACTORY.createDictionaryValidator();
        }
        return SERVICE_MANAGER.createDictionaryValidator(uri);
    }

    private static class ExtendedSailfishURIComparator implements Comparator<SailfishURI> {
        private static final ExtendedSailfishURIComparator INSTANCE = new ExtendedSailfishURIComparator();

        @Override
        public int compare(SailfishURI first, SailfishURI second) {
            boolean isFirstGen = IVersion.GENERAL.equalsIgnoreCase(first.getPluginAlias());
            boolean isSecondGen = IVersion.GENERAL.equalsIgnoreCase(second.getPluginAlias());
            int cmp = first.compareTo(second);
            if(isFirstGen && isSecondGen) {
                return cmp;
            }
            if(isFirstGen) return -1;
            if(isSecondGen) return 1;
            return cmp;
        }
    }

}