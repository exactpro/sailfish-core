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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import com.exactpro.sf.aml.Description;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.ObjectUtils;

import com.exactpro.sf.configuration.suri.SailfishURI;

public class UtilityManagerUtils {
    private static final Object[] NULL_ARGUMENT_ARRAY = new Object[] { null };

    public static boolean checkArgs(Object[] args, Class<?>... argTypes) {
        return ClassUtils.isAssignable(ClassUtils.toClass(args), argTypes, true);
    }

    @SuppressWarnings("unchecked")
    public static <T> T castNumber(Object o, Class<T> toClass) {
        if(o == null) {
            return null;
        }

        Number number = (Number)o;

        if(toClass.equals(byte.class) || toClass.equals(Byte.class)) {
            return (T)(Byte)number.byteValue();
        } else if(toClass.equals(short.class) || toClass.equals(Short.class)) {
            return (T)(Short)number.shortValue();
        } else if(toClass.equals(int.class) || toClass.equals(Integer.class)) {
            return (T)(Integer)number.intValue();
        } else if(toClass.equals(long.class) || toClass.equals(Long.class)) {
            return (T)(Long)number.longValue();
        } else if(toClass.equals(float.class) || toClass.equals(Float.class)) {
            return (T)(Float)number.floatValue();
        } else if(toClass.equals(double.class) || toClass.equals(Double.class)) {
            return (T)(Double)number.doubleValue();
        }

        return (T)o;
    }

    public static Class<?>[] getVarArgsClasses(Object[] args, Class<?>... argTypes) {
        int argsLength = args != null ? args.length : 0;
        int typesLength = argTypes.length;

        if(argsLength < (typesLength - 1)) {
            return argTypes;
        }

        Class<?>[] classes = new Class<?>[argsLength];

        for(int i = 0; i < args.length; i++) {
            if(i < typesLength) {
                classes[i] = argTypes[i];
            } else {
                classes[i] = argTypes[typesLength - 1];
            }
        }

        return classes;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] getVarArgsArray(Object[] args, int startIndex, Class<? extends T> clazz) {
        int argsLength = args != null ? args.length : 0;
        int resultLength = argsLength - startIndex;

        if(resultLength <= 0) {
            return (T[])Array.newInstance(clazz, 0);
        }

        if(resultLength == 1 && args[startIndex] == null) {
            return null;
        }

        T[] result = (T[])Array.newInstance(clazz, resultLength);

        for(int i = 0; i < resultLength; i++) {
            Object value = args[i + startIndex];

            if(ClassUtils.isAssignable(clazz, Number.class, true)) {
                result[i] = (T)castNumber(value, clazz);
            } else {
                result[i] = (T)value;
            }
        }

        return result;
    }

    public static UtilityInfo getUtilityInfo(SailfishURI suri, Method utilityMethod) {
        UtilityInfo utilityInfo = new UtilityInfo();

        // In Java 8 it is possible[1] to extract parameter names... but still can fail
        // we can add parameter annotations... or use method's @Description as it done
        // in SF Help
        //
        // [1] http://stackoverflow.com/a/21455958
        Parameter[] params = utilityMethod.getParameters();
        String[] paramNames = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            paramNames[i] = params[i].getName();
        }

        utilityInfo.setURI(suri);
        utilityInfo.setParameterNames(paramNames);
        utilityInfo.setParameterTypes(utilityMethod.getParameterTypes());
        utilityInfo.setReturnType(utilityMethod.getReturnType());

        if(utilityMethod.isAnnotationPresent(Description.class)){
            Description description = utilityMethod.getAnnotation(Description.class);
            utilityInfo.setDescription(description.value());
        }

