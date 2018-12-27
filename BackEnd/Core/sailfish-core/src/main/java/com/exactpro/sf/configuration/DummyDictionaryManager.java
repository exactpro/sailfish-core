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
package com.exactpro.sf.configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exactpro.sf.aml.DictionarySettings;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.workspace.WorkspaceLayerException;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityInfo;

public class DummyDictionaryManager implements IDictionaryManager {
    private final Map<String, IDictionaryStructure> uriToDictionary;

    public DummyDictionaryManager(Map<String, IDictionaryStructure> suriToDictionary) {
        this.uriToDictionary = suriToDictionary;
    }

    public DummyDictionaryManager(IDictionaryStructure dictionaryStructure, SailfishURI dictionaryURI) {
        this.uriToDictionary = new HashMap<>();
        this.uriToDictionary.put(dictionaryURI.getResourceName(), dictionaryStructure);
    }
    
    @Override
    public IDictionaryStructure getDictionary(SailfishURI uri) throws RuntimeException {
        IDictionaryStructure dictionary;

        if ((dictionary = uriToDictionary.get(uri.getResourceName())) == null) {
            throw new IllegalArgumentException(String.format("There is no dictionary with [%s] name", uri.getResourceName()));
        }

        return dictionary;
    }

    @Override
    public Set<SailfishURI> getDictionaryURIs(String pluginAlias) {
        return null;
    }

    @Override
    public Set<SailfishURI> getDictionaryURIs() {
        return null;
    }

    @Override
    public List<SailfishURI> getCachedDictURIs() {
        return null;
    }

    @Override
    public DictionarySettings getSettings(SailfishURI uri) {
        return null;
    }

    @Override
    public IMessageFactory getMessageFactory(SailfishURI uri) {
        return DefaultMessageFactory.getFactory();
    }

    @Override
    public void invalidateDictionaries(SailfishURI... uris) {

    }

    @Override
    public IDictionaryRegistrator registerDictionary(String title, boolean overwrite) throws WorkspaceLayerException, IOException {
        return null;
    }

    @Override
    public IDictionaryStructure createMessageDictionary(String pathName) {
        return null;
    }

    @Override
    public IMessageFactory createMessageFactory() {
        return null;
    }

    @Override
    public Map<SailfishURI, String> getDictionaryLocations() {
        return null;
    }

    @Override
    public UtilityInfo getUtilityInfo(SailfishURI dictionaryURI, SailfishURI utilityURI, Class<?>... argTypes) throws SailfishURIException {
        return null;
    }

    @Override
    public Set<SailfishURI> getUtilityURIs(SailfishURI dictionaryURI) {
        return null;
    }

    @Override
    public long getDictionaryId(SailfishURI uri) {
        return 0;
    }

    @Override
    public void subscribeForEvents(IDictionaryManagerListener listener) {

    }

    @Override
    public void unSubscribeForEvents(IDictionaryManagerListener listener) {

    }
}
