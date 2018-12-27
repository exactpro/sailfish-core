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
package com.exactpro.sf.aml;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.Value;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.actionmanager.ActionInfo;

public interface IAction extends Cloneable {

	void setReference(String ref);
	String getReference();
	boolean hasReference();

	void setReferenceToFilter(String ref);
	String getReferenceToFilter();
	boolean hasReferenceToFilter();

	void setId(String id);
	String getId();
	boolean hasId();

	void setServiceName(String serviceName);
	String getServiceName();
	boolean hasServiceName();

	void setCheckPoint(String checkPoint);
	String getCheckPoint();

	void setDoublePrecision(String precision);
	String getDoublePrecision();

	void setExecute(boolean b);
	boolean isExecute();

	SailfishURI getActionURI();
	void setActionURI(SailfishURI actionURI);
	boolean hasActionURI();

	void setTimeout(Value s);
	Value getTimeout();

	String getOutcome();
	void setOutcome(String cell);

	String getSystemPrecision();
	void setSystemPrecision(String precision);

	String getStaticType();
	void setStaticType(String staticType);

	Value getStaticValue();
	void setStaticValue(Value value);

	boolean hasTemplate();
	String getTemplate();
	void setTemplate(String template);

	Map<String, Value> getParameters();

	String getMessageTypeColumn();
	void setMessageTypeColumn(String mesageTypeColumn);

	Map<Column, ? extends IField> getDefinedServiceFields();
	void addDefinedServiceField(Column key, IField value);
	IField getDefinedServiceField(Column key);

	void addDefinedColumn(String columnName);
	Set<String> getDefinedColumns();

	String getDescrption();
	void setDescrption(String description);

	SailfishURI getDictionaryURI();
	void setDictionaryURI(SailfishURI dictionaryURI);
	boolean hasDictionaryURI();

	void setMessageCount(String messageCount);
	String getMessageCount();

	boolean getContinueOnFailed();
	void setContinueOnFailed(boolean b);

	boolean getAutoStart();
	void setAutoStart(boolean b);

	boolean getReorderGroups();
	void setReorderGroups(boolean b);

	List<String> getHeaders();
	void setHeaders(List<String> headers);

	void addMessageReference(String lineRef, MessageReference ref);
	Map<String, MessageReference> getMessageReferences();
	void setMessageReferences(Map<String, MessageReference> messageReferences);

	void put(String key, Value value);
	Value get(String key);

	ActionInfo getActionInfo();
	void setActionInfo(ActionInfo actionInfo);

	IAction clone();

}
