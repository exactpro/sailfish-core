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
package com.exactpro.sf.testwebgui.servlets;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;

import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.dictionaries.DictionaryEditorModel;

public class SessionModelsMapper {

    private Map<HttpSession, DictionaryEditorModel> dictionaryEditorModels = new ConcurrentHashMap<>();

    public DictionaryEditorModel getDictionaryEditorModel(HttpSession session) {

        if (session == null) {
            return null;
        }

        if (!this.dictionaryEditorModels.containsKey(session)) {
            this.dictionaryEditorModels.put(session, new DictionaryEditorModel(BeanUtil.getSfContext()));
        }

        return this.dictionaryEditorModels.get(session);
    }

    public void destroyModel(DictionaryEditorModel model) {

        HttpSession found = null;

        for (HttpSession session : this.dictionaryEditorModels.keySet()) {
            if (this.dictionaryEditorModels.get(session) == model) {
                found = session;
                break;
            }
        }

        if (found != null) {
            this.dictionaryEditorModels.remove(found);
        }
    }
}
