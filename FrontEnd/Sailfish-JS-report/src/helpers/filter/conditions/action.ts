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

import Action from "../../../models/Action";
import filterEntry from "../filterEntry";
import { ACTION_FIELDS, INPUT_PARAM_NODE_FIELD, INPUT_PARAM_VALUE_FIELDS } from "../../search/searchEngine";
import FilterCondition from "./FilterCondition";
import {toRegExpArray} from "../../regexp";
import FilterPath from "../../../models/filter/FilterPath";
import ActionParameter from "../../../models/ActionParameter";

const STUB_FUNCTION = () => false;

export default function getActionCondition(path: FilterPath, values: string[]): FilterCondition<Action> {
    switch (path) {
        case FilterPath.SERVICE:
            return action => filterEntry(action, ['serviceName'], toRegExpArray(values));

        case FilterPath.STATUS:
            return action => values.includes(action.status.status);

        case FilterPath.ALL:
            return action => filterEntry(action, ACTION_FIELDS, toRegExpArray(values)) ||
                filterInputParams(action.parameters, toRegExpArray(values));

        default:
            return STUB_FUNCTION;
    }
}

function filterInputParams(params: ActionParameter[] | null, values: RegExp[]): boolean {
    if (params == null) {
        return false;
    }


    return params.some(param => checkParamEntry(param, values));
}

function checkParamEntry(param: ActionParameter, values: RegExp[]): boolean {
    if (param.subParameters == null) {
        return filterEntry(param, INPUT_PARAM_VALUE_FIELDS, values);
    }

    return filterEntry(param, INPUT_PARAM_NODE_FIELD, values) ||
        param.subParameters.some(subParam => checkParamEntry(subParam, values));
}
