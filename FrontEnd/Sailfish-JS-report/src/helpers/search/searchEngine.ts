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

import TestCase from '../../models/TestCase';
import SearchResult from './SearchResult';
import Message from '../../models/Message';
import Action, { isAction } from '../../models/Action';
import { keyForMessage, keyForAction, keyForActionParamter } from '../keys';
import ActionParameter from '../../models/ActionParameter';


// list of fields that will be used to search (order is important!)
const MESSAGE_FIELDS: Array<keyof Message> = ['msgName', 'from', 'to' ,'contentHumanReadable'],
    ACTION_FIELDS: Array<keyof Action> = ['name', 'description'],
    INPUT_PARAM_VALUE_FIELDS: Array<keyof ActionParameter> = ['name', 'value'],
    // we neeed to ignore 'value' filed in param nodes because it doesn't render
    INPUT_PARAM_NODE_FIELD: Array<keyof ActionParameter> = ['name'];

export function findAll(searchString: string, testCase: TestCase): SearchResult {
    const searchResults = new Array<[string, number]>();

    if (searchString) {
        testCase.actions
            .filter(isAction)
            .forEach(action => searchResults.push(...findAllInAction(action, searchString)));

        searchResults.push(...testCase.messages.reduce((acc, message) => [...acc, ...findAllInObject(
            message,
            MESSAGE_FIELDS,
            searchString,
            keyForMessage(message.id)
        )], []));
    }

    return new SearchResult(searchResults);
}

function findAllInAction(action: Action, searchString: string): Array<[string, number]> {
    let results = new Array<[string, number]>();

    results.push(...findAllInObject(action, ACTION_FIELDS, searchString, keyForAction(action.id)));

    action.subNodes
        .filter(isAction)
        .forEach(subAction => results.push(...findAllInAction(subAction, searchString)));

    action.parameters && action.parameters.forEach((param, index) => 
        results.push(...findAllInParams(param, searchString, keyForActionParamter(action.id, index)))
    )

    return results;
}

function findAllInParams(param: ActionParameter, searchString: string, keyPrefix: string): Array<[string, number]> {
    let results = new Array<[string, number]>();

    results.push(...findAllInObject(
        param, 
        param.subParameters && param.subParameters.length > 0 ? INPUT_PARAM_NODE_FIELD : INPUT_PARAM_VALUE_FIELDS, 
        searchString, 
        keyPrefix
    ));

    param.subParameters && param.subParameters.forEach((param, index) => {
        results.push(...findAllInParams(param, searchString, keyPrefix + `-${index}`));
    })

    return results;
}

/**
 * This funciton perfoms a search in a specific fields of target object 
 * and returns result as array of ["prefix - field name", number of search results] 
 * @param target target object
 * @param fieldsList list of fields of target object that will be used to search in them
 * @param searchString target search string
 * @param resultKeyPrefix prefix for search result key
 */
function findAllInObject<T>(target: T, fieldsList: Array<keyof T>, searchString: string, resultKeyPrefix: string): Array<[string, number]> {
    let results = new Array<[string, number]>();

    fieldsList.forEach(fieldName => {
        const targetField = target[fieldName];

        if (typeof targetField !== 'string') {
            if (targetField !== null) {
                console.warn(`Trying to search on field that doesn't look like string (${fieldName})`);
            }

            return;
        }

        const searchResultsCount = targetField.split(searchString).length - 1;

        if (searchResultsCount > 0) {
            results.push([`${resultKeyPrefix}-${fieldName}`, searchResultsCount]);
        }
    });

    return results;
}
