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
 * An adapter manager maintains a registry of adapter factories. Clients
 * directly invoke methods on an adapter manager to register and unregister
 * adapters. All adaptable objects (that is, objects that implement the <code>IAdaptable</code>
 * interface) funnel <code>IAdaptable.getAdapter</code> invocations to their
 * adapter manager's <code>IAdapterManger.getAdapter</code> method. The
 * adapter manager then forwards this request unmodified to the <code>IAdapterFactory.getAdapter</code>
 * method on one of the registered adapter factories.
 * <p>
 * Adapter factories can be registered programmatically using the <code>registerAdapters</code>
 * method.  Alternatively, they can be registered declaratively using the
 * <code>org.eclipse.core.runtime.adapters</code> extension point.  Factories registered
 * with this extension point will not be able to provide adapters until their
 * corresponding plugin has been activated.
 * <p>
 * The following code snippet shows how one might register an adapter of type
 * <code>com.example.acme.Sticky</code> on resources in the workspace.
 * <p>
 *
 * <pre>
 *  IAdapterFactory pr = new IAdapterFactory() {
 *  	public Class[] getAdapterList() {
 *  		return new Class[] { com.example.acme.Sticky.class };
 *  	}
 *  	public Object getAdapter(Object adaptableObject, Class adapterType) {
 *  		IResource res = (IResource) adaptableObject;
 *  		QualifiedName key = new QualifiedName(&quot;com.example.acme&quot;, &quot;sticky-note&quot;);
 *  		try {
 *  			com.example.acme.Sticky v = (com.example.acme.Sticky) res.getSessionProperty(key);
 *  			if (v == null) {
 *  				v = new com.example.acme.Sticky();
 *  				res.setSessionProperty(key, v);
 *  			}
 *  		} catch (CoreException e) {
 *  			// unable to access session property - ignore
 *  		}
 *  		return v;
 *  	}
 *  }
 *  Platform.getAdapterManager().registerAdapters(pr, IResource.class);
 *   </pre>
 *
 * </p><p>
 * This interface can be used without OSGi running.
 * </p><p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @see IAdaptable
 * @see IAdapterFactory
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IAdapterManager {

	/**
	 * This value can be returned to indicate that no applicable adapter factory
	 * was found.
	 * @since org.eclipse.equinox.common 3.3
	 */
	public static final int NONE = 0;

	/**
	 * This value can be returned to indicate that an adapter factory was found,
	 * but has not been loaded.
	 * @since org.eclipse.equinox.common 3.3
	 */
	public static final int NOT_LOADED = 1;

	/**
	 * This value can be returned to indicate that an adapter factory is loaded.
	 * @since org.eclipse.equinox.common 3.3
	 */
	public static final int LOADED = 2;

	/**
	 * Returns the types that can be obtained by converting <code>adaptableClass</code>
	 * via this manager. Converting means that subsequent calls to <code>getAdapter()</code>
	 * or <code>loadAdapter()</code> could result in an adapted object.
	 * <p>
	 * Note that the returned types do not guarantee that
	 * a subsequent call to <code>getAdapter</code> with the same type as an argument
	 * will return a non-null result. If the factory's plug-in has not yet been
	 * loaded, or if the factory itself returns <code>null</code>, then
	 * <code>getAdapter</code> will still return <code>null</code>.
	 * </p>
	 * @param adaptableClass the adaptable class being queried
	 * @return an array of type names that can be obtained by converting
	 * <code>adaptableClass</code> via this manager. An empty array
	 * is returned if there are none.
	 * @since 3.1
	 */
	public String[] computeAdapterTypes(Class<?> adaptableClass);

	/**
	 * Returns the class search order for a given class. The search order from a
	 * class with the definition <br>
	 * <code>class X extends Y implements A, B</code><br>
	 * is as follows:
	 * <ul>
	 * <li>the target's class: X
	 * <li>X's superclasses in order to <code>Object</code>
	 * <li>a breadth-first traversal of each class's interfaces in the
	 * order returned by <code>getInterfaces</code> (in the example, X's
	 * superinterfaces then Y's superinterfaces) </li>
	 * </ul>
	 *
	 * @param clazz the class for which to return the class order.
	 * @return the class search order for the given class. The returned
	 * search order will minimally  contain the target class.
	 * @since 3.1
	 */
	public Class<?>[] computeClassOrder(Class<?> clazz);

	/**
	 * Returns an object which is an instance of the given class associated
	 * with the given object. Returns <code>null</code> if no such object can
	 * be found.
	 * <p>
	 * Note that this method will never cause plug-ins to be loaded. If the
	 * only suitable factory is not yet loaded, this method will return <code>null</code>.
	 *
	 * @param adaptable the adaptable object being queried (usually an instance
	 * of <code>IAdaptable</code>)
	 * @param adapterType the type of adapter to look up
	 * @return an object castable to the given adapter type, or <code>null</code>
	 * if the given adaptable object does not have an available adapter of the
	 * given type
	 */
	public Object getAdapter(Object adaptable, Class<?> adapterType);

	public Object getAdapter(Class<?> clazz,  Class<?> adapterType);

	/**
	 * Returns whether there is an adapter factory registered that may be able
	 * to convert <code>adaptable</code> to an object of type <code>adapterType</code>.
	 * <p>
	 * Note that a return value of <code>true</code> does not guarantee that
	 * a subsequent call to <code>getAdapter</code> with the same arguments
	 * will return a non-null result. If the factory's plug-in has not yet been
	 * loaded, or if the factory itself returns <code>null</code>, then
	 * <code>getAdapter</code> will still return <code>null</code>.
	 *
	 * @param adaptable the adaptable object being queried (usually an instance
	 * of <code>IAdaptable</code>)
	 * @param adapterType the class of an adapter to look up
	 * @return <code>true</code> if there is an adapter factory that claims
	 * it can convert <code>adaptable</code> to an object of type <code>adapterType</code>,
	 * and <code>false</code> otherwise.
	 * @since 3.0
	 */
	public boolean hasAdapter(Object adaptable, Class<?> adapterType);

	/**
	 * Returns a status of an adapter factory registered that may be able
	 * to convert <code>adaptable</code> to an object of type <code>adapterType</code>.
	 * <p>
	 * One of the following values can be returned:<ul>
	 * <li>{@link org.eclipse.core.runtime.IAdapterManager#NONE} if no applicable adapter factory was found;</li>
	 * <li>{@link org.eclipse.core.runtime.IAdapterManager#NOT_LOADED} if an adapter factory was found, but has not been loaded;</li>
	 * <li>{@link org.eclipse.core.runtime.IAdapterManager#LOADED} if an adapter factory was found, and it is loaded.</li>
	 * </ul></p>
	 * @param adaptable the adaptable object being queried (usually an instance
	 * of <code>IAdaptable</code>)
	 * @param adapterType the class of an adapter to look up
	 * @return a status of the adapter
	 * @since org.eclipse.equinox.common 3.3
	 */
	public int queryAdapter(Object adaptable, Class<?> adapterType);

	/**
	 * Registers the given adapter factory as extending objects of the given
	 * type.
	 * <p>
	 * If the type being extended is a class, the given factory's adapters are
	 * available on instances of that class and any of its subclasses. If it is
	 * an interface, the adapters are available to all classes that directly or
	 * indirectly implement that interface.
	 * </p>
	 *
	 * @param factory the adapter factory
	 * @param adaptable the type being extended
	 * @see #unregisterAdapters(IAdapterFactory)
	 * @see #unregisterAdapters(IAdapterFactory, Class)
	 */
	public void registerAdapters(IAdapterFactory factory, Class<?> adaptable);

	/**
	 * Removes the given adapter factory completely from the list of registered
	 * factories. Equivalent to calling <code>unregisterAdapters(IAdapterFactory,Class)</code>
	 * on all classes against which it had been explicitly registered. Does
	 * nothing if the given factory is not currently registered.
	 *
	 * @param factory the adapter factory to remove
	 * @see #registerAdapters(IAdapterFactory, Class)
	 */
	public void unregisterAdapters(IAdapterFactory factory);

	/**
	 * Removes the given adapter factory from the list of factories registered
	 * as extending the given class. Does nothing if the given factory and type
	 * combination is not registered.
	 *
	 * @param factory the adapter factory to remove
	 * @param adaptable one of the types against which the given factory is
	 * registered
	 * @see #registerAdapters(IAdapterFactory, Class)
	 */
	public void unregisterAdapters(IAdapterFactory factory, Class<?> adaptable);
}
