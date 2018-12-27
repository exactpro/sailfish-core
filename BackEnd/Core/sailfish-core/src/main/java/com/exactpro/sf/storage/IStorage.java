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
package com.exactpro.sf.storage;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;

public interface IStorage {
	
	void add(Object entity) throws StorageException;
	
	void batchAdd(List<Object> entities) throws StorageException;
	
	void update(Object entity) throws StorageException;
	
	void delete(Object entity) throws StorageException;
	
	Object getEntityById(Class<?> entityClass, Long id);
	
	<T> T getEntityById(Class<T> entityClass, String id);
	
	<T> T getEntityByField(Class<T> entityClass, String fieldName, Object value);
	
	<T> List<T> getAllEntities(Class<T> entityClass);
	
	<T> List<T> getAllEntities(String className);
	
	<T> List<T> getAllEntities(Class<T> entityClass, List<Criterion> criterions);
	
	void clear(String[] entities);
	
	SessionFactory getSessionFactory();

	<T> List<T> getAllEntities(Class<T> entityClass, List<Criterion> criterions,
			String orderField, boolean orderAsc);

}
