/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.actions;

import com.exactpro.sf.aml.CustomColumn;
import com.exactpro.sf.aml.CustomColumns;
import com.exactpro.sf.aml.Description;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.actionmanager.ActionMethod;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;

import static com.exactpro.sf.actions.ActionUtil.unwrapFilters;

@MatrixActions
@ResourceAliases("XMLActions")
public class XMLActions extends AbstractCaller {
    public static final String TEMPLATE = "Template";
    public static final String OUT = "Content";

    @Description("The generated XML will available by this reference in field `Content`<br>" +
            "Example:<br/>" +
            "#{ref.Content}")
    @CustomColumns(
            @CustomColumn(value = TEMPLATE, required = true)
    )
    @ActionMethod
    public HashMap<?, ?> generateDocument(IActionContext actionContext, HashMap<?, ?> inputData) {
        inputData = unwrapFilters(inputData);
        String templateAlias = Objects.requireNonNull((String) inputData.remove(TEMPLATE), "'template' parameter");

        IDataManager dataManager = actionContext.getDataManager();
        try (InputStream dataInputStream = dataManager.getDataInputStream(SailfishURI.parse(templateAlias))) {
            StringTemplateLoader stringLoader = new StringTemplateLoader();
            stringLoader.putTemplate(TEMPLATE, IOUtils.toString(dataInputStream, StandardCharsets.UTF_8));

            Configuration cfg = new Configuration(Configuration.VERSION_2_3_27);
            cfg.setTemplateLoader(stringLoader);

            Template temp = cfg.getTemplate(TEMPLATE);
            try (Writer out = new StringWriter()) {

                temp.process(inputData, out);

                HashMap<String, String> result = new HashMap<>();
                result.put(OUT, out.toString());
                return result;
            }
        } catch (Exception e) {
            throw new EPSCommonException(e);
        }
    }
}
