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
package com.exactpro.sf.help;

import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;


public class HelpTemplateWrapper {
    private final Template template;
    private final Map<String, Object> data;

    public HelpTemplateWrapper(Template template) {
        this.template = template;
        this.data = new HashMap<>();
    }

    public void setData(String name, Object value) {
        data.put(name, value);
    }

    public void write(Writer writer) throws TemplateException, IOException {
        template.process(data, writer);
    }
}
