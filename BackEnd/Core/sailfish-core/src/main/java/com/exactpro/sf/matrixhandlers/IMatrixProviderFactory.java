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
package com.exactpro.sf.matrixhandlers;

import java.util.Collection;

import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;

public interface IMatrixProviderFactory {

	// Note all implementations should have zero-argument constructor


	void init(IWorkspaceDispatcher wd);

    // Resolve link to multiple matrices (with wildcards)
    default Collection<String> resolveLinks(String link) {
        throw new UnsupportedOperationException("this method is not implemented");
    }

    default IMatrixProvider getMatrixProvider(String link) {
        throw new UnsupportedOperationException("this method is not implemented");
    }

    @Deprecated
    default Collection<IMatrixProvider> getMatrixProviders(String link) {
        throw new UnsupportedOperationException("this method is no longer supported");
    }

	String getHumanReadableName();

	// Alias of matrix provider
	String getAlias();

	String getNotes();

}
