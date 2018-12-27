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

import com.exactpro.sf.storage.entities.StoredMessage;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import java.util.Iterator;

public class MessageLoader implements Iterator<StoredMessage> {
    private ScrollableResults results;
    private Session session;
    private int numberOfRecords;

    public MessageLoader(ScrollableResults results, Session session) {
        this.results = results;
        this.session = session;
        results.last();
        numberOfRecords = results.getRowNumber() + 1;
        results.beforeFirst();
    }

    @Override
    public boolean hasNext() {
        boolean result = results.next();
        results.previous();
        return result;
    }

    @Override
    public StoredMessage next() {
        results.next();
        return (StoredMessage)results.get(0);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("The iterator's implementation for read only");
    }

    public void close(){
        if(session.isOpen()) {
            session.close();
        }
    }

    public int getNumberOfRecords() {
        return numberOfRecords;
    }
    
    public void previous() {
    	results.previous();
    }
}
