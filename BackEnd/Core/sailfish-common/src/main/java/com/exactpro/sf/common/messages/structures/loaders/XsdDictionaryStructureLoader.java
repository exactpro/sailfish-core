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
package com.exactpro.sf.common.messages.structures.loaders;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;
import org.exolab.castor.types.AnyNode;
import org.exolab.castor.xml.schema.Annotation;
import org.exolab.castor.xml.schema.AppInfo;
import org.exolab.castor.xml.schema.ComplexType;
import org.exolab.castor.xml.schema.Documentation;
import org.exolab.castor.xml.schema.ElementDecl;
import org.exolab.castor.xml.schema.Facet;
import org.exolab.castor.xml.schema.Group;
import org.exolab.castor.xml.schema.ModelGroup;
import org.exolab.castor.xml.schema.Particle;
import org.exolab.castor.xml.schema.Schema;
import org.exolab.castor.xml.schema.SimpleType;
import org.exolab.castor.xml.schema.Structure;
import org.exolab.castor.xml.schema.Wildcard;
import org.exolab.castor.xml.schema.XMLType;
import org.exolab.castor.xml.schema.reader.SchemaReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.StructureBuilder;
import com.exactpro.sf.common.messages.structures.StructureUtils;
import com.exactpro.sf.common.messages.structures.impl.AttributeStructure;
import com.exactpro.sf.common.messages.structures.impl.DictionaryStructure;
import com.exactpro.sf.common.messages.structures.impl.FieldStructure;
import com.exactpro.sf.common.messages.structures.impl.MessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.Pair;

public class XsdDictionaryStructureLoader implements IDictionaryStructureLoader {

	private static final Logger logger = LoggerFactory.getLogger(XsdDictionaryStructureLoader.class);

	private enum States {
		MESSAGE, FIELD
	}

	private static class Context {
		Object value;
		States state;
		int deep;
	}

	private static final String PROTOCOL_ELEMENT = "protocol";

	private final Stack<Context> stack = new Stack<>();

	@Override
	public IDictionaryStructure load(InputStream input) throws EPSCommonException {

		try {
			IDictionaryStructure dict = doCreateDictionary(input);

			return dict;

		} catch (Exception e) {
			throw new EPSCommonException("Failed to load dictionary as schema file", e);
		}
	}

	@Override
	public String extractNamespace(InputStream input) throws EPSCommonException {
        Schema schema;
        try {
            schema = loadSchema(input);

            String namespace = schema.getTargetNamespace();

            if (namespace == null) {
                throw new EPSCommonException("TargetNamespace attribute is not specified in schema file");
            }
            return namespace;
        } catch (IOException e) {
            throw new EPSCommonException("Failed to load dictionary as schema file", e);
        }
	}

	@SuppressWarnings("unchecked")
	private IDictionaryStructure doCreateDictionary(InputStream stream) throws IOException {

		Schema schema = loadSchema(stream);

		schema.getAttributeGroups();
		schema.getAttributes();
		schema.getSimpleTypes();

		String namespace = schema.getTargetNamespace();

		if (namespace == null) {
			throw new EPSCommonException("TargetNamespace attribute is not specified in schema file");
		}

		StructureBuilder builder = new StructureBuilder(namespace);

		Collection<ElementDecl> elements = schema.getElementDecls();

		for(ElementDecl el : elements) {
			// Message declaration
			String msgName = el.getName();

			logger.debug("Message declaration: Name = [{}] Type = [{}]", msgName, el.getType().getName());

			Map<String, IAttributeStructure> protocolAttributes = getProtocolAttributes(el.getAnnotations());
			String description = getDescription(el.getAnnotations());

			StructureBuilder msgStructureBuilder = new StructureBuilder(msgName);

			Context msgStrContext = new Context();
			msgStrContext.deep = 1;
			msgStrContext.state = States.MESSAGE;
			msgStrContext.value = msgStructureBuilder;

			stack.push(msgStrContext);
			xmlType(builder, el.getType());
			stack.pop();

			IMessageStructure msgStructure = new MessageStructure(msgName, namespace,
						description, new ArrayList<>(msgStructureBuilder.getFieldStructures()), protocolAttributes);

			builder.addMessageStructure(msgStructure);
		}

		Collection<ComplexType> complexTypes = schema.getComplexTypes();

		for (ComplexType complexType : complexTypes) {
			Context fldStrContext = new Context();
			fldStrContext.deep = 0;
			fldStrContext.state = States.FIELD;
			Map<String, Object> fldAttribs = new HashMap<>();
			fldStrContext.value = fldAttribs;

			stack.push(fldStrContext);
			complexType(builder, complexType);
			stack.pop();
        }

        Map<String, IAttributeStructure> attributes = getProtocolAttributes(schema.getAnnotations());

        return new DictionaryStructure(namespace, null, attributes, builder.getMsgStructureMap(), builder.getFieldStructureMap());
	}

