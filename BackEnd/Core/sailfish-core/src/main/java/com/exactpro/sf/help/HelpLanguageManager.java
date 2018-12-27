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
package com.exactpro.sf.help;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.ClassUtils;

import com.exactpro.sf.aml.ICodeGenerator;
import com.exactpro.sf.aml.MessageDirection;
import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.common.impl.messages.BaseMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.configuration.ILoadableManagerContext;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.suri.SailfishURIRule;
import com.exactpro.sf.configuration.suri.SailfishURIUtils;
import com.exactpro.sf.scriptrunner.actionmanager.ActionMethod;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.languagemanager.ICompatibilityChecker;
import com.exactpro.sf.scriptrunner.languagemanager.ILanguageFactory;
import com.exactpro.sf.scriptrunner.languagemanager.LanguageManager;
import com.exactpro.sf.scriptrunner.languagemanager.exceptions.LanguageManagerException;

// Used to generate help for General

class HelpLanguageManager extends LanguageManager {

    private final SailfishURI HELP_LANGUAGE_URI = SailfishURI.unsafeParse("General:HELP_LANGUAGE");

    private final ILanguageFactory HELP_LANGUAGE = new ILanguageFactory() {

        public final SailfishURI URI = HELP_LANGUAGE_URI;

        @Override public void init(ClassLoader... pluginClassLoaders) {

        }

        @Override public String getName() {
            return URI.getResourceName();
        }

        @Override public Object getReader() {
            return null;
        }

        @Override public ICodeGenerator getGenerator() {
            return null;
        }

        @Override public ICompatibilityChecker getChecker() {
            return new ICompatibilityChecker() {
                @Override public boolean isCompatible(Method method) {
                    int modifiers = method.getModifiers();

                    if (!Modifier.isPublic(modifiers) || !method.isAnnotationPresent(ActionMethod.class)) {
                        return false;
                    }

                    Class<?>[] parameterTypes = method.getParameterTypes();

                    if (parameterTypes.length != 1 && parameterTypes.length != 2) {
                        return false;
                    }

                    if (!IActionContext.class.isAssignableFrom(parameterTypes[0])) {
                        return false;
                    }

                    if (parameterTypes.length == 2) {

                        if (IMessage.class.isAssignableFrom(parameterTypes[1])) {
                            return method.isAnnotationPresent(MessageDirection.class);
                        }

                        List<Class<?>> superClasses = ClassUtils.getAllSuperclasses(parameterTypes[1]);

                        boolean isBaseMessage = BaseMessage.class.isAssignableFrom(parameterTypes[1]);

                        boolean isFixMessage = superClasses.size() >= 2 && superClasses.get(1).getCanonicalName().equals("quickfix.Message");

                        if ((isBaseMessage || isFixMessage) && method.isAnnotationPresent(MessageDirection.class)) {
                            return false;
                        }

                        return isBaseMessage || isFixMessage || HashMap.class.isAssignableFrom(parameterTypes[1]);
                    }

                    return true;
                }
            };
        }

        @Override public ClassLoader createClassLoader(URL binFolder, ClassLoader parent) throws Exception {
            return null;
        }
    };

    @SuppressWarnings("serial") private final Map<SailfishURI, ILanguageFactory> uriToFactory = new TreeMap<SailfishURI, ILanguageFactory>() {{
        put(HELP_LANGUAGE_URI, HELP_LANGUAGE);
    }};

    protected HelpLanguageManager() {
    }

    @Override
    public void load(ILoadableManagerContext context) {
        ClassLoader classLoader = context.getClassLoaders()[0];
        IVersion plugin = context.getVersion();

        try {
            for (ILanguageFactory languageFactory : ServiceLoader.load(ILanguageFactory.class, classLoader)) {
                SailfishURI languageURI = new SailfishURI(plugin.getAlias(), null, languageFactory.getName());
                uriToFactory.put(languageURI, languageFactory);
            }
        } catch(Exception e) {
            throw new LanguageManagerException("Failed to load language factories", e);
        }
    }

    @Override
    public Set<ILanguageFactory> getLanguageFactoriesByPlugin(IVersion plugin) throws SailfishURIException {
        if (plugin == null) {
            return null;
        }

        SailfishURI pluginURI = new SailfishURI(plugin.getAlias());

        return SailfishURIUtils.getMatchingValues(pluginURI, uriToFactory, SailfishURIRule.REQUIRE_PLUGIN);
    }

    @Override
    public Map<SailfishURI, ILanguageFactory> getLanguages() {
        return Collections.unmodifiableMap(uriToFactory);
    }

    public Set<ILanguageFactory> getOrigLanguageFactoriesByPlugin(IVersion plugin) throws SailfishURIException {
        Set<ILanguageFactory> set = new HashSet<>(getLanguageFactoriesByPlugin(plugin));
        set.remove(HELP_LANGUAGE);
        return set;
    }
}
