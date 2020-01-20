/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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

import filterEntry from "../filterEntry";
import { MESSAGE_FIELDS } from "../../search/searchEngine";
import Message from "../../../models/Message";
import FilterCondition from "./FilterCondition";
import {toRegExpArray} from "../../regexp";
import FilterPath from "../../../models/filter/FilterPath";

const STUB_FUNCTION = () => false;

export default function getMessageCondition(path: FilterPath, values: string[]): FilterCondition<Message> {
    switch (path) {
        case FilterPath.SERVICE:
            return message => filterEntry(message, ['from', 'to'], toRegExpArray(values));

        case FilterPath.STATUS:
            return STUB_FUNCTION;

        case FilterPath.ALL:
            return message => filterEntry(message, MESSAGE_FIELDS, toRegExpArray(values));

        default:
            return STUB_FUNCTION;
    }
}
