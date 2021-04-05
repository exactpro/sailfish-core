/******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.json;

import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.services.MessageHelper;

public class JSONMessageHelper extends MessageHelper {
    
    public static final String IS_URI_PARAM_ATTR = "IsURIParam";
    /**
     * It is used to mark collection of {@link IMessageStructure} or {@link IFieldStructure} and helps during decode/encode JSON.
     * Marked collection hasn't got special name in JSON object.
     * <pre>
     *      [ // Element marked by current attribute
     *          "b",
     *          "c"
     *      ]
     * </pre>
     */
    public static final String IS_NO_NAME_ATTR = "isNoName";
    /**
     * It is used to mark {@link IMessageStructure} and helps during encode JSON. {@link IMessageStructure} isn't encoded as JSON object.
     * Incorrect JSON example
     * <pre>
     *      // { Message marked  by current attribute
     *          "fieldA": "a",
     *          "fieldB": "b",
     *          "fieldC": "c"
     *      // }
     * </pre>
     * Array in a root node
     * <pre>
     *      [{
     *          "fieldA": "a",
     *          "fieldB": "b",
     *          "fieldC": "c"
     *      }]
     * </pre>
     */
    public static final String IS_NO_OBJECT_ATTR = "isNoObject";

    /**
     * Used to mark {@link IMessageStructure} which is encoded/decoded as an array.
     * Message fields are mapped into array's elements in order of their appearance
     */
    public static final String FROM_ARRAY_ATTR = "fromArray";

    // TODO: add those rules to the validator
    // TODO: create a validator for JSON dictionaries
    /**
     * Used to reorganize JSON object/map to the list of key-value pairs.<br/>
     * <b>Only for decoding.</b><br/>
     * Can be applied only for fields which are collections and have reference to a {@link IMessageStructure}.
     * The {@link IMessageStructure} must have <b>key</b> and <b>value</b> fields in its definition.
     *
     * <br/>
     * Original JSON:
     * <pre>
     *     {
     *         "foo": 42,
     *         "bar": 79
     *     }
     * </pre>
     *
     * The result:
     * <pre>
     *     [
     *          {
     *              "key": "foo",
     *              "value": 42
     *          },
     *          {
     *              "key": "bar",
     *              "value": 79
     *          }
     *     ]
     * </pre>
     */
    public static final String KEY_VALUE_LIST_ATTR = "keyValueList";
}