    /**
     * @param stream
     * @return
     * @throws IOException
     */
    private Schema loadSchema(InputStream stream) throws IOException {
        InputSource inputSource = new InputSource(new InputStreamReader(stream));

		SchemaReader schemaReader = new SchemaReader(inputSource);

		Schema schema = schemaReader.read();

        return schema;
    }

	@SuppressWarnings("unchecked")
	private void complexType(StructureBuilder builder, ComplexType type) {

		Context context = stack.peek();
		int curIntend = context.deep;
		States curState = context.state;

		String descStr = MessageFormat.format("ComplexType {0}", new Object[] { type.getName() == null
																 		? "" : " name=\"" + type.getName() + "\""
																 });

        if(logger.isDebugEnabled()) {
            logger.debug("{}{}", intend(curIntend), descStr);
        }

		if (type.isSimpleContent()) {
            // Simple content
			type.getBaseType();

			if ("restriction".equals(type.getDerivationMethod())) {
				// Restriction
                if(logger.isDebugEnabled()) {
                    logger.debug("{}{}", intend(curIntend), descStr);
                }

				// dumpComplexTypeAttribute(type);
			} else {
				// Extension
                if(logger.isDebugEnabled()) {
                    logger.debug("{}{}", intend(curIntend), descStr);
                }

				// dumpComplexTypeAttribute(type);
				throw new RuntimeException("simple content is not implemented");

			}
		} else {
			// Complex content
            type.getBaseType();

			if (type.getDerivationMethod() == null) {

				if (States.MESSAGE.equals(curState)) {

					for (int i = 0; i < type.getParticleCount(); ++i) {
						this.particle(builder, type.getParticle(i));
					}

				} else if (States.FIELD.equals(curState)) {
					// Should create complex field type
					Map<String, Object> fieldAttributes = (Map<String, Object>) context.value;

					fieldAttributes.put("IsComplexField", new Boolean(true));

					String subMessageName = type.getName();

					// Check if this type already exists in the dictionary
					if (builder.getMessageStructure(subMessageName) == null) {
						String description = getDescription(type.getAnnotations());

						Map<String, IAttributeStructure> typeProtocolAttributes = getProtocolAttributes(type.getAnnotations());

						StructureBuilder msgStructureBuilder = new StructureBuilder(subMessageName);

						Context subMsgStructContext = new Context();
						subMsgStructContext.deep = curIntend + 1;
						subMsgStructContext.state = States.MESSAGE;
						subMsgStructContext.value = msgStructureBuilder;

						stack.push(subMsgStructContext);

						for (int i = 0; i < type.getParticleCount(); ++i) {
							particle(builder, type.getParticle(i));
						}

						stack.pop();

						IMessageStructure msgStructure = new MessageStructure(subMessageName, builder.getNamespace(),
								description, new ArrayList<>(msgStructureBuilder.getFieldStructures()), typeProtocolAttributes);

						fieldAttributes.put("FieldType", msgStructure);

						builder.addMessageStructure(msgStructure);

					} else {
                        if(logger.isDebugEnabled()) {
                            logger.debug("{}{} already processed", intend(curIntend), subMessageName);
                        }
						fieldAttributes.put("FieldType", builder.getMessageStructure(subMessageName));
					}
				} else {
					throw new RuntimeException("Unknown state");
				}

			} else {
				// Extension
                if(logger.isDebugEnabled()) {
                    logger.debug("{}{}", intend(curIntend), descStr);
                }

				// Check if have redefine tag
				if (type.isRedefined()) {
					throw new RuntimeException("Redefine tag is not implemented");
				}

				throw new RuntimeException("Extension is not implemented");
			}
		}
	}

