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
package com.exactpro.sf.scriptrunner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.exactpro.sf.common.impl.messages.BaseMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.scriptrunner.actionmanager.ActionMethod;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.exceptions.ActionCallException;
import com.exactpro.sf.scriptrunner.actionmanager.exceptions.ActionManagerException;
import com.exactpro.sf.scriptrunner.actionmanager.exceptions.ActionNotFoundException;
import com.exactpro.sf.scriptrunner.utilitymanager.IUtilityCaller;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityManagerUtils;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityMethod;
import com.exactpro.sf.scriptrunner.utilitymanager.exceptions.UtilityCallException;
import com.exactpro.sf.scriptrunner.utilitymanager.exceptions.UtilityNotFoundException;
import com.exactpro.sf.util.KnownBugException;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public abstract class AbstractCaller implements IActionCaller, IUtilityCaller {
    private static final Class<?>[] NULL_ARGUMENT_ARRAY = new Class<?>[] { null };

    private final Map<String, Method> withSettings = new HashMap<>();
    private final Map<String, Method> withIMessage = new HashMap<>();
    private final Map<String, Method> withHashMap = new HashMap<>();
    private final Multimap<String, Method> withBaseMessage = HashMultimap.create();
    private final Multimap<String, Method> withObject = HashMultimap.create();
    private final Multimap<String, Method> utilityMethods = HashMultimap.create();

    public AbstractCaller() {
        try {
            Method[] classMethods = getClass().getMethods();

            for(Method classMethod : classMethods) {
                if(classMethod.isAnnotationPresent(ActionMethod.class)) {
                    Class<?>[] types = classMethod.getParameterTypes();

                    if(ClassUtils.isAssignable(types, IActionContext.class)) {
                        withSettings.put(classMethod.getName(), classMethod);
                    } else if(ClassUtils.isAssignable(types, IActionContext.class, IMessage.class)) {
                        withIMessage.put(classMethod.getName(), classMethod);
                    } else if(ClassUtils.isAssignable(types, IActionContext.class, BaseMessage.class)) {
                        withBaseMessage.put(classMethod.getName(), classMethod);
                    } else if(ClassUtils.isAssignable(types, IActionContext.class, HashMap.class)) {
                        withHashMap.put(classMethod.getName(), classMethod);
                    } else if(ClassUtils.isAssignable(types, IActionContext.class, Object.class)) {
                        withObject.put(classMethod.getName(), classMethod);
                    } else {
                        throw new ActionManagerException("Unknown action method signature: " + getSignature(classMethod.getName(), types));
                    }
                } else if(classMethod.isAnnotationPresent(UtilityMethod.class)) {
                    utilityMethods.put(classMethod.getName(), classMethod);
                }
            }
        } catch(Exception e) {
            throw new EPSCommonException(e);
        }
    }

    @Override
    public final <T> T call(String actionName, IActionContext actionContext) throws ActionCallException, ActionNotFoundException, InterruptedException {
        return call(actionName, withSettings, actionContext);
    }

    @Override
    public final <T> T call(String actionName, IActionContext actionContext, IMessage iMessage) throws ActionCallException, ActionNotFoundException, InterruptedException {
        return call(actionName, withIMessage, actionContext, iMessage);
    }

    @Override
    public final <T> T call(String actionName, IActionContext actionContext, BaseMessage baseMessage) throws ActionCallException, ActionNotFoundException, InterruptedException {
        return call(actionName, withBaseMessage, actionContext, baseMessage);
    }

    @Override
    public final <T> T call(String actionName, IActionContext actionContext, Object message) throws ActionCallException, ActionNotFoundException, InterruptedException {
        return call(actionName, withObject, actionContext, message);
    }

    @Override
    public final <T> T call(String actionName, IActionContext actionContext, HashMap<?, ?> hashMap) throws ActionCallException, ActionNotFoundException, InterruptedException {
        return call(actionName, withHashMap, actionContext, hashMap);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> T call(String utilityName, Object... args) throws UtilityCallException, UtilityNotFoundException, InterruptedException {
        int maxCompatibilityIndex = -1;
        Method bestMethod = null;

        for(Method utilityMethod : utilityMethods.get(utilityName)) {
            int compatibilityIndex = UtilityManagerUtils.getCompatibilityIndex(utilityMethod, args);

            if(compatibilityIndex > maxCompatibilityIndex) {
                maxCompatibilityIndex = compatibilityIndex;
                bestMethod = utilityMethod;
            }
        }

        if(maxCompatibilityIndex == -1) {
            throw new UtilityNotFoundException(getSignature(utilityName, args));
        }

        try {
            return (T)bestMethod.invoke(this, UtilityManagerUtils.getReflectionArgs(bestMethod, args));
        } catch(Exception e) {
            int interruptedExceptionIndex = ExceptionUtils.indexOfThrowable(e, InterruptedException.class);

            if(interruptedExceptionIndex != -1) {
                throw (InterruptedException)ExceptionUtils.getThrowableList(e).get(interruptedExceptionIndex);
            } else {
                if(e instanceof InvocationTargetException) {
                    throw new UtilityCallException(e.getCause());
                }

                throw new UtilityCallException(e);
            }
        }
    }

    private <T> T call(String actionName, Map<String, Method> methodMap, Object... args) throws InterruptedException {
        Method actionMethod = methodMap.get(actionName);

        if(actionMethod != null) {
            return call(actionMethod, args);
        }

        throw new ActionNotFoundException(getSignature(actionName, args));
    }

    private <T> T call(String actionName, Multimap<String, Method> methodMap, Object... args) throws InterruptedException {
        Collection<Method> actionMethods = methodMap.get(actionName);
        Class<?>[] argTypes = ClassUtils.toClass(args);

        for(Method actionMethod : actionMethods) {
            if(ClassUtils.isAssignable(argTypes, actionMethod.getParameterTypes())) {
                return call(actionMethod, args);
            }
        }

        throw new ActionNotFoundException(getSignature(actionName, args));
    }

    @SuppressWarnings("unchecked")
    private <T> T call(Method method, Object... args) throws InterruptedException {
        try {
            return (T)method.invoke(this, args);
        } catch(Exception e) {
            int interruptedExceptionIndex = ExceptionUtils.indexOfThrowable(e, InterruptedException.class);

            if(interruptedExceptionIndex != -1) {
                throw (InterruptedException)ExceptionUtils.getThrowableList(e).get(interruptedExceptionIndex);
            } else {
                int knownBugExceptionIndex = ExceptionUtils.indexOfType(e, KnownBugException.class);

                if (knownBugExceptionIndex != -1) {
                    throw (KnownBugException)ExceptionUtils.getThrowableList(e).get(knownBugExceptionIndex);
                } else {
                    if(e instanceof InvocationTargetException) {
                        throw new ActionCallException(e.getCause());
                    }

                    throw new ActionCallException(e);
                }
            }
        }
    }

    private String getSignature(String methodName, Object... args) {
        return getSignature(methodName, ClassUtils.toClass(args));
    }

    private String getSignature(String methodName, Class<?>[] argTypes) {
        argTypes = ObjectUtils.defaultIfNull(argTypes, NULL_ARGUMENT_ARRAY);
        StringBuilder signature = new StringBuilder();

        signature.append(methodName);
        signature.append('(');

        List<String> classNames = ClassUtils.convertClassesToClassNames(Arrays.asList(argTypes));

        signature.append(String.join(", ", classNames));
        signature.append(')');

        return signature.toString();
    }
}
