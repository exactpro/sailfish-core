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
package com.exactpro.sf.scriptrunner.utilitymanager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.suri.SailfishURIRule;
import com.exactpro.sf.configuration.suri.SailfishURIUtils;
import com.exactpro.sf.scriptrunner.actionmanager.exceptions.ActionManagerException;
import com.exactpro.sf.scriptrunner.utilitymanager.exceptions.UtilityCallException;
import com.exactpro.sf.scriptrunner.utilitymanager.exceptions.UtilityManagerException;
import com.exactpro.sf.scriptrunner.utilitymanager.exceptions.UtilityNotFoundException;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class UtilityManager implements IUtilityManager {
    private final Map<SailfishURI, Class<? extends IUtilityCaller>> uriToClass = new ConcurrentHashMap<>();
    private final SetMultimap<SailfishURI, UtilityInfo> uriToInfos = HashMultimap.create();
    private final Map<SailfishURI, UtilityClass> uriToUtilityClass = new HashMap<>();

    private final ThreadLocal<Map<Class<? extends IUtilityCaller>, IUtilityCaller>> classToInstance = new ThreadLocal<Map<Class<? extends IUtilityCaller>, IUtilityCaller>>() {
        @Override
        protected Map<Class<? extends IUtilityCaller>, IUtilityCaller> initialValue() {
            return new HashMap<>();
        };
    };

    public UtilityManager() {}

    /**
     * Loads utilities from specified class
     *
     * @param classLoader   class loader used for utility class loading
     * @param className     utility class name
     * @param version        plugin info
     */
    public UtilityClass load(ClassLoader classLoader, String className, IVersion version) throws SailfishURIException {
        List<SailfishURI> classURIs = new ArrayList<>();

        Class<?> clazz;
        UtilityClass utilityClass;

        try {
            clazz = classLoader.loadClass(className);
            utilityClass = new UtilityClass(className, clazz.getSimpleName(), version);
            Class<? extends IUtilityCaller> subclass = clazz.asSubclass(IUtilityCaller.class);
            ResourceAliases resourceAliases = subclass.getAnnotation(ResourceAliases.class);

            if(resourceAliases == null) {
                throw new ActionManagerException("No resource annotation for utility class: " + className);
            }

            String[] classAliases = resourceAliases.value();

            if(classAliases.length == 0) {
                throw new ActionManagerException("No resource aliases for utility class: " + className);
            }

            for(String classAlias : classAliases) {
                SailfishURI classURI = new SailfishURI(version.getAlias(), classAlias);

                if(uriToClass.containsKey(classURI)) {
                    if(!uriToClass.get(classURI).equals(clazz)) {
                        throw new UtilityManagerException("Utility class is already loaded: " + classURI);
                    }

                    return uriToUtilityClass.get(classURI);
                }

                classURIs.add(classURI);
                uriToClass.put(classURI, subclass);
                utilityClass.addClassAlias(classAlias);
                uriToUtilityClass.put(classURI, utilityClass);
            }
        } catch(ClassNotFoundException e) {
            throw new UtilityManagerException("Failed to load utility class: " + className, e);
        }

        for(Method utilityMethod : clazz.getMethods()) {
            if(utilityMethod.isAnnotationPresent(UtilityMethod.class)) {
                for(SailfishURI classURI : classURIs) {
                    SailfishURI utilityURI = new SailfishURI(version.getAlias(), classURI.getClassAlias(), utilityMethod.getName());
                    UtilityInfo utilityInfo = UtilityManagerUtils.getUtilityInfo(utilityURI, utilityMethod);

                    if(!uriToInfos.put(utilityURI, utilityInfo)) {
                        throw new UtilityManagerException("Utility is already loaded: " + utilityURI);
                    }
                }

                utilityClass.addClassMethod(utilityMethod);
            }
        }

        return utilityClass;
    }

    // it's public because of the way we call utility functions in AML v2
    @Override
    public IUtilityCaller getInstance(SailfishURI uri) {
        Class<? extends IUtilityCaller> clazz = SailfishURIUtils.getMatchingValue(uri, uriToClass, SailfishURIRule.REQUIRE_CLASS);

        if(clazz == null) {
            throw new ActionManagerException("Cannot find utility class for URI: " + uri);
        }

        IUtilityCaller instance = classToInstance.get().get(clazz);

        if(instance == null) {
            try {
                instance = clazz.newInstance();
            } catch(InstantiationException | IllegalAccessException e) {
                throw new UtilityManagerException("Failed to instantiate utility class for URI: " + uri);
            }

            classToInstance.get().put(clazz, instance);
        }

        return instance;
    }

    @Override
    public <T> T call(SailfishURI uri, Object... args) throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        SailfishURIUtils.checkURI(uri, SailfishURIRule.REQUIRE_CLASS, SailfishURIRule.REQUIRE_RESOURCE);
        return getInstance(uri).call(uri.getResourceName(), args);
    }

    @Override
    public void reset() {
        classToInstance.get().clear();
    }

    // TODO: add argTypes check
    @Override
    public UtilityInfo getUtilityInfo(SailfishURI uri, Class<?>... argTypes) {
        return SailfishURIUtils.getMatchingValue(uri, uriToInfos, SailfishURIRule.REQUIRE_RESOURCE);
    }

    // TODO: add argTypes check
    @Override
    public boolean containsUtility(SailfishURI uri, Class<?>... argTypes) {
        return getUtilityInfo(uri, argTypes) != null;
    }

    @Override
    public UtilityClass getUtilityClassByName(String className) {
        for(UtilityClass utilityClass : uriToUtilityClass.values()) {
            if(utilityClass.getClassName().equals(className)) {
                return utilityClass;
            }
        }

        return null;
    }

    @Override
    public UtilityClass getUtilityClassByURI(SailfishURI uri) {
        return SailfishURIUtils.getMatchingValue(uri, uriToUtilityClass, SailfishURIRule.REQUIRE_CLASS);
    }

    @Override
    public List<UtilityClass> getUtilityClasses() {
        return new ArrayList<>(uriToUtilityClass.values());
    }

    @Override
    public List<SailfishURI> getUtilityURIs() {
        return new ArrayList<>(uriToUtilityClass.keySet());
    }

    @Override
    public Set<UtilityInfo> getUtilityInfos(SailfishURI utilityURI) {
        return SailfishURIUtils.getMatchingValues(utilityURI, uriToInfos, SailfishURIRule.REQUIRE_PLUGIN, SailfishURIRule.REQUIRE_CLASS);
    }
}