	private void elementDecl(StructureBuilder builder, ElementDecl decl) {

		Context context = stack.peek();
		int curIntend = context.deep;
		States curState = context.state;

		String declName = decl.getName();
		XMLType type = decl.getType();

		if (States.MESSAGE.equals(curState)) {
			// Field declaration
			Context fldStrContext = new Context();
			fldStrContext.deep = curIntend + 1;
			fldStrContext.state = States.FIELD;
			Map<String, Object> fldAttribs = new HashMap<>();
			fldStrContext.value = fldAttribs;

			stack.push(fldStrContext);
			xmlType(builder, type);
			stack.pop();

			IFieldStructure fieldStructureForType = (IFieldStructure) fldAttribs.get("FieldType");
			boolean isComplexType = fieldStructureForType.isComplex();

			Boolean isRequired = decl.getMinOccurs() > 0;

			Boolean isCollection = decl.getMaxOccurs() != 1;

			Boolean isServiceName = isComplexType ? false : fieldStructureForType.isServiceName();

			StructureBuilder msgStruct = (StructureBuilder) context.value;

			String description = getDescription(decl.getAnnotations());

			Map<String, IAttributeStructure> declProtocolAttributes = getProtocolAttributes(decl.getAnnotations());

			if (!isComplexType) {
				// Simple field
				String defValue = (decl.getDefaultValue() == null) ? null : decl.getDefaultValue();

				IFieldStructure fldStruct = new FieldStructure(declName, builder.getNamespace(), description, null,
						declProtocolAttributes, null, fieldStructureForType.getJavaType() == null
								? getFieldType(fieldStructureForType.getName())
								: fieldStructureForType.getJavaType(),
						isRequired, isCollection, isServiceName, defValue);

				msgStruct.addFieldStructure(fldStruct);

			} else {

				IMessageStructure fldStruct = new MessageStructure(declName, builder.getNamespace(),
						description, isRequired, isCollection, declProtocolAttributes, (IMessageStructure)fieldStructureForType);

				msgStruct.addFieldStructure(fldStruct);
			}

		} else {
			throw new EPSCommonException("Illegal state = " + curState);
		}
	}

	private void modelGroup(StructureBuilder builder, ModelGroup group) {

		final int len = group.getParticleCount();

		for (int i = 0; i < len; i++) {
			particle(builder, group.getParticle(i));
		}
	}

	private void group(StructureBuilder builder, Group group) {

		final int len = group.getParticleCount();

		for (int i = 0; i < len; i++) {
			particle(builder, group.getParticle(i));
		}
	}

	private void particle(StructureBuilder builder, Particle particle) {

		if (particle.getStructureType() == Structure.ELEMENT) {
			this.elementDecl(builder, (ElementDecl) particle);
		} else if (particle.getStructureType() == Structure.MODELGROUP) {
			this.modelGroup(builder, (ModelGroup) particle);
		} else if (particle.getStructureType() == Structure.GROUP) {
			this.group(builder, (Group) particle);
		} else if (particle.getStructureType() == Structure.MODELGROUP_REF) {
			this.modelGroup(builder, (ModelGroup) particle);
		} else if (particle.getStructureType() == Structure.WILDCARD) {
			this.wildcard((Wildcard) particle);
		} else {
			throw new RuntimeException("not implemented. Structure type = " + particle.getStructureType());
		}
	}

