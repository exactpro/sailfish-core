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
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.StringType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TruncatedString implements UserType, ParameterizedType, Serializable {
    private static final long serialVersionUID = 605263450166121405L;

    private static final Logger logger = LoggerFactory.getLogger(TruncatedString.class);

    private static final int DEFAULT_LENGTH = 32672;
    private static final String LENGTH_PARAM = "length";
    private int maxLength = DEFAULT_LENGTH;

    @Override
    public void setParameterValues(Properties parameters) {
        try {
            this.maxLength = Integer.parseInt(parameters.getProperty(LENGTH_PARAM));
        } catch (Exception e) {
            logger.warn("TruncatedString doesn't have [" + LENGTH_PARAM + "] param", e);
        }
    }

    @Override
    public int[] sqlTypes() {
        return new int[] { java.sql.Types.VARCHAR };
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class returnedClass() {
        return String.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return (x == y) || (x != null && y != null && (x.equals(y)));
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        return StringType.INSTANCE.nullSafeGet(rs, names[0], session);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
        String val = (String) value;
        if (val != null && val.length() > this.maxLength) {
            val = val.substring(0, maxLength);
        }
        st.setString(index, val);
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        if (value == null) {
            return null;
        }
        return new String(((String) value));
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
}
