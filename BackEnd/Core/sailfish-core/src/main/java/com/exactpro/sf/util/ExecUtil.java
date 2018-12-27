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
package com.exactpro.sf.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.exactpro.sf.common.util.EPSCommonException;

/**
 * #9620: Implement an ability to execute Sailfish Util functions from console
 * java -cp testtools.jar com.exactpro.sf.util.ExecUtil com.exactpro.sf.actions.FIXMatrixUtil TransactTime "Y+2:m-6:D=4:h+1:M-2:s=39"
 * @author dmitry.guriev
 *
 *
 */
public class ExecUtil {

	public static void main (String[] args)
	{
		new ExecUtil().run(args);
	}

	private void run(String[] args)
	{
		if (args.length < 2 || args[1].equals("-h") || args[1].equals("--help")) {
			usage();
			return;
		}

		String className = args[0];
		String methodName = args[1];
		Class<?> cls;
		try {
			cls = Class.forName(className);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}

		List<String> methods = new ArrayList<String>();
		
		for (Method method : cls.getMethods())
		{
			if (method.getName().equals(methodName))
			{
				Class<?>[] pt = method.getParameterTypes();
				if (pt.length == args.length-2)
				{
					Object[] args2 = new Object[args.length-2];
					for (int i=0; i<args.length-2; i++)
					{
						args2[i] = cast(args[i+2], pt[i]);
					}
					try {
						Object result = method.invoke(null, args2);
						System.out.println(result.getClass()+": "+result);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
					return;
				}
				methods.add(method.toGenericString());
			}
		}

		if (methods.size() == 0) {
			System.out.println("No methods found.");
		} else {
			String s = methods.size() == 1 ? "" : "s";
			System.out.println("Found "+methods.size()+" method"+s+":");
		}
		for (String str : methods) {
			System.out.println(str);
		}
	}

	private Object cast(String o, Class<?> cls) {
		
		if (cls.equals(String.class)) {
			return o.toString();
		}
		if (cls.equals(BigDecimal.class)) {
			return new BigDecimal(o.toString());
		}
		if (cls.equals(double.class) || cls.equals(Double.class)) {
			return Double.parseDouble(o.toString());
		}
		if (cls.equals(float.class) || cls.equals(Float.class)) {
			return Float.parseFloat(o.toString());
		}
		if (cls.equals(long.class) || cls.equals(Long.class)) {
			return Long.parseLong(o.toString());
		}
		if (cls.equals(int.class) || cls.equals(Integer.class)) {
			return Integer.parseInt(o.toString());
		}
		if (cls.equals(short.class) || cls.equals(Short.class)) {
			return Short.parseShort(o.toString());
		}
		if (cls.equals(char.class) || cls.equals(Character.class)) {
			return o.toString().charAt(0);
		}
		if (cls.equals(byte.class) || cls.equals(Byte.class)) {
			return Byte.parseByte(o.toString());
		}
		if (cls.equals(boolean.class) || cls.equals(Boolean.class)) {
			return Boolean.parseBoolean(o.toString());
		}
		throw new EPSCommonException("Can not cast value ["+o+"] to class "+cls.getCanonicalName());
	}

	private void usage() {
		System.out.println("ExecUtil <class_name> <util_method> [args]");
	}
}
