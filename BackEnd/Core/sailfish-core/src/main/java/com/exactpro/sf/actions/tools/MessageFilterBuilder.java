/******************************************************************************
 *  Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *       http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either 
 *  express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 ******************************************************************************/

package com.exactpro.sf.actions.tools;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.util.EPSCommonException;

import java.util.ArrayList;
import java.util.List;

public final class MessageFilterBuilder {

    private IMessage result;
    private IMessage tmp;
    private final IMessageFactory msgFactory;

    public MessageFilterBuilder(IMessageFactory msgFactory) {
        this.msgFactory = msgFactory;
    }

    public IMessage build() {
        checkRootExists();
        return result;
    }

    /***
     * Attach alredy defined repeating group to current submessage and set its current
     * @param name field of current message where will be writen ref
     * @param ref reference defined by user
     * @return
     */
    public MessageFilterBuilder m(String name, IMessage ref) {
        checkRootExists();
        tmp.addField(name, ref);
        tmp = tmp.getField(name);

        return this;
    }

    /***
     * attach new submessage with type = type to current message by name key and set created submessage as current
     * @param name field of current message where will be writen ref
     * @param type name of submessage to create
     * @return
     */
    public MessageFilterBuilder m(String name, String type) {
        checkRootExists();
        tmp.addField(name, msgFactory.createMessage(type));
        tmp = tmp.getField(name);

        return this;
    }

    /***
     * use if key and type are equal
     * attach new submessage with type = type to current message by type key and set created submessage as current
     * @param type name of submessage to create
     * @return
     */
    public MessageFilterBuilder meq(String type) {
        checkRootExists();
        tmp.addField(type, msgFactory.createMessage(type));
        tmp = tmp.getField(type);

        return this;
    }

    /***
     * create root message and set it as current
     * @param type name of submessage to create
     * @return
     */
    public MessageFilterBuilder m(String type) {
        tmp = msgFactory.createMessage(type);
        if (result == null) {
            result = tmp;
        } else {
            throw new EPSCommonException("Head already created");
        }

        return this;
    }

    /***
     * attach new collection item with type to name field. item sets as current
     * @param name field of current message where will be writen ref
     * @param type name of collection item to create
     * @return
     */
    public MessageFilterBuilder c(String name, String type) {
        checkRootExists();
        List<IMessage> list = new ArrayList<>();
        tmp.addField(name, list);
        tmp = msgFactory.createMessage(type);
        list.add(tmp);

        return this;
    }

    private void checkRootExists() {
        if (result == null || tmp == null) {
            throw new EPSCommonException("m(type) must called first");
        }
    }

}
