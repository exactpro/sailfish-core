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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exactpro.sf.aml.DictionarySettings;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.workspace.WorkspaceLayerException;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityInfo;

public interface IDictionaryManager {

	/**
	 * Load dictionary from both xml and xsd file
	 * @param uri URI of dictionary (must contain at least resource name)
	 * @return dictionary
	 */
	IDictionaryStructure getDictionary(SailfishURI uri) throws RuntimeException;

    Set<SailfishURI> getDictionaryURIs(String pluginAlias);

    Set<SailfishURI> getDictionaryURIs();

    List<SailfishURI> getCachedDictURIs();

    /**
     * Retrieves dictionary settings by specified URI
     *
     * @param uri dictionary settings URI (must contain at least resource name)
     *
     * @return dictionary settings or {@code null}
     */
	DictionarySettings getSettings(SailfishURI uri);

	/**
	 * Retrieves message factory by specified URI
	 *
	 * @param uri message factory URI (must contain at least resource name)
	 *
	 * @return message factory or {@code null}
	 */
	IMessageFactory getMessageFactory(SailfishURI uri);

	/**
	 * Invalidates dictionaries with specified URIs.
	 * URIs will be used as is (without any matching)
	 *
	 * @param uris dictionary URIs
	 */
	void invalidateDictionaries(SailfishURI... uris);

	/**
	 * Register new or overwrite exist dictionary by title
	 *
	 * @param title     dictionary title
	 * @param overwrite resource
	 */
	IDictionaryRegistrator registerDictionary(String title, boolean overwrite) throws WorkspaceLayerException, IOException;

	/**
 	 * create dictionary from file
	 * @param pathName - relative to {ROOT} folder path
	 * @return
	 */
	// FIXME: is it ok to be public?
	IDictionaryStructure createMessageDictionary(String pathName);

	// FIXME: Is it ok to be here?
	IMessageFactory createMessageFactory();

	/**
	 * URI -> filename
	 */
	Map<SailfishURI, String> getDictionaryLocations();

	/**
     * Retrieves utility info by specified utility URI and argument types.
     * Search is scoped by utility classes assigned to a dictionary specified by dictionary URI.
     * If utility URI is absolute then search isn't scoped.
     *
     * @param dictionaryURI dictionary URI (must contain at least class alias)
     * @param utilityURI    utility URI (must contain at least resource name)
     * @param argTypes      utility argument types
     *
     * @return utility info or {@code null}
     */
	UtilityInfo getUtilityInfo(SailfishURI dictionaryURI, SailfishURI utilityURI, Class<?>... argTypes) throws SailfishURIException;

    /**
     * @param dictionaryURI
     * @return all UtilityFuntion's URIs for the given dictionary SURI
     */
    Set<SailfishURI> getUtilityURIs(SailfishURI dictionaryURI);

	/**
	 * Retrieves dictionary id by specified URI
	 *
	 * @param uri dictionary URI (must contain at least resource name)
	 */
	long getDictionaryId(SailfishURI uri);

	void subscribeForEvents(IDictionaryManagerListener listener);

	void unSubscribeForEvents(IDictionaryManagerListener listener);
}