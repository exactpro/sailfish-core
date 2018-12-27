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
package com.exactpro.sf.scriptrunner.impl.htmlreport;

import java.io.IOException;

import freemarker.cache.ClassTemplateLoader;
import freemarker.core.ParseException;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateNotFoundException;

public class TemplateWrapperFactory {
    private final Configuration configuration;

    public TemplateWrapperFactory(String templatesPackagePath) {
        configuration = new Configuration(Configuration.VERSION_2_3_24);

        configuration.setTemplateLoader(new ClassTemplateLoader(HtmlReport.class, templatesPackagePath));
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setAPIBuiltinEnabled(true);
        configuration.setObjectWrapper(new BeansWrapperBuilder(Configuration.VERSION_2_3_24).build());
        configuration.setRecognizeStandardFileExtensions(true);
    }

    public TemplateWrapper createWrapper(String templateName) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        return new TemplateWrapper(configuration.getTemplate(templateName));
    }
}