	@SuppressWarnings("unchecked")
	private void simpleType(StructureBuilder builder, SimpleType simpleType) {

		Context context = stack.peek();
		int curIntend = context.deep;
		States curState = context.state;

		String descStr = MessageFormat.format("State {0} SimpleType {1}", curState,
				simpleType.getName() == null ? "" : " name=\"" + simpleType.getName() + "\"");

        if(logger.isDebugEnabled()) {
            logger.debug("{}{}", intend(curIntend), descStr);
        }

		if (simpleType.isBuiltInType()) {

			if (States.FIELD.equals(curState)) {

				Map<String, Object> fieldAttribs = (Map<String, Object>) context.value;

				String description = getDescription(simpleType.getAnnotations());
				Map<String, IAttributeStructure> protocolAttributes = getProtocolAttributes(simpleType.getAnnotations());

				IFieldStructure fieldStructureForType = new FieldStructure(simpleType.getName(),
						builder.getNamespace(), description, null, protocolAttributes, null, null, false, false, false, null);

				fieldAttribs.put("FieldType", fieldStructureForType);

			} else {
				throw new RuntimeException("is not implemented");
			}

		} else if (simpleType.getDerivationMethod().equals("restriction")) {

			if (States.FIELD.equals(curState)) {

				Map<String, Object> fieldAttribs = (Map<String, Object>) context.value;

				String fieldStructureName = simpleType.getName();

				// check if this type already exists in the dictionary
				if (builder.getFieldStructure(fieldStructureName) == null) {
					String description = getDescription(simpleType.getAnnotations());
					Map<String, IAttributeStructure> protocolAttributes = getProtocolAttributes(simpleType.getAnnotations());

					IFieldStructure fieldStructure = null;

					List<Pair<String, IAttributeStructure>> enumElementsList = null;
					if (simpleType.isBuiltInType())
						fieldStructure = builder.getFieldStructure(simpleType.getName());
					else if (simpleType.getBaseType().isSimpleType()
							&& ((SimpleType) simpleType.getBaseType()).isBuiltInType()) {
						fieldStructure = builder.getFieldStructure(simpleType.getBaseType().getName());

						Enumeration<Facet> facets = simpleType.getFacets();

						if (facets != null) {
							List<Pair<String, IAttributeStructure>> enumElements = new LinkedList<>();
							while (facets.hasMoreElements()) {
								Facet facet = facets.nextElement();
                                if(logger.isDebugEnabled()) {
                                    logger.debug("{}Facet name:{}; Facet value:{}",
                                            intend(curIntend), facet.getName(), facet.getValue());
                                }

								if (facet.getName().equals("enumeration")) {
									Map<String, IAttributeStructure> enumElementProtocolAttributes = getProtocolAttributes(
											facet.getAnnotations());

									String alias = null;

									if (enumElementProtocolAttributes != null)
										alias = enumElementProtocolAttributes.get("Alias").getValue();

									Pair<String, IAttributeStructure> enumElement = null;

									IAttributeStructure attr = enumElementProtocolAttributes.values().iterator().next();

									JavaType javaType = null;

									if (fieldStructure != null) {
										javaType = fieldStructure.getJavaType();
									} else {
										javaType = getFieldType(simpleType.getBaseType().getName());
									}

									enumElement = new Pair<>(alias,
											new AttributeStructure(attr.getValue(), facet.getValue(),
													StructureUtils.castValueToJavaType(facet.getValue(), javaType), javaType));

									enumElements.add(enumElement);
								}
							}

							if (!enumElements.isEmpty()) {
								if (enumElementsList == null) enumElementsList = new ArrayList<>();
								enumElementsList.addAll(enumElements);
							}
						}
					} else {
						// nesting can be unlimited. However current model only supports 1 level
						throw new RuntimeException("not implemented");
					}

					IFieldStructure fieldStruct = null;

					if (enumElementsList == null) {

						fieldStruct = new FieldStructure(simpleType.getName(), builder.getNamespace(), description, null,
								protocolAttributes, null, getFieldType(simpleType.getBaseTypeName()), false, false, false, null);

					} else {

						fieldStruct = new FieldStructure(simpleType.getName(), builder.getNamespace(),
								description, fieldStructure != null ? fieldStructure.getName() : null,
								protocolAttributes, valuesMapFromEnumList(enumElementsList),
								getFieldType(simpleType.getBaseTypeName()), false, false, false, null);
					}

					fieldAttribs.put("FieldType", fieldStruct);

					builder.addFieldStructure(fieldStruct);

				} else {
                    if(logger.isDebugEnabled()) {
                        logger.debug("{}{} already processed", intend(curIntend), fieldStructureName);
                    }
					fieldAttribs.put("FieldType", builder.getFieldStructure(fieldStructureName));
				}
			} else {
				throw new RuntimeException("is not implemented");
			}
		}
	}

	private Map<String, IAttributeStructure> valuesMapFromEnumList(List<Pair<String, IAttributeStructure>> list) {

		Map<String, IAttributeStructure> values = new HashMap<>();

		for (Pair<String, IAttributeStructure> elem : list) {
			values.put(elem.getFirst(), elem.getSecond());
		}

		return values;
	}

	private void wildcard(Wildcard wc) {
		throw new RuntimeException("XSWildcard is not implemented");
	}

	private void xmlType(StructureBuilder builder, XMLType type) {

		if (type.isComplexType()) {

			complexType(builder, (ComplexType) type);

		} else if (type.isSimpleType()) {

			simpleType(builder, (SimpleType) type);

		} else {
			throw new RuntimeException("Not implemented");
		}
	}

	private static String intend(int n) {
        return StringUtils.repeat('\t', n);
	}

