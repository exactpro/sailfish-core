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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.adapting.IAdapterFactory;
import com.exactpro.sf.common.adapting.IAdapterManager;
import com.exactpro.sf.common.adapting.impl.DefaultAdapterFactory;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.adapters.AdapterDefinition;
import com.exactpro.sf.configuration.adapters.Adapters;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * This class is the standard implementation of <code>IAdapterManager</code>. It provides
 * fast lookup of property values with the following semantics:
 * <ul>
 * <li>At most one factory will be invoked per property lookup
 * <li>If multiple installed factories provide the same adapter, only the first found in
 * the search order will be invoked.
 * <li>The search order from a class with the definition <br>
 * <code>class X extends Y implements A, B</code><br> is as follows: <il>
 * <li>the target's class: X
 * <li>X's superclasses in order to <code>Object</code>
 * <li>a breadth-first traversal of each class's interfaces in the
 * order returned by <code>getInterfaces</code> (in the example, X's
 * superinterfaces then Y's superinterfaces) </li>
 * </ul>
 *
 * @see IAdapterFactory
 * @see IAdapterManager
 */

public final class DefaultAdapterManager implements IAdapterManager, ILoadableManager {

    private static final Logger logger = LoggerFactory.getLogger(DefaultAdapterManager.class);

    /**
     * Cache of adapters for a given adaptable class. Maps Class -> Map
     * (adaptable class -> (adapter class -> factory instance)) Thread safety
     * note: The outer map is synchronized using a synchronized map wrapper
     * class. The inner map is not synchronized, but it is immutable so
     * synchronization is not necessary.
     */
	private Map<Class<?>, Map<Class<?>, IAdapterFactory>> adapterLookup;

	/**
	 * Cache of class lookup order (Class -> Class[]). This avoids having to compute often, and
	 * provides clients with quick lookup for instanceOf checks based on type class.
	 * Thread safety note: The map is synchronized using a synchronized
	 * map wrapper class.  The arrays within the map are immutable.
	 */
	private Map<Class<?>, Class<?>[]> classSearchOrderLookup;

	/**
	 * Map of factories, keyed by <code>Class</code>, the adaptable class
	 * that the factory provides adapters for. Value is a <code>List</code>
	 * of <code>IAdapterFactory</code>.
	 */
	private final Map<Class<?>, List<IAdapterFactory>> factories;

	private static final DefaultAdapterManager singleton = new DefaultAdapterManager();

	public static DefaultAdapterManager getDefault() {
		return singleton;
	}

    @Override
    public void load(ILoadableManagerContext context) {
        Multimap<String, AdapterDescription> mapDescrs = ArrayListMultimap.create();

        ClassLoader classLoader = context.getClassLoaders()[0];
        InputStream stream = context.getResourceStream();
        
        try {
            JAXBContext jc = JAXBContext.newInstance(Adapters.class);
            Unmarshaller u = jc.createUnmarshaller();

            JAXBElement<Adapters> root = u.unmarshal(new StreamSource(stream), Adapters.class);
            Adapters adaptersXml = root.getValue();

            for (AdapterDefinition adapterXml : adaptersXml.getAdapter()) {
                AdapterDescription descr = new AdapterDescription();

                descr.setAdapterClass(adapterXml.getAdapterClass());
                descr.setAdapterClassImpl(adapterXml.getAdapterClassImpl());
                descr.setAdapterForClass(adapterXml.getAdapterForClass());

                mapDescrs.put(descr.getAdapterForClass(), descr);
            }

            for (String keyClass : mapDescrs.keySet()) {
                Collection<AdapterDescription> adapters = mapDescrs.get(keyClass);

                Map<Class<?>, Class<?>> adapterMapClass = new HashMap<>();

                Class<?>[] adapterList = new Class<?>[adapters.size()];

                Class<?> adaptableClass = null;

                try {
                    adaptableClass = classLoader.loadClass(keyClass);
                } catch (ClassNotFoundException e) {
                    throw new EPSCommonException(e);
                }

                int i = 0;
                for (AdapterDescription adDescr : adapters) {
                    try {
                        Class<?> adapterClass = classLoader.loadClass(adDescr.getAdapterClass());
                        Class<?> adapterClassImpl = classLoader.loadClass(adDescr.getAdapterClassImpl());

                        adapterMapClass.put(adapterClass, adapterClassImpl);
                        adapterList[i++] = adapterClass;

                    } catch (ClassNotFoundException e) {
                        throw new EPSCommonException(e);
                    }
                }

                DefaultAdapterFactory adfactory = new DefaultAdapterFactory(adapterList, adapterMapClass);

                registerAdapters(adfactory, adaptableClass);

                logger.info("Register Adapter for {}", keyClass);
            }

        } catch (JAXBException e) {
            throw new EPSCommonException("Failed to load adapter", e);
        }
    }

    @Override
    public void finalize(ILoadableManagerContext context) throws Exception {
        // TODO Auto-generated method stub

    }

	/**
	 * Private constructor to block instance creation.
	 */
	private DefaultAdapterManager() {
		factories = new HashMap<>(5);
	}

	/**
	 * Given a type class, add all of the factories that respond to those types into
	 * the given table. Each entry will be keyed by the adapter class (supplied in
	 * IAdapterFactory.getAdapterList).
	 */
	private void addFactoriesFor(Class<?> type, Map<Class<?>, IAdapterFactory> table) {
		List<IAdapterFactory> factoryList = getFactories().get(type);
		if (factoryList == null)
			return;
		for (int i = 0, imax = factoryList.size(); i < imax; i++) {
			IAdapterFactory factory = factoryList.get(i);

			Class<?>[] adapters = factory.getAdapterList();
			for (int j = 0; j < adapters.length; j++) {
				if (table.get(adapters[j]) == null)
					table.put(adapters[j], factory);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterManager#getAdapterTypes(java.lang.Class)
	 */
	@Override
    public String[] computeAdapterTypes(Class<?> adaptable) {
		Set<?> types = getFactories(adaptable).keySet();
		return types.toArray(new String[types.size()]);
	}

	/**
	 * Computes the adapters that the provided class can adapt to, along
	 * with the factory object that can perform that transformation. Returns
	 * a table of adapter class to factory object.
	 * @param adaptable
	 */
	private Map<Class<?>, IAdapterFactory> getFactories(Class<?> adaptable) {
		//cache reference to lookup to protect against concurrent flush
		Map<Class<?>, Map<Class<?>, IAdapterFactory>> lookup = adapterLookup;
		if (lookup == null)
			adapterLookup = lookup = Collections.synchronizedMap(new HashMap<Class<?>, Map<Class<?>, IAdapterFactory>>(30));
		Map<Class<?>, IAdapterFactory> table = lookup.get(adaptable);
		if (table == null) {
			// calculate adapters for the class
			table = new HashMap<>(4);
			Class<?>[] classes = computeClassOrder(adaptable);
			for (int i = 0; i < classes.length; i++)
				addFactoriesFor(classes[i], table);
			// cache the table
			lookup.put(adaptable, table);
		}
		return table;
	}

	/**
	 * Returns the super-type search order starting with <code>adaptable</code>.
	 * The search order is defined in this class' comment.
	 */
	@Override
    public Class<?>[] computeClassOrder(Class<?> adaptable) {
		Class<?>[] classes = null;
		//cache reference to lookup to protect against concurrent flush
		Map<Class<?>, Class<?>[]> lookup = classSearchOrderLookup;
		if (lookup == null)
			classSearchOrderLookup = lookup = Collections.synchronizedMap(new HashMap<Class<?>, Class<?>[]>());
		else
			classes = lookup.get(adaptable);
		// compute class order only if it hasn't been cached before
		if (classes == null) {
			classes = doComputeClassOrder(adaptable);
			lookup.put(adaptable, classes);
		}
		return classes;
	}

	/**
	 * Computes the super-type search order starting with <code>adaptable</code>.
	 * The search order is defined in this class' comment.
	 */
	private Class<?>[] doComputeClassOrder(Class<?> adaptable) {
		List<Class<?>> classes = new ArrayList<>();
		Class<?> clazz = adaptable;
		Set<Class<?>> seen = new HashSet<>(4);
		//first traverse class hierarchy
		while (clazz != null) {
			classes.add(clazz);
			clazz = clazz.getSuperclass();
		}
		//now traverse interface hierarchy for each class
		Class<?>[] classHierarchy = classes.toArray(new Class<?>[classes.size()]);
		for (int i = 0; i < classHierarchy.length; i++)
			computeInterfaceOrder(classHierarchy[i].getInterfaces(), classes, seen);
		return classes.toArray(new Class<?>[classes.size()]);
	}

	private void computeInterfaceOrder(Class<?>[] interfaces, Collection<Class<?>> classes, Set<Class<?>> seen) {
		List<Class<?>> newInterfaces = new ArrayList<>(interfaces.length);
		for (int i = 0; i < interfaces.length; i++) {
			Class<?> interfac = interfaces[i];
			if (seen.add(interfac)) {
				//note we cannot recurse here without changing the resulting interface order
				classes.add(interfac);
				newInterfaces.add(interfac);
			}
		}
		for (Iterator<Class<?>> it = newInterfaces.iterator(); it.hasNext();)
			computeInterfaceOrder(it.next().getInterfaces(), classes, seen);
	}

	/**
	 * Flushes the cache of adapter search paths. This is generally required whenever an
	 * adapter is added or removed.
	 * <p>
	 * It is likely easier to just toss the whole cache rather than trying to be smart
	 * and remove only those entries affected.
	 * </p>
	 */
	public synchronized void flushLookup() {
		adapterLookup = null;
		classSearchOrderLookup = null;
	}


	@Override
    public Object getAdapter(Class<?> clazz, Class<?> adapterType)
	{
		IAdapterFactory factory = getFactories(clazz).get(adapterType);
		Object result = null;
		if (factory != null)
			result = factory.getAdapter(null, adapterType);
		return result;

	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterManager#getAdapter(java.lang.Object, java.lang.Class)
	 */
	@Override
    public Object getAdapter(Object adaptable, Class<?> adapterType) {
		IAdapterFactory factory = getFactories(adaptable.getClass()).get(adapterType);
		Object result = null;
		if (factory != null)
			result = factory.getAdapter(adaptable, adapterType);
		if (result == null && adapterType.isInstance(adaptable))
			return adaptable;
		return result;
	}

	@Override
    public boolean hasAdapter(Object adaptable, Class<?> adapterType) {
		return getFactories(adaptable.getClass()).get(adapterType) != null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterManager#queryAdapter(java.lang.Object, java.lang.Class)
	 */
	@Override
    public int queryAdapter(Object adaptable, Class<?> adapterType) {
		IAdapterFactory factory = getFactories(adaptable.getClass()).get(adapterType);
		if (factory == null)
			return NONE;

		return LOADED;
	}

	/*
	 * @see IAdapterManager#registerAdapters
	 */
	@Override
    public synchronized void registerAdapters(IAdapterFactory factory, Class<?> adaptable) {
		registerFactory(factory, adaptable);
		flushLookup();
	}

	/*
	 * @see IAdapterManager#registerAdapters
	 */
	public void registerFactory(IAdapterFactory factory, Class<?> adaptable) {
		List<IAdapterFactory> list = factories.get(adaptable);
		if (list == null) {
			list = new ArrayList<>(5);
			factories.put(adaptable, list);
		}
		list.add(factory);
	}

	/*
	 * @see IAdapterManager#unregisterAdapters
	 */
	@Override
    public synchronized void unregisterAdapters(IAdapterFactory factory) {
		for (Iterator<List<IAdapterFactory>> it = factories.values().iterator(); it.hasNext();)
			it.next().remove(factory);
		flushLookup();
	}

	/*
	 * @see IAdapterManager#unregisterAdapters
	 */
	@Override
    public synchronized void unregisterAdapters(IAdapterFactory factory, Class<?> adaptable) {
		List<IAdapterFactory> factoryList = factories.get(adaptable);
		if (factoryList == null)
			return;
		factoryList.remove(factory);
		flushLookup();
	}

	/*
	 * Shuts down the adapter manager by removing all factories
	 * and removing the registry change listener. Should only be
	 * invoked during platform shutdown.
	 */
	public synchronized void unregisterAllAdapters() {
		factories.clear();
		flushLookup();
	}

	public Map<Class<?>, List<IAdapterFactory>> getFactories() {
		return factories;
	}

}
