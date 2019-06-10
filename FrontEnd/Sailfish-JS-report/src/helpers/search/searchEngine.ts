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

// list of fields that will be used to search (order is important!)
const MESSAGE_FIELDS: Array<keyof Message> = ['msgName', 'from', 'to' ,'contentHumanReadable'],
    ACTION_FIELDS: Array<keyof Action> = ['name', 'description'];

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
            `msg-${message.id}`
        )], []));
    }

    return new SearchResult(searchResults);
}

function findAllInAction(action: Action, searchString: string): Array<[string, number]> {
    let results = new Array<[string, number]>();

    results.push(...findAllInObject(action, ACTION_FIELDS, searchString, `action-${action.id}`));

    action.subNodes
        .filter(isAction)
        .forEach(subAction => results.push(...findAllInAction(subAction, searchString)));

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