	@SuppressWarnings("unchecked")
	private String getDescription(Enumeration<Annotation> annotations) {

		StringBuilder content = new StringBuilder();

		while (annotations.hasMoreElements()) {

			Annotation annotation = annotations.nextElement();
			Enumeration<Documentation> documentations = annotation.getDocumentation();

			while (documentations.hasMoreElements()) {

				Documentation doc = documentations.nextElement();
				Enumeration<Object> annItems = doc.getObjects();

				while (annItems.hasMoreElements()) {

					AnyNode annItem = (AnyNode)annItems.nextElement();
					content.append(annItem.getStringValue().trim());
				}
			}
		}

		return content.toString().isEmpty() ? null : content.toString();
	}

	@SuppressWarnings("unchecked")
	private Map<String, IAttributeStructure> getProtocolAttributes(Enumeration<Annotation> annotations) {

		Map<String, IAttributeStructure> attributes = new HashMap<>();

		while (annotations.hasMoreElements()) {

			Annotation annotation = annotations.nextElement();

			Enumeration<AppInfo> appInfos = annotation.getAppInfo();

			while (appInfos.hasMoreElements()) {

				AppInfo appInfo = appInfos.nextElement();

				Enumeration<Object> annItems = appInfo.getObjects();

				while (annItems.hasMoreElements()) {

					AnyNode annItem = (AnyNode)annItems.nextElement();

					if (annItem.getNodeType() == AnyNode.ELEMENT && annItem.getLocalName().equals(PROTOCOL_ELEMENT)) {

						AnyNode attrib = annItem.getFirstAttribute();

						String name = null;
						String type = null;
						String value = null;

						while (attrib != null) {

							String attrName = attrib.getLocalName();
							String attrValue = attrib.getStringValue();

							if (attrName.equals("name")) {

								name = attrValue;

							} else if (attrName.equals("type")) {

								type = attrValue;

							} else if (attrName.equals("default")) {

								value = attrValue;

							} else {

								throw new EPSCommonException("Unknown attribute = [" + attrName + "]");
							}

							attrib = attrib.getNextSibling();
						}

						if (name == null)
							throw new EPSCommonException("Attribute \"name\" must be specified within protocol element");

						if (type == null) {
							throw new EPSCommonException("Attribute \"type\" must be specified within protocol element");
						}

						if (type.indexOf(':') != -1) {

							String prefix = type.substring(0, type.indexOf(':'));

							if (!prefix.equals("xsd")) {
								throw new IllegalArgumentException("Namespace prefix should be \"xsd\"");
							}

							type = type.substring(type.indexOf(':') + 1);

						} else {
							throw new IllegalArgumentException("Namespace prefix should be\"xsd\"");
						}

						JavaType javaType = getFieldType(type);

						attributes.put(name,
								new AttributeStructure(name, value, StructureUtils.castValueToJavaType(value, javaType), javaType));
					}
				}
			}
		}

		return attributes.isEmpty() ? null : attributes;
	}

	private JavaType getFieldType(String typeName) {
		if (typeName.equals("string")) {
			return JavaType.JAVA_LANG_STRING;
		} else if (typeName.equals("int")) {
			return JavaType.JAVA_LANG_INTEGER;
		} else if (typeName.equals("byte")) {
			return JavaType.JAVA_LANG_BYTE;
		} else if (typeName.equals("short")) {
			return JavaType.JAVA_LANG_SHORT;
		} else if (typeName.equals("long")) {
			return JavaType.JAVA_LANG_LONG;
		} else if (typeName.equals("boolean")) {
			return JavaType.JAVA_LANG_BOOLEAN;
		} else if (typeName.equals("float")) {
			return JavaType.JAVA_LANG_FLOAT;
		} else if (typeName.equals("double")) {
			return JavaType.JAVA_LANG_DOUBLE;
        } else if (typeName.equals("date") || typeName.equals("localDateTime")) {
			return JavaType.JAVA_TIME_LOCAL_DATE_TIME;
        } else if (typeName.equals("localDate")) {
            return JavaType.JAVA_TIME_LOCAL_DATE;
        } else if (typeName.equals("localTime")) {
            return JavaType.JAVA_TIME_LOCAL_TIME;
		} else if (typeName.equals("decimal")) {
			return JavaType.JAVA_MATH_BIG_DECIMAL;
		} else {
			return null;
		}
	}
}
