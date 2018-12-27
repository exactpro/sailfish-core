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
package com.exactpro.sf.common.adapting;

/**
 * An adapter factory defines behavioral extensions for
 * one or more classes that implements the <code>IAdaptable</code>
 * interface. Adapter factories are registered with an
 * adapter manager.
 * <p>
 * This interface can be used without OSGi running.
 * </p><p>
 * Clients may implement this interface.
 * </p>
 * @see IAdapterManager
 * @see IAdaptable
 */
public interface IAdapterFactory {
	/**
	 * Returns an object which is an instance of the given class
	 * associated with the given object. Returns <code>null</code> if
	 * no such object can be found.
	 *
	 * @param adaptableObject the adaptable object being queried
	 *   (usually an instance of <code>IAdaptable</code>)
	 * @param adapterType the type of adapter to look up
	 * @return a object castable to the given adapter type, 
	 *    or <code>null</code> if this adapter factory 
	 *    does not have an adapter of the given type for the
	 *    given object
	 */
	public Object getAdapter(Object adaptableObject, Class<?> adapterType);

	/**
	 * Returns the collection of adapter types handled by this
	 * factory.
	 * <p>
	 * This method is generally used by an adapter manager
	 * to discover which adapter types are supported, in advance
	 * of dispatching any actual <code>getAdapter</code> requests.
	 * </p>
	 *
	 * @return the collection of adapter types
	 */
	public Class<?>[] getAdapterList();
}
