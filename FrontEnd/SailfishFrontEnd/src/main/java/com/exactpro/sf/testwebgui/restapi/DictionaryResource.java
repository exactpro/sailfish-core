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
package com.exactpro.sf.testwebgui.restapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.utilitymanager.IUtilityManager;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityInfo;
import com.exactpro.sf.testwebgui.restapi.json.JsonError;
import com.exactpro.sf.testwebgui.restapi.json.dictionary.JsonDictionariesList;
import com.exactpro.sf.testwebgui.restapi.json.dictionary.JsonDictionary;
import com.exactpro.sf.testwebgui.restapi.json.dictionary.JsonEnumField;
import com.exactpro.sf.testwebgui.restapi.json.dictionary.JsonField;
import com.exactpro.sf.testwebgui.restapi.json.dictionary.JsonMessageStructure;
import com.exactpro.sf.testwebgui.restapi.json.dictionary.JsonRefField;
import com.exactpro.sf.testwebgui.restapi.json.dictionary.JsonSimpleField;
import com.exactpro.sf.testwebgui.restapi.json.dictionary.JsonUtilFunction;
import com.exactpro.sf.testwebgui.restapi.json.dictionary.JsonUtilFunction.Parameter;

@Path("dictionary")
public class DictionaryResource {

	private static final Logger logger = LoggerFactory.getLogger(DictionaryResource.class);

	private static class DictionaryTraverser {

		private final LinkedHashMap<String, JsonField> fields = new LinkedHashMap<>();

		public void traverse(IMessageStructure msgStructure) {
			this.traverse(msgStructure.getFields());
		}

		public void traverse(List<IFieldStructure> fields) {

			int i = 0;
			for (IFieldStructure curField : fields) {

				try {
					if (!curField.isComplex()) {
						visitSimpleType(curField, i++);
					} else {
						visitIMessage(curField, i++);
					}
				} catch (RuntimeException e) {
					StringBuilder builder;
					if (e.getMessage() != null) {
						builder = new StringBuilder(e.getMessage());
					} else {
						builder = new StringBuilder();
					}
					builder.append(". in field name = [").append(curField.getName()).append("]");
					throw new EPSCommonException(builder.toString(), e);
				}

			}
		}

		private void visitSimpleType(IFieldStructure fieldsStructure, int index) {
			JsonField field = null;

			if (fieldsStructure.isEnum()) {
				field = new JsonEnumField(
						fieldsStructure.getName(),
						fieldsStructure.getDescription(),
						null,
						fieldsStructure.getDefaultValue(),
						fieldsStructure.getJavaType(),
						fieldsStructure.isRequired(),
						fieldsStructure.isCollection(),
						index,
						fieldsStructure.getValues());
			}
			else {
				field = new JsonSimpleField(
						fieldsStructure.getName(),
						fieldsStructure.getDescription(),
						null,
						fieldsStructure.getDefaultValue(),
						fieldsStructure.getJavaType(),
						fieldsStructure.isRequired(),
						fieldsStructure.isCollection(),
						index);
			}
			fields.put(fieldsStructure.getName(), field);
		}


		private void visitIMessage(IFieldStructure fieldsStructure, int index) {
			if (fieldsStructure instanceof IMessageStructure) {
				JsonRefField msg = new JsonRefField(
						fieldsStructure.getName(),
						fieldsStructure.getDescription(),
						null,
						null,
						fieldsStructure.isRequired(),
						fieldsStructure.isCollection(),
						index,
						fieldsStructure.getReferenceName());

				fields.put(fieldsStructure.getName(), msg);
			} else {
				JsonRefField msg = new JsonRefField(
						fieldsStructure.getName(),
						fieldsStructure.getDescription(),
						null,
						fieldsStructure.getDefaultValue(),
						fieldsStructure.isRequired(),
						fieldsStructure.isCollection(),
						index,
						fieldsStructure.getReferenceName());

				fields.put(fieldsStructure.getName(), msg);
			}
		}

