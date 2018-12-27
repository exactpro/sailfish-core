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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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

import com.exactpro.sf.aml.MessageDirection;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;
import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.BaseClass;
import com.exactpro.sf.scriptrunner.actionmanager.ActionInfo;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;
import com.exactpro.sf.scriptrunner.utilitymanager.IUtilityManager;
import com.exactpro.sf.testwebgui.restapi.json.JsonError;
import com.exactpro.sf.testwebgui.restapi.json.action.JsonAction;
import com.exactpro.sf.testwebgui.restapi.json.action.JsonActions;
import com.exactpro.sf.testwebgui.restapi.json.dictionary.JsonUtilFunction;

@Path("actions")
public class ActionsResource {

	private static final Logger logger = LoggerFactory.getLogger(ActionsResource.class);

	@Path("{aml_version}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
    public Response getActions(
			@PathParam("aml_version") int amlVer,
			@QueryParam("utils") String withUtils) {

		try {
            ISFContext context = SFLocalContext.getDefault();
            IActionManager actionManager = context.getActionManager();
            IUtilityManager utilityManager = context.getUtilityManager();

            SailfishURI language = SailfishURI.parse("AML_v" + amlVer);

			if(language == null) {
			    throw new EPSCommonException("Invalid aml_version: " + amlVer);
			}

            SailfishURI aml2Lang = SailfishURI.unsafeParse("AML_v2");
            SailfishURI aml3Lang = SailfishURI.unsafeParse("AML_v3");

            Map<SailfishURI, Set<JsonUtilFunction>> utils = new HashMap<>();
            Map<SailfishURI, JsonAction> actions = new LinkedHashMap<>();

            Set<SailfishURI> actionClassURIs = actionManager.getActionClassURIs();

            for (SailfishURI actionClassURI: actionClassURIs) {

                Set<SailfishURI> utilityClassURIs = actionManager.getUtilityURIs(actionClassURI);

                utils.putAll(DictionaryResource.toJsonUtilFunctions(utilityClassURIs, utilityManager));

                Set<ActionInfo> actionInfos = actionManager.getActionInfosByClass(actionClassURI);

                Set<String> utilityClassNames = utilityClassURIs.stream()
                                                                .map(SailfishURI::toString)
                                                                .collect(Collectors.toSet());
                for (ActionInfo actionInfo : actionInfos) {
                    if (!actionInfo.isLanguageCompatible(language, false)) {
                        continue;
                    }

                    MessageDirection direction = actionInfo.getAnnotation(MessageDirection.class);

                    List<String> requiredColumns = new LinkedList<>(actionInfo.getRequirements().getRequiredColumns());

                    HashSet<String> allColumns = new HashSet<>();
                    allColumns.addAll(actionInfo.getCommonColumns());
                    allColumns.addAll(actionInfo.getCustomColumns());
                    allColumns.removeAll(requiredColumns);
                    List<String> optionalColumns = new LinkedList<>(allColumns);

                    actions.put(actionInfo.getURI(),
                                new JsonAction(actionInfo.getURI().toString(), requiredColumns, optionalColumns,
                                               direction != null ? direction.direction() : null, utilityClassNames,
                                               actionInfo.isLanguageCompatible(aml2Lang, false),
                                               actionInfo.isLanguageCompatible(aml3Lang, false),
                                               actionInfo.getDescription()));
                }

            }

			// JavaStatement
			Collection<JsonAction> statements = new LinkedList<>();
			statements.add(new JsonAction(
					JavaStatement.BEGIN_LOOP.getValue(),
					Arrays.asList(Column.MessageCount.getName()))
			);
			statements.add(new JsonAction(
					JavaStatement.END_LOOP.getValue())
			);
			statements.add(new JsonAction(
					JavaStatement.SET_STATIC.getValue(),
					Arrays.asList(Column.Reference.getName(), Column.StaticType.getName()),
					Arrays.asList(Column.StaticValue.getName()))
			);
			statements.add(new JsonAction(
					JavaStatement.DEFINE_SERVICE_NAME.getValue(),
					Arrays.asList(Column.Reference.getName(), Column.ServiceName.getName()))
			);
			statements.add(new JsonAction(
					JavaStatement.DEFINE_HEADER.getValue())
			);
			statements.add(new JsonAction(
					JavaStatement.BEGIN_IF.getValue(),
					Arrays.asList(Column.Condition.getName()))
			);
			statements.add(new JsonAction(
					JavaStatement.BEGIN_ELIF.getValue(),
					Arrays.asList(Column.Condition.getName()))
			);
			statements.add(new JsonAction(
					JavaStatement.BEGIN_ELSE.getValue())
			);
			statements.add(new JsonAction(
					JavaStatement.END_IF.getValue())
			);

			return Response.status(Response.Status.OK).entity(new JsonActions(actions, statements, utils) ).build();
		} catch (Exception e) {
            logger.error(e.getMessage(), e);
            JsonError response = new JsonError("Can not retrive actions list", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
	}

    private String extractClassName(BaseClass cls) {
        String fullName = cls.getClassName();
        int idx = fullName.lastIndexOf(".");
        if (idx >= 0) {
            return fullName.substring(idx);
        }
        return fullName;
    }

}
