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

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.LongType;
import org.hibernate.usertype.UserType;

public class TimestampToLong implements UserType, Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6302440546874233292L;

	@Override
	public Object assemble(Serializable cached, Object obj) throws HibernateException {
		return cached;
	}

	@Override
	public Object deepCopy(Object obj) throws HibernateException {
		if (obj == null) return null;
		Date orig = (Date) obj;
		return new Timestamp(orig.getTime());
	}

	@Override
	public Serializable disassemble(Object obj) throws HibernateException {
		return (Serializable) obj;
	}

	@Override
	public boolean equals(Object a, Object b) throws HibernateException {
		return a.equals(b);
	}

	@Override
	public int hashCode(Object obj) throws HibernateException {
		return obj.hashCode();
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Object nullSafeGet(ResultSet ps, String[] names, SessionImplementor s, Object owner)
			throws HibernateException, SQLException {
		Long value = LongType.INSTANCE.nullSafeGet(ps, names[0], s);
		return new Timestamp(value);
	}

	@Override
	public void nullSafeSet(PreparedStatement ps, Object value, int index, SessionImplementor s)
			throws HibernateException, SQLException {		
		 if (value == null)
			 LongType.INSTANCE.set(ps, 0L, index, s);
		 else
			 LongType.INSTANCE.set(ps, ((Date) value).getTime(), index, s);

	}

	@Override
	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return original;
	}

	@Override
	public Class<?> returnedClass() {
		return Timestamp.class;
	}

	@Override
	public int[] sqlTypes() {
		return new int[] { java.sql.Types.BIGINT };
	}

}
