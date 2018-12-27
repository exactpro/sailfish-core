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
package com.exactpro.sf.aml.checkers;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import com.exactpro.sf.aml.MessageDirection;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.scriptrunner.actionmanager.ActionMethod;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.languagemanager.ICompatibilityChecker;

public class AML3Checker implements ICompatibilityChecker {

    public AML3Checker() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean isCompatible(Method method) {
        int modifiers = method.getModifiers();

        if(!Modifier.isPublic(modifiers) || !method.isAnnotationPresent(ActionMethod.class)) {
            return false;
        }

        Class<?>[] parameterTypes = method.getParameterTypes();

        if(parameterTypes.length != 1 && parameterTypes.length != 2) {
            return false;
        }

        if(!IActionContext.class.isAssignableFrom(parameterTypes[0])) {
            return false;
        }

        if(parameterTypes.length == 2) {
            if(IMessage.class.isAssignableFrom(parameterTypes[1])) {
                return method.isAnnotationPresent(MessageDirection.class);
            }

            return HashMap.class.isAssignableFrom(parameterTypes[1]);
        }

        return true;
    }

}
