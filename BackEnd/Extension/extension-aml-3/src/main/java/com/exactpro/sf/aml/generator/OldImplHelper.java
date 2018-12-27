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
package com.exactpro.sf.aml.generator;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.AMLAction;
import com.exactpro.sf.aml.AMLTestCase;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;
import com.exactpro.sf.aml.generator.matrix.RefParameter;
import com.exactpro.sf.aml.generator.matrix.Value;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.scriptrunner.actionmanager.ActionInfo;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;
import com.exactpro.sf.scriptrunner.utilitymanager.IUtilityManager;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityInfo;

public class OldImplHelper {

	private static final Logger logger = LoggerFactory.getLogger(OldImplHelper.class);

	/**
	 * Value starts with this string should interpreted as java code
	 * and will be used without transformation.
	 * Before insertion it should be checked via compilation.
	 */
	private static final String TAG_INTERPRET_AS_JAVA = "java:";

	private static final String BEGIN_REFERENCE = "${";
	private static final String END_REFERENCE = "}";

	private static final String BEGIN_STATIC = "%{";
	private static final String END_STATIC = "}";

	private static final String BEGIN_FUNCTION = "#{";
	private static final String END_FUNCTION = "}";

	static void substituteReference(AMLTestCase tc, AMLAction action, AlertCollector alertCollector, String column, Value value, Set<String> definedReferences, IDictionaryManager dictionaryManager, IActionManager actionManager, IUtilityManager utilityManager) throws SailfishURIException
	{
		logger.debug("substituteReference: column: {}; Value: {}", column, value.getValue());
		if (column == null)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Key is null"));
			return;
		}

