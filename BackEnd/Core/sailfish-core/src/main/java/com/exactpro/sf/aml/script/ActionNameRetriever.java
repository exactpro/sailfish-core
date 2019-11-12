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
package com.exactpro.sf.aml.script;

import static net.bytebuddy.implementation.InvocationHandlerAdapter.of;
import static net.bytebuddy.matcher.ElementMatchers.anyOf;
import static net.bytebuddy.matcher.ElementMatchers.noneOf;
import static org.jooq.lambda.Unchecked.function;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jooq.lambda.fi.util.function.CheckedConsumer;

import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller.ConsumerAction;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller.ConsumerActionWithParameters;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller.FunctionAction;
import com.exactpro.sf.scriptrunner.actionmanager.IActionCaller.FunctionActionWithParameters;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;

import net.bytebuddy.ByteBuddy;

@SuppressWarnings("unchecked")
public class ActionNameRetriever {
    private static final ThreadLocal<Map<Class<? extends IActionCaller>, IActionCallerProxy>> CACHE = ThreadLocal.withInitial(HashMap::new);

    public static <T extends IActionCaller> String getMethodName(Class<? extends IActionCaller> clazz, ConsumerAction<T> method, IActionContext actionContext) throws Exception {
        return getMethodName(clazz, caller -> method.accept((T)caller, actionContext));
    }

    public static <T extends IActionCaller, R> String getMethodName(Class<? extends IActionCaller> clazz, FunctionAction<T, R> method, IActionContext actionContext) throws Exception {
        return getMethodName(clazz, caller -> method.apply((T)caller, actionContext));
    }

    public static <T extends IActionCaller, P> String getMethodName(Class<? extends IActionCaller> clazz, ConsumerActionWithParameters<T, P> method, IActionContext actionContext, P parameters) throws Exception {
        return getMethodName(clazz, caller -> method.accept((T)caller, actionContext, parameters));
    }

    public static <T extends IActionCaller, P, R> String getMethodName(Class<? extends IActionCaller> clazz, FunctionActionWithParameters<T, P, R> method, IActionContext actionContext, P parameters) throws Exception {
        return getMethodName(clazz, caller -> method.apply((T)caller, actionContext, parameters));
    }

    private static String getMethodName(Class<? extends IActionCaller> clazz, CheckedConsumer<IActionCaller> method) throws Exception {
        try {
            IActionCallerProxy proxy = getProxy(clazz);
            method.accept(proxy);
            return proxy.getMethodName();
        } catch (Exception e) {
            throw e;
        } catch (Throwable t) {
            return ExceptionUtils.rethrow(t);
        }
    }

    private static IActionCallerProxy getProxy(Class<? extends IActionCaller> actionClass) {
        return CACHE.get().computeIfAbsent(actionClass, function(clazz -> (IActionCallerProxy)new ByteBuddy()
                .subclass(clazz)
                .implement(IActionCallerProxy.class)
                .defineProperty("methodName", String.class)
                .method(anyOf(clazz.getMethods()).and(noneOf(Object.class.getMethods())))
                .intercept(of((proxy, method, args) -> {
                    ((IActionCallerProxy)proxy).setMethodName(method.getName());
                    return null;
                }))
                .make()
                .load(clazz.getClassLoader())
                .getLoaded()
                .newInstance()));
    }

    protected interface IActionCallerProxy extends IActionCaller {
        String getMethodName();

        void setMethodName(String methodName);
    }
}
