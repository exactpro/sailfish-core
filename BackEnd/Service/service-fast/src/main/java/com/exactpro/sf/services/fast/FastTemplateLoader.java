/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.fast;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDataManager;
import org.openfast.template.TemplateRegistry;
import org.openfast.template.loader.XMLMessageTemplateLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class FastTemplateLoader {
    private static final Logger logger = LoggerFactory.getLogger(FastTemplateLoader.class);

    public TemplateRegistry loadFastTemplates(IDataManager dataManager, String pluginAlias, String templateName) {
        XMLMessageTemplateLoader loader = new XMLMessageTemplateLoader();
        loader.setLoadTemplateIdFromAuxId(true);
        try (InputStream templateStream = dataManager.getDataInputStream(pluginAlias, FASTMessageHelper.getTemplatePath(templateName))) {
            loader.load(templateStream);
        } catch (IOException e) {
            logger.warn("Can not read template {} from resources", templateName, e);
            throw new EPSCommonException("Can not read template " + templateName + " from resources", e);
        }
        return loader.getTemplateRegistry();
    }
}