		if (value.getValue() == null)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Value is null for parameter '"+column+"'"));
			return;
		}

		if ("".equals(value.getValue()))
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Empty value for parameter '"+column+"'"));
			return;
		}

		// value is java code

		if (value.getValue().toLowerCase().startsWith(TAG_INTERPRET_AS_JAVA))
		{
			String v = value.getValue().substring(TAG_INTERPRET_AS_JAVA.length()).trim();
			value.setValue(v);
			value.setJava(true);
		}

		if (value.getValue().contains(BEGIN_REFERENCE))
		{
			// expand complex value

			int index1 = value.getValue().indexOf(BEGIN_REFERENCE);
			while (index1 != -1)
			{
				index1 = expandReferenceValue(tc, action, column, value, index1, alertCollector, definedReferences, dictionaryManager);
			}
		}

		if (value.getValue().contains(BEGIN_STATIC))
		{
			// expand static value

			int index1 = value.getValue().indexOf(BEGIN_STATIC);
			while (index1 != -1)
			{
				index1 = expandStaticValue(tc, action, column, value, index1, alertCollector, dictionaryManager);
			}
		}

		if (value.getValue().contains(BEGIN_FUNCTION))
		{
			// expand function

			int index1 = value.getValue().indexOf(BEGIN_FUNCTION);
			while (index1 != -1)
			{
				index1 = expandUtilityFunction(action, column, value, index1, alertCollector, dictionaryManager, actionManager, utilityManager);
			}
		}
	}

	private static int expandReferenceValue(AMLTestCase tc, AMLAction action,
			String column, Value value, int index, AlertCollector alertCollector, Set<String> definedReferences, IDictionaryManager dictionaryManager)
	{

		int index2 = value.getValue().indexOf(END_REFERENCE, index);
		if (index2 == -1)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Unbalansed brackets in column '"+column+"'."));
			return -1;
		}

		// get reference to previous message and field name

		String var = value.getValue().substring(index+BEGIN_REFERENCE.length(), index2);

		if (var.length() == 0)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference is empty in column '"+column+"'."));
			return -1;
		}

		//var now is an expression that references other messages columns


		//		String[] arr = StringUtil.split(var, "");

		String[] arr = var.split("[:.]");
		if (arr.length == 0)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Invalid reference format in column '"+column+"': '"+var+"'. "
					+"Expected format: ${reference:column} or ${reference}."));
			return -1;
		}

		String lineRef = arr[0].trim(); // reference to message

		if (lineRef.length() == 0)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference to row is missed in column '"+column+"': '"+value.getOrigValue()+"'."));
			return -1;
		}

		// find action by reference
		AMLAction refAction = tc.findActionByRef(lineRef);
		if (refAction == null)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference to unknown action '"+lineRef
					+"' is found in column '"
					+column+"': '"+value.getOrigValue()+"'."));
			return -1;
		}

		// ${ref:field} linked to submessage should not be expanded immediately
		// because type of the submessage is not yet defined until it expanded
		if (!definedReferences.contains(lineRef))
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference '"+lineRef+"' is not yet defined in column '"
					+column+"': '"+value.getOrigValue()+"'."));
			return -1;
		}

        Class<?> messageType = refAction.getActionInfo().getReturnType();

        if(messageType == void.class)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Cannot refer to the void action"));
			return -1;
		}

		String[] columns = Arrays.copyOfRange(arr, 1, arr.length);

		verifyMessageColumns(
				action,
				refAction,
				column,
				value,
				alertCollector,
				messageType,
				columns,
				dictionaryManager);

		// replace reference

		String src = BEGIN_REFERENCE+var+END_REFERENCE;

		//String targ = "m"+lineRef+getter;
		String paramName = "v"+value.nextIndex();

		StringBuilder targ = new StringBuilder(paramName);
		for (String columnRef : columns) {
			targ.append(".").append(columnRef);
		}
		value.addParameter(new RefParameter(paramName, CodeGenerator_new.MAP_NAME + ".get(\"" + lineRef + "\")"));
		String v = value.getValue().replaceFirst(Pattern.quote(src), " " + targ.toString() + " ");
		value.setValue(v);
		value.setReference(true);

		// search for the next reference

		index = value.getValue().indexOf(BEGIN_REFERENCE);

		return index;
	}


	private static boolean verifyMessageColumns(
			AMLAction action,
			AMLAction refAction,
			String column,
			Value value,
			AlertCollector alertCollector,
			Class<?> messageType,
			String[] columns,
			IDictionaryManager dictionaryManager) {

		if (messageType == IMessage.class) {
			SailfishURI dictionaryURI = refAction.getDictionaryURI();
			IDictionaryStructure dict = null;
			try {
				dict = dictionaryManager.getDictionary(dictionaryURI);
			} catch (RuntimeException ex) {
				alertCollector.add(new Alert(action.getLine(), action.getUID(), ex.getMessage()));
				return false;
			}
			String msgType = refAction.getMessageTypeColumn();
			IFieldStructure messageStruct = dict.getMessageStructure(msgType);
			if (messageStruct != null) {
				boolean columnExists = true;
				IFieldStructure fldType = messageStruct;

				for (int i = 0; i < columns.length; i++) {
                    String columnRef = columns[i];
                    boolean itLastField = (i + 1 == columns.length);
					boolean collectionRequired = false;
					{
						int idx = columnRef.indexOf('[');
						if (idx != -1) {
							//FIXME: need gramma parser
							String dimensions = columnRef.substring(idx+1);
							if (dimensions.charAt(dimensions.length()-1) != ']') {
								alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "No trailing ']' in the column " +
										"reference " + columnRef +
										"column '"+column+"': '"+value.getOrigValue()+"'."
										));
							}
							dimensions = dimensions.substring(0, dimensions.length()-1);
							columnRef = columnRef.substring(0, idx);
							collectionRequired = true;

						}
					}

					IFieldStructure fld = null;
                    if (!fldType.isComplex()) {
                        alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Type " + fldType.getName() + " is not complex in the column " +
                                "reference " + columnRef +
                                "column '"+column+"': '"+value.getOrigValue()+"'."
                                ));
                    } else {
                        fld = fldType.getField(columnRef);
                    }
					if (fld == null) {
						columnExists = false;
						break;
					}

					if (collectionRequired && !fld.isCollection()) { // field1 is not collection: ref.field1[0], ref.field1[0].field2
						alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Field should be collection " + columnRef +
								" in column '"+column+"': '"+value.getOrigValue()+"'."
								));
					} else if (!collectionRequired && fld.isCollection() && !itLastField) { // field1 is collection: ref.field1.field2
						alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference to field that is not collection " + columnRef +
								" in column '"+column+"': '"+value.getOrigValue()+"'."
								));
					}
					fldType = fld;
				}
				if (columnExists) {
				    value.setCheck(false);
					return true;
				}
			}
		} else if(messageType == HashMap.class) {
            for(String columnRef : columns) {
                boolean columnExist = refAction.getParameters().containsKey(columnRef);

                if(!columnExist) {
                    alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference to unknown column '"
                            +columnRef+"' is found in column '"+column+"': '"+value.getOrigValue()+"'.", AlertType.WARNING));

                    //return false;
                }
            }

            value.setCheck(false);

            return true;
        }

		for (String columnRef : columns) {

			if (columnRef.length() == 0)
			{
				alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference to column is missed in column '"+column+"': '"+value.getOrigValue()+"'."));
				return false;
			}

			boolean columnExist = action.getHeaders().contains(columnRef);
			boolean getterExist = false;
			if (!columnExist) {
				Method[] methods = messageType.getMethods();
				for (Method method : methods) {
					if (method.getName().equalsIgnoreCase("get"+columnRef)) {
						getterExist = true;
						break;
					}
				}
			}

			// check field in referred message

			if (!getterExist && !columnExist)
			{
				alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference to unknown column '"
						+columnRef+"' is found in column '"+column+"': '"+value.getOrigValue()+"'."));
				return false;
			}

		}

		value.setCheck(false);
		return true;
	}

	private static int expandStaticValue(AMLTestCase tc, AMLAction action,
			String column, Value value, int index, AlertCollector alertCollector, IDictionaryManager dictionaryManager)
	{

		int index2 = value.getValue().indexOf(END_STATIC, index);

		if (index == -1)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Unbalansed brackets in column '"+column+"'."));
			return -1;
		}

		// get reference to previous message and field name

		String val = value.getValue().substring(index+2, index2);
		if (val.length() == 0)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference is empty in column '"+column+"'."));
			return -1;
		}

		String[] arr = val.split("[:.]");
        if (arr.length == 0)
		{
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Invalid reference format in column '"+column+"': '"+val+"'. "
                    +"Expected format: ${reference:column} or ${reference}."));
            return -1;
        }

        String lineRef = arr[0].trim(); // reference to message

        if (lineRef.length() == 0)
        {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference to row is missed in column '"+column+"': '"+value.getOrigValue()+"'."));
			return -1;
		}

		// find action by reference

        AMLAction refAction = tc.findActionByRef(lineRef);

		if (refAction == null)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Undefined reference ["+val+"] in column '"
					+column+"': '"+value.getOrigValue()+"'."));
			return -1;
		}

        if(refAction.getGenerateStatus() != AMLGenerateStatus.GENERATED) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Static reference to a not generated action: " + lineRef));
            return -1;
        }

        String[] columns = Arrays.copyOfRange(arr, 1, arr.length);

        if(JavaStatement.SET_STATIC.getURI().equals(refAction.getActionURI())) {
            if(arr.length > 1) {
    			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Invalid reference format to static variable in column '"+column+"': '"+val+"'. "
    					+"Expected format: %{reference}."));
    			return -1;
            }
		} else {
		    if(!refAction.isStaticAction()) {
		        alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Reference to a non-static action: " + lineRef));
		        return -1;
		    }

            Class<?> messageType = refAction.getActionInfo().getReturnType();

	        verifyMessageColumns(
	                action,
	                refAction,
	                column,
	                value,
	                alertCollector,
	                messageType,
	                columns,
	                dictionaryManager);
		}

		// replace reference

        String src = BEGIN_STATIC+val+END_STATIC;
		String paramName = "s"+value.nextIndex();
		StringBuilder targ = new StringBuilder(paramName);
        for (String columnRef : columns) {
            targ.append(".").append(columnRef);
        }
		value.addParameter(new RefParameter(paramName, CodeGenerator_new.STATIC_MAP_NAME+".get(\""+lineRef+"\")"));
		String v = value.getValue().replaceFirst(Pattern.quote(src), " " + targ + " ");
		value.setValue(v);
		value.setReference(true);

		// search for the next reference

		index = value.getValue().indexOf(BEGIN_STATIC);

		return index;
	}

	private static int expandUtilityFunction(AMLAction action,
			String column, Value value, int index, AlertCollector alertCollector, IDictionaryManager dictionaryManager, IActionManager actionManager, IUtilityManager utilityManager)
					throws SecurityException, SailfishURIException {
		String stringValue = value.getValue();
		int index2 = CodeGenerator_new.indexOfCloseBracket(stringValue, BEGIN_FUNCTION, END_FUNCTION, index);
		if (index2 == -1)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Unbalansed brackets in column '"+column+"'."));
			return -1;
		}

		// get name of the static method declared in action class
		// with @UtilFunction annotation

		String var = stringValue.substring(index+2, index2);

		if (var.length() == 0)
		{
			alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Utility function name is empty in column '"+column+"'."));
			return -1;
		}

		int openSpaceIndex = var.indexOf("(");

		if (openSpaceIndex == -1) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Syntaxis error in column '"+column+"': missed open bracket '('."));
            return -1;
        }

		int closeSpaceIndex = CodeGenerator_new.indexOfCloseBracket(var, "(", ")", openSpaceIndex);

        if (closeSpaceIndex == -1) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), column, "Syntaxis error in column '"+column+"': missed close bracket ')'."));
            return -1;
        }

		SailfishURI funcURI = SailfishURI.parse(var.substring(0, openSpaceIndex).trim());
        StringBuilder funcArgs = new StringBuilder(var.substring(openSpaceIndex + 1, closeSpaceIndex).trim());

        if(funcArgs.length() > 0) {
            funcArgs.insert(0, ", ");
        }
        funcArgs.append(')').append(var.substring(closeSpaceIndex + 1).trim());

		UtilityInfo utilityInfo = null;

		if(funcURI.isAbsolute()) {
		    utilityInfo = utilityManager.getUtilityInfo(funcURI);
		} else {
    		if(action.hasDictionaryURI()) {
    		    utilityInfo = dictionaryManager.getUtilityInfo(action.getDictionaryURI(), funcURI);
    		}

		    ActionInfo actionInfo = action.getActionInfo();

    		if(utilityInfo == null && actionInfo != null) {
    		    utilityInfo = actionManager.getUtilityInfo(actionInfo.getURI(), funcURI);
            }

		    if(utilityInfo == null) {
    		    utilityInfo = utilityManager.getUtilityInfo(funcURI);
			}
        }

		if(utilityInfo == null) {
            alertCollector.add(new Alert(action.getLine(), action.getUID(), action.getReference(), "Unable to resolve utility function: " + funcURI));
            return -1;
        }

		// replace function

        String src = BEGIN_FUNCTION+var+END_FUNCTION;
        //                      String targ = utilityClass.getSimpleName()+"."+var;
        String targ = String.format("%s(%s.parse(\"%s\")%s", CodeGenerator_new.UTILITY_MANAGER_CALL, SailfishURI.class.getSimpleName(), utilityInfo.getURI(), funcArgs);
        targ = targ.replace("\\", "\\\\").replace("$", "\\$");
        String v = stringValue.replaceFirst(Pattern.quote(src), " " + targ + " ");
        //                      v = "import " + clazz.getCanonicalName() + ";" + v;
        value.setValue(v);
        value.setReference(true);
        value.addParameter(new RefParameter(CodeGenerator_new.UTILITY_MANAGER_VARIABLE, CodeGenerator_new.UTILITY_MANAGER));

        // search next function

        index = value.getValue().indexOf(BEGIN_FUNCTION);

        return index;
	}
}
