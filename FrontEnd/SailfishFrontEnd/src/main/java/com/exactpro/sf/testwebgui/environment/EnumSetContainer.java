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
package com.exactpro.sf.testwebgui.environment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;

import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.IServiceHandler;

/**
 * Class contains values for service parameter dropdown lists
 */
public class EnumSetContainer {

    private final Set<String> handlerClassNames;

    private final Set<SailfishURI> languagesURIs;

    private final IDictionaryManager dictionaryManager;

    public EnumSetContainer(IDictionaryManager dictionaryManager) {
        this.handlerClassNames = collectInheritorNames(IServiceHandler.class);
        this.languagesURIs = SFLocalContext.getDefault().getLanguageManager().getLanguageURIs();
        this.dictionaryManager = dictionaryManager;
    }

    public Set<SailfishURI> loadDictionaries() {
        return this.dictionaryManager.getDictionaryURIs();
    }

    private <T> Set<String> collectInheritorNames(Class<T> parent) {
        Set<String> result = new HashSet<String>();

        Reflections reflections = new Reflections("com.exactpro.sf");
        Set<Class<? extends T>> classes = reflections.getSubTypesOf(parent);
        for(Class<?> cl : classes) {
            if (!Modifier.isAbstract(cl.getModifiers()) && !Modifier.isPrivate(cl.getModifiers())) {
                result.add(cl.getName());
            }
        }
        return result;
    }

    public List<SailfishURI> completeDictURI(String query) {
        if (StringUtils.isEmpty(query)) {
            return new ArrayList<>(loadDictionaries());
        }

        List<SailfishURI> result = new ArrayList<>();
        for (SailfishURI dictUri : loadDictionaries()) {
            if (StringUtils.containsIgnoreCase(dictUri.toString(), query)) {
                result.add(dictUri);
            }
        }

        return result;
    }

    public Set<String> loadHandlerClassName() {
        return handlerClassNames;
    }

    public Set<SailfishURI> getLanguagesURIs() {
        return this.languagesURIs;
    }
}
