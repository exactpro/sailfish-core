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
package com.exactpro.sf.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUMap<K, V> extends LinkedHashMap<K, V> {

	private static final long serialVersionUID = -8659090900298278542L;

	private final int maxSize;

	public LRUMap(final int maxSize) {
		super();
		this.maxSize = maxSize;
	}

	public LRUMap(int maxSize, int initialCapacity, float loadFactor, boolean accessOrder) {
		super(initialCapacity, loadFactor, accessOrder);
		this.maxSize = maxSize;
	}

	public LRUMap(int maxSize, int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		this.maxSize = maxSize;
	}

	public LRUMap(int maxSize, int initialCapacity) {
		super(initialCapacity);
		this.maxSize = maxSize;
	}

	public LRUMap(int maxSize, Map<? extends K, ? extends V> m) {
		super(m);
		this.maxSize = maxSize;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
		return size() > maxSize;
	}
}