        return utilityInfo;
    }

    public static int getCompatibilityIndex(Method method, Object... args) {
        args = ObjectUtils.defaultIfNull(args, NULL_ARGUMENT_ARRAY);
        boolean varArgs = method.isVarArgs();

        Class<?>[] signatureClasses = method.getParameterTypes();
        Class<?>[] argumentsClasses = ClassUtils.toClass(args);

        int signatureLength = signatureClasses.length;
        int argumentsLength = argumentsClasses.length ;

        int compatibilityIndex = 0;

        if(signatureLength != argumentsLength) {
            if(!varArgs || (varArgs && argumentsLength < signatureLength - 1)) {
                return -1;
            }
        }

        if(varArgs) {
            int maxLength = Math.max(signatureLength, argumentsLength);

            if(signatureLength == argumentsLength) {
                int lastIndex = signatureLength - 1;
                Class<?> varArgsClass = signatureClasses[lastIndex].getComponentType();

                if(ClassUtils.isAssignable(argumentsClasses[lastIndex], varArgsClass)) {
                    signatureClasses[lastIndex] = varArgsClass;
                }
            }

            if(signatureLength < maxLength) {
                int lastIndex = signatureLength - 1;
                signatureClasses[lastIndex] = signatureClasses[lastIndex].getComponentType();
                signatureClasses = Arrays.copyOf(signatureClasses, maxLength);
                Arrays.fill(signatureClasses, signatureLength, maxLength, signatureClasses[lastIndex]);
                signatureLength = maxLength;
            }

            if(argumentsLength < maxLength) {
                argumentsClasses = Arrays.copyOf(argumentsClasses, maxLength);
                argumentsLength = maxLength;
            }
        }

        for(int i = 0; i < signatureLength; i++) {
            Class<?> signatureClass = signatureClasses[i];
            Class<?> argumentClass = argumentsClasses[i];

            if(signatureClass.isPrimitive()) {
                argumentClass = ClassUtils.wrapperToPrimitive(argumentClass);
            }

            if(signatureClass.equals(argumentClass)) {
                compatibilityIndex++;
                continue;
            }

            if(!ClassUtils.isAssignable(argumentClass, signatureClass)) {
                return -1;
            }
        }

        return compatibilityIndex;
    }

    public static Object[] getReflectionArgs(Method method, Object... args) {
        if(args == null) {
            return NULL_ARGUMENT_ARRAY;
        }

        if(!method.isVarArgs()) {
            return args;
        }

        Class<?>[] signatureClasses = method.getParameterTypes();
        int signatureLength = signatureClasses.length;

        if(signatureLength == 0) {
            return ArrayUtils.EMPTY_OBJECT_ARRAY;
        }

        int lastIndex = signatureLength - 1;
        Class<?> varArgsClass = signatureClasses[lastIndex].getComponentType();
        Object varArgsArray = getVarArgsArray(args, lastIndex, ClassUtils.primitiveToWrapper(varArgsClass));
        Object[] newArgs = new Object[signatureLength];

        System.arraycopy(args, 0, newArgs, 0, signatureLength - 1);

        if(varArgsClass.isPrimitive()) {
            if(varArgsClass.equals(boolean.class)) {
                varArgsArray = ArrayUtils.toPrimitive((Boolean[])varArgsArray);
            } else if(varArgsClass.equals(byte.class)) {
                varArgsArray = ArrayUtils.toPrimitive((Byte[])varArgsArray);
            } else if(varArgsClass.equals(short.class)) {
                varArgsArray = ArrayUtils.toPrimitive((Short[])varArgsArray);
            } else if(varArgsClass.equals(char.class)) {
                varArgsArray = ArrayUtils.toPrimitive((Character[])varArgsArray);
            } else if(varArgsClass.equals(int.class)) {
                varArgsArray = ArrayUtils.toPrimitive((Integer[])varArgsArray);
            } else if(varArgsClass.equals(float.class)) {
                varArgsArray = ArrayUtils.toPrimitive((Float[])varArgsArray);
            } else if(varArgsClass.equals(long.class)) {
                varArgsArray = ArrayUtils.toPrimitive((Long[])varArgsArray);
            } else if(varArgsClass.equals(double.class)) {
                varArgsArray = ArrayUtils.toPrimitive((Double[])varArgsArray);
            }
        }

        newArgs[lastIndex] = varArgsArray;

        return newArgs;
    }
}