		public LinkedHashMap<String, JsonField> getFields() {
			return fields;
		}

	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDictionaries() {
		ISFContext context = SFLocalContext.getDefault();
		try {

			IDictionaryManager manager = context.getDictionaryManager();
			Set<SailfishURI> dURIs = manager.getDictionaryURIs();

			JsonDictionariesList response = new JsonDictionariesList();
			response.setDictionaries(dURIs.stream().map(SailfishURI::toString).collect(Collectors.toList()));
			return Response.status(Response.Status.OK).entity(response).build();
		} catch (Exception e) {
            logger.error(e.getMessage(), e);
            JsonError response = new JsonError("Can not retrive dictionaries list", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
	}

	@Path("{dictionary_name}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDictionary(
			@PathParam("dictionary_name") String dtitle,
			@QueryParam("deep") String deep,
			@QueryParam("utils") String withUtils) {
		ISFContext context = SFLocalContext.getDefault();
		try {
		    SailfishURI dictionaryURI = SailfishURI.parse(dtitle);
			IDictionaryManager dictManager = context.getDictionaryManager();
			IDictionaryStructure dictionary = dictManager.getDictionary(dictionaryURI);
            IUtilityManager utilityManager = context.getUtilityManager();

            JsonDictionary response = new JsonDictionary();
			response.setName(dtitle);
			response.setNamespace(dictionary.getNamespace());
			response.setDescription(dictionary.getDescription());

            Map<SailfishURI, Set<JsonUtilFunction>> utils = DictionaryResource.toJsonUtilFunctions(
                    dictManager.getSettings(dictionaryURI).getUtilityClassURIs(), utilityManager);

            response.setUtils(utils);

			for (IMessageStructure structure : dictionary.getMessageStructures()) {
				response.getMessages().put(structure.getName(), convert(structure, deep != null));
			}

			return Response.status(Response.Status.OK).entity(response).build();
		} catch (Exception e) {
            logger.error(e.getMessage(), e);
            JsonError response = new JsonError("Can not retrieve dictionaries", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
	}

	@Path("{dictionary_name}/message/{message_name}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMessage(
			@PathParam("dictionary_name") String dname,
			@PathParam("message_name") String message_name) {
		ISFContext context = SFLocalContext.getDefault();
		try {

			IDictionaryManager manager = context.getDictionaryManager();
			IDictionaryStructure dictionary = manager.getDictionary(SailfishURI.parse(dname));

			List<JsonMessageStructure> response = new ArrayList<>();

			IMessageStructure structure = dictionary.getMessageStructure(message_name);
			if (structure != null) {
				response.add(convert(structure, true));
			}
			return Response.status(Response.Status.OK).entity(response).build();
		} catch (Exception e) {
            logger.error(e.getMessage(), e);
            JsonError response = new JsonError("Can not retrive message", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
	}

    public static Map<SailfishURI, Set<JsonUtilFunction>> toJsonUtilFunctions(Set<SailfishURI> utilityClassURIs,
                                                                              IUtilityManager utilityManager) {
        Map<SailfishURI, Set<JsonUtilFunction>> utils = new HashMap<>();

        for (SailfishURI utilityClassURI : utilityClassURIs) {
            Set<UtilityInfo> infos = utilityManager.getUtilityInfos(utilityClassURI);

            Set<JsonUtilFunction> jsonUtils = infos.stream()
                                                   .map(DictionaryResource::toJsonUtilFunction)
                                                   .collect(Collectors.toSet());

            utils.put(utilityClassURI, jsonUtils);
        }

        return utils;
    }

    private static JsonUtilFunction toJsonUtilFunction(UtilityInfo uinfo) {
        return new JsonUtilFunction(
                uinfo.getURI().toString(),
                uinfo.getDescription(),
                convertParameters(uinfo.getParameterNames(), uinfo.getParameterTypes()),
                uinfo.getReturnType().getSimpleName());
    }

	private static Collection<Parameter> convertParameters(String[] names, Class<?>[] parameterTypes) {
		Collection<Parameter> result = new ArrayList<>();
		
		if (names == null || parameterTypes.length != names.length) {
			// fallback to types only:
			for (Class<?> paramType : parameterTypes) {
				result.add(new Parameter(null, paramType.getSimpleName()));
			}
		} else {
			for (int i=0; i< parameterTypes.length; i++) {
				Class<?> paramType = parameterTypes[i];
				String name = names[i];
				result.add(new Parameter(name, paramType.getSimpleName()));
			}
		}

		return result;
	}

	private JsonMessageStructure convert(IMessageStructure structure, boolean deep) {
		JsonMessageStructure msg = new JsonMessageStructure(
				structure.getName(),
				structure.getNamespace(),
				structure.getDescription(),
				null,
				null);
		if (deep) {
			DictionaryTraverser traverser = new DictionaryTraverser();
			traverser.traverse(structure);
			msg.getFields().putAll(traverser.getFields());
		}
		return msg;
	}

}
