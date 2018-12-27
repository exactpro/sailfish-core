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
package com.exactpro.sf.scriptrunner.actionmanager;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.AMLLangConst;
import com.exactpro.sf.aml.legacy.ActionDefinition;
import com.exactpro.sf.aml.legacy.Actions;
import com.exactpro.sf.aml.legacy.ClassName;
import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.common.impl.messages.BaseMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.configuration.ILoadableManager;
import com.exactpro.sf.configuration.ILoadableManagerContext;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.suri.SailfishURIRule;
import com.exactpro.sf.configuration.suri.SailfishURIUtils;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.exceptions.ActionCallException;
import com.exactpro.sf.scriptrunner.actionmanager.exceptions.ActionManagerException;
import com.exactpro.sf.scriptrunner.actionmanager.exceptions.ActionNotFoundException;
import com.exactpro.sf.scriptrunner.languagemanager.ILanguageFactory;
import com.exactpro.sf.scriptrunner.languagemanager.LanguageManager;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityClass;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityInfo;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityManager;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class ActionManager implements IActionManager, ILoadableManager {
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(ActionManager.class);

    private final Map<ActionDefinition, PluginComponent> actionsToLoad = new HashMap<>();
    private final Map<SailfishURI, Class<? extends IActionCaller>> uriToClass = new ConcurrentHashMap<>();
    private final SetMultimap<SailfishURI, SailfishURI> uriToUtilites = HashMultimap.create();
    private final SetMultimap<SailfishURI, ActionInfo> uriToInfos = HashMultimap.create();
    private final Map<SailfishURI, ActionClass> uriToActionClass = new HashMap<>();

    private final ThreadLocal<Map<Class<? extends IActionCaller>, IActionCaller>> classToInstance = new ThreadLocal<Map<Class<? extends IActionCaller>, IActionCaller>>() {
        @Override
        protected Map<Class<? extends IActionCaller>, IActionCaller> initialValue() {
            return new HashMap<>();
        };
    };

    private class PluginComponent {
        private final ClassLoader loader;
        private final IVersion version;

        public PluginComponent(ClassLoader loader, IVersion version) {
            this.loader = loader;
            this.version = version;
        }
    }

    private final UtilityManager utilityManager;
    private final LanguageManager languageManager;

    public ActionManager(UtilityManager utilityManager, LanguageManager languageManager) {
    	this.utilityManager = Objects.requireNonNull(utilityManager, "utilityManager cannot be null");
        this.languageManager = Objects.requireNonNull(languageManager, "languageManager cannot be null");
	}

    /**
     * Loads action definitions from specified input stream.
     * During this process only action definitions will be loaded and no actual class loading will be performed.
     *
     * @param classLoader   class loader to load action classes later
     * @param inputStream   input stream with action definitions
     * @param version        plugin info
     */
    @Override
    public void load(ILoadableManagerContext context) {
        ClassLoader classLoader = context.getClassLoaders()[0];
        InputStream inputStream = context.getResourceStream();
        IVersion version = context.getVersion();

		try {
    		Unmarshaller unmarshaller = JAXBContext.newInstance(Actions.class).createUnmarshaller();
        	JAXBElement<Actions> root = unmarshaller.unmarshal(new StreamSource(inputStream), Actions.class);
        	PluginComponent component = new PluginComponent(classLoader, version);

            for (ActionDefinition action : root.getValue().getAction()) {
                actionsToLoad.put(action, component);
            }
        } catch (JAXBException e) {
        	throw new ActionManagerException("Failed to load actions", e);
		}
    }

    @Override
    public void finalize(ILoadableManagerContext context) throws Exception {
        Map<SailfishURI, ILanguageFactory> languages = languageManager.getLanguages();

        for (ILanguageFactory languageFactory : languages.values()) {
            languageFactory.init(context.getClassLoaders());
        }

        registerLanguages(languages);
    }

    /**
     * Registers specified languages.
     * During this process we load actions using info from action definitions and check them for compatibility against specified languages.
     *
     * @param languageFactories factories of languages to register
     */
    private void registerLanguages(Map<SailfishURI, ILanguageFactory> languageFactories) throws SailfishURIException {
        for (Entry<ActionDefinition, PluginComponent> entry : actionsToLoad.entrySet()) {
            ActionDefinition action = entry.getKey();
            ClassLoader classLoader = entry.getValue().loader;
            IVersion version = entry.getValue().version;

            String classDescription = action.getDescription();
            String className = action.getActionClassName().getName();

            List<SailfishURI> classURIs = new ArrayList<>();

            Class<?> clazz;
            ActionClass actionClass = new ActionClass(className, classDescription, version);

            try {
                clazz = classLoader.loadClass(className);
                Class<? extends IActionCaller> subclass = clazz.asSubclass(IActionCaller.class);
                ResourceAliases resourceAliases = subclass.getAnnotation(ResourceAliases.class);

                if(resourceAliases == null) {
                    throw new ActionManagerException("No resource annotation for action class: " + className);
                }

                String[] classAliases = resourceAliases.value();

                if(classAliases.length == 0) {
                    throw new ActionManagerException("No resource aliases for action class: " + className);
                }

                for(String classAlias : classAliases) {
                    SailfishURI classURI = new SailfishURI(version.getAlias(), classAlias);

                    if(uriToClass.containsKey(classURI)) {
                        throw new ActionManagerException("Action class is already loaded: " + classURI);
                    }

                    classURIs.add(classURI);
                    uriToClass.put(classURI, subclass);
                    actionClass.addClassAlias(classAlias);
                    uriToActionClass.put(classURI, actionClass);
                }
            } catch(ClassCastException | ClassNotFoundException e) {
                throw new ActionManagerException("Failed to load action class: " + className, e);
            }

            for(Method actionMethod : clazz.getMethods()) {
                Set<SailfishURI> compatibleLanguages;

                // hack to load System_DefineMessage action
                if(actionMethod.getName().equals(AMLLangConst.DEFINE_MESSAGE_ACTION_URI.getResourceName())) {
                    compatibleLanguages = new HashSet<>();

                    for(SailfishURI languageURI : languageFactories.keySet()) {
                        compatibleLanguages.add(languageURI);
                    }
                } else {
                    compatibleLanguages = getCompatibleLanguages(languageFactories, actionMethod);
                }

                if(!compatibleLanguages.isEmpty()) {
                    for(SailfishURI classURI : classURIs) {
                        SailfishURI actionURI = new SailfishURI(version.getAlias(), classURI.getClassAlias(), actionMethod.getName());
                        ActionInfo actionInfo = ActionManagerUtils.getActionInfo(actionURI, actionMethod, compatibleLanguages);

                        if(!uriToInfos.put(actionURI, actionInfo)) {
                            throw new ActionManagerException("Action is already loaded: " + actionInfo);
                        }
                    }

                    actionClass.addClassMethod(actionMethod);
                }
            }

            for(ClassName utilityClassName : action.getUtilityClassName()) {
                UtilityClass utilityClass = utilityManager.load(classLoader, utilityClassName.getName(), version);

                for(String utilityClassAlias : utilityClass.getClassAliases()) {
                    SailfishURI utilityClassURI = new SailfishURI(version.getAlias(), utilityClassAlias);

                    for(SailfishURI classURI : classURIs) {
                        uriToUtilites.put(classURI, utilityClassURI);
                    }
                }

                actionClass.addUtilityClass(utilityClass);
            }
        }

        actionsToLoad.clear();
    }

    private Set<SailfishURI> getCompatibleLanguages(Map<SailfishURI, ILanguageFactory> languageFactories, Method method) {
        Set<SailfishURI> compatibleLanguages = new HashSet<>();

        for(Entry<SailfishURI, ILanguageFactory> e : languageFactories.entrySet()) {
            if(e.getValue().getChecker().isCompatible(method)) {
                compatibleLanguages.add(e.getKey());
            }
        }

        return Collections.unmodifiableSet(compatibleLanguages);
    }

    private IActionCaller getInstance(SailfishURI uri) {
        Class<? extends IActionCaller> clazz = SailfishURIUtils.getMatchingValue(uri, uriToClass, SailfishURIRule.REQUIRE_CLASS);

        if(clazz == null) {
            throw new ActionManagerException("Cannot find action class for URI: " +  uri);
        }

        IActionCaller instance = classToInstance.get().get(clazz);

        if(instance == null) {
            try {
                instance = clazz.newInstance();
            } catch(InstantiationException | IllegalAccessException e) {
                throw new ActionManagerException("Failed to instantiate action class for URI: " + uri);
            }

            classToInstance.get().put(clazz, instance);
        }

        return instance;
    }

    @Override
    public <T> T call(SailfishURI uri, IActionContext actionContext) throws ActionCallException, ActionNotFoundException, InterruptedException {
        SailfishURIUtils.checkURI(uri, SailfishURIRule.REQUIRE_CLASS, SailfishURIRule.REQUIRE_RESOURCE);
        return getInstance(uri).call(uri.getResourceName(), actionContext);
    }

    @Override
    public <T> T call(SailfishURI uri, IActionContext actionContext, IMessage message) throws ActionCallException, ActionNotFoundException, InterruptedException {
        SailfishURIUtils.checkURI(uri, SailfishURIRule.REQUIRE_CLASS, SailfishURIRule.REQUIRE_RESOURCE);
        return getInstance(uri).call(uri.getResourceName(), actionContext, message);
    }

    @Override
    public <T> T call(SailfishURI uri, IActionContext actionContext, BaseMessage message) throws ActionCallException, ActionNotFoundException, InterruptedException {
        SailfishURIUtils.checkURI(uri, SailfishURIRule.REQUIRE_CLASS, SailfishURIRule.REQUIRE_RESOURCE);
        return getInstance(uri).call(uri.getResourceName(), actionContext, message);
    }

    @Override
    public <T> T call(SailfishURI uri, IActionContext actionContext, Object message) throws ActionCallException, ActionNotFoundException, InterruptedException {
        SailfishURIUtils.checkURI(uri, SailfishURIRule.REQUIRE_CLASS, SailfishURIRule.REQUIRE_RESOURCE);
        return getInstance(uri).call(uri.getResourceName(), actionContext, message);
    }

    @Override
    public <T> T call(SailfishURI uri, IActionContext actionContext, HashMap<?, ?> map) throws ActionCallException, ActionNotFoundException, InterruptedException {
        SailfishURIUtils.checkURI(uri, SailfishURIRule.REQUIRE_CLASS, SailfishURIRule.REQUIRE_RESOURCE);
        return getInstance(uri).call(uri.getResourceName(), actionContext, map);
    }

    @Override
    public void reset() {
        classToInstance.get().clear();
    }

    @Override
    public Set<ActionInfo> getActionInfos(SailfishURI uri) {
        return SailfishURIUtils.getMatchingValues(uri, uriToInfos, SailfishURIRule.REQUIRE_RESOURCE);
    }

    @Override
    public Set<ActionInfo> getActionInfosByClass(SailfishURI uri) {
        return SailfishURIUtils.getMatchingValues(uri, uriToInfos, SailfishURIRule.REQUIRE_PLUGIN, SailfishURIRule.REQUIRE_CLASS);

    }

    @Override
    public ActionInfo getActionInfo(SailfishURI uri, SailfishURI languageURI) {
        Set<ActionInfo> actionInfos = getActionInfos(uri);

        if(actionInfos.isEmpty()) {
            return null;
        }

        if(languageURI == null) {
            return actionInfos.iterator().next();
        }

        SailfishURIUtils.checkURI(languageURI, SailfishURIRule.REQUIRE_RESOURCE);

        for(ActionInfo actionInfo : actionInfos) {
            if(actionInfo.isLanguageCompatible(languageURI, false)) {
                return actionInfo;
            }
        }

        return null;
    }

    @Override
    public ActionInfo getActionInfo(SailfishURI uri) {
        return getActionInfo(uri, null);
    }

    @Override
    public boolean containsAction(SailfishURI uri, SailfishURI languageURI) {
        return getActionInfo(uri, languageURI) != null;
    }

    @Override
    public boolean containsAction(SailfishURI uri) {
        return getActionInfo(uri) != null;
    }

    @Override
    public UtilityInfo getUtilityInfo(SailfishURI classURI, SailfishURI utilityURI, Class<?>... argTypes) throws SailfishURIException {
        if(utilityURI.isAbsolute()) {
            return utilityManager.getUtilityInfo(utilityURI, argTypes);
        }

        Set<SailfishURI> utilityURIs = SailfishURIUtils.getMatchingValues(classURI, uriToUtilites, SailfishURIRule.REQUIRE_CLASS);

        if(utilityURIs == null) {
            return null;
        }

        for(SailfishURI utilityClassSuri : utilityURIs) {
            UtilityInfo utilityInfo = utilityManager.getUtilityInfo(utilityURI.merge(utilityClassSuri), argTypes);

            if(utilityInfo != null) {
                return utilityInfo;
            }
        }

        return null;
    }

    @Override
    public boolean containsUtility(SailfishURI classURI, SailfishURI utilityURI, Class<?>... argTypes) throws SailfishURIException {
        return getUtilityInfo(classURI, utilityURI, argTypes) != null;
    }

    @Override
    public ActionClass getActionClassByName(String className) {
        if(className == null) {
            return null;
        }

        for(ActionClass actionClass : uriToActionClass.values()) {
            if(actionClass.getClassName().equals(className)) {
                return actionClass;
            }
        }

        return null;
    }

    @Override
    public ActionClass getActionClassByURI(SailfishURI uri) {
        return SailfishURIUtils.getMatchingValue(uri, uriToActionClass, SailfishURIRule.REQUIRE_CLASS);
    }

    @Override
    public List<ActionClass> getActionClasses() {
        return new ArrayList<>(uriToActionClass.values());
    }

    @Override
    public Set<SailfishURI> getUtilityURIs(SailfishURI actionURI) {
        return new HashSet<>(uriToUtilites.get(actionURI));
    }

    @Override
    public List<ActionInfo> getActionInfos() {
        return new ArrayList<>(uriToInfos.values());
    }

    @Override
    public Set<SailfishURI> getActionClassURIs(){
        return Collections.unmodifiableSet(uriToActionClass.keySet());
    }
}
