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
import Action, { isAction, ActionNodeType } from '../../models/Action';
import { keyForMessage, keyForAction, keyForActionParamter, keyForVerification } from '../keys';
import ActionParameter from '../../models/ActionParameter';
import VerificationEntry from '../../models/VerificationEntry';
import Verification from '../../models/Verification';
import { createCaseInsensitiveRegexp } from '../regexp';
import { isCheckpoint } from '../actionType';

// list of fields that will be used to search (order is important!)
const MESSAGE_FIELDS: Array<keyof Message> = ['msgName', 'from', 'to' ,'contentHumanReadable'],
    ACTION_FIELDS: Array<keyof Action> = ['name', 'description'],
    VERIFICATION_FIELDS: Array<keyof Verification> = ['name'],
    INPUT_PARAM_VALUE_FIELDS: Array<keyof ActionParameter> = ['name', 'value'],
    VERIFICATION_VALUE_FIELDS: Array<keyof VerificationEntry> = ['name', 'expected', 'actual', 'status'],
    // we neeed to ignore all fileds besides 'name' in parent nodes because it doesn't render
    INPUT_PARAM_NODE_FIELD: Array<keyof ActionParameter> = ['name'],
    VERIFICATION_NODE_FEILDS: Array<keyof VerificationEntry> = ['name'];

export function findAll(searchString: string, testCase: TestCase): SearchResult {
    const searchResults = new Array<[string, number]>();

    if (searchString) {
        const searchPattern = createCaseInsensitiveRegexp(searchString);

        testCase.actions
            .filter(actionNode => isAction(actionNode) && !isCheckpoint(actionNode))
            .forEach(action => searchResults.push(...findAllInAction(action as Action, searchPattern)));

        testCase.messages.forEach(message => {
            searchResults.push(...findAllInObject(
                message,
                MESSAGE_FIELDS,
                searchPattern,
                keyForMessage(message.id)
            ));
        });
    }

    return new SearchResult(searchResults);
}

function findAllInAction(action: Action, searchPattern: RegExp): Array<[string, number]> {
    let results = new Array<[string, number]>();

    results.push(...findAllInObject(action, ACTION_FIELDS, searchPattern, keyForAction(action.id)));

    action.parameters && action.parameters.forEach((param, index) => 
        results.push(...findAllInParams(param, searchPattern, keyForActionParamter(action.id, index)))
    );

    action.subNodes.forEach(subAction => {
        switch(subAction.actionNodeType) { 
            case ActionNodeType.ACTION: {
                results.push(...findAllInAction(subAction, searchPattern));
                return;
            }

            case ActionNodeType.VERIFICATION: {
                results.push(...findAllInVerification(subAction, searchPattern, action.id));
                return;
            }

            default: {
                return;
            }
        }
    });

    return results;
}

function findAllInParams(param: ActionParameter, searchPattern: RegExp, keyPrefix: string): Array<[string, number]> {
    let results = new Array<[string, number]>();

    results.push(...findAllInObject(
        param, 
        param.subParameters && param.subParameters.length > 0 ? INPUT_PARAM_NODE_FIELD : INPUT_PARAM_VALUE_FIELDS, 
        searchPattern, 
        keyPrefix
    ));

    param.subParameters && param.subParameters.forEach((param, index) => {
        results.push(...findAllInParams(param, searchPattern, keyPrefix + `-${index}`));
    });

    return results;
}

function findAllInVerification(verification: Verification, searchPattern: RegExp, parentActionId: number): Array<[string, number]> {
    let results = new Array<[string, number]>(),
        key = keyForVerification(parentActionId, verification.messageId);

    results.push(...findAllInObject(
        verification,
        VERIFICATION_FIELDS,
        searchPattern,
        key
    ));

    verification.entries && verification.entries.forEach((entry, index) => {
        results.push(...findAllInVerificationEntries(entry, searchPattern, `${key}-${index}`));
    });

    return results;
}

function findAllInVerificationEntries(entry: VerificationEntry, searchPattern: RegExp, keyPrefix: string): Array<[string, number]> {
    let results = new Array<[string, number]>();

    results.push(...findAllInObject(
        entry,
        entry.subEntries && entry.subEntries.length !== 0 ? VERIFICATION_NODE_FEILDS : VERIFICATION_VALUE_FIELDS,
        searchPattern,
        keyPrefix
    ));

    entry.subEntries && entry.subEntries.forEach((entry, index) => {
        results.push(...findAllInVerificationEntries(entry, searchPattern, `${keyPrefix}-${index}`));
    });

    return results;
}

/**
 * This funciton perfoms a search in a specific fields of target object 
 * and returns result as array of ["prefix - field name", number of search results] 
 * @param target target object
 * @param fieldsList list of fields of target object that will be used to search in them
 * @param searchPattern target search string
 * @param resultKeyPrefix prefix for search result key
 */
function findAllInObject<T>(target: T, fieldsList: Array<keyof T>, searchPattern: RegExp, resultKeyPrefix: string): Array<[string, number]> {
    let results = new Array<[string, number]>();

    fieldsList.forEach(fieldName => {
        const targetField = target[fieldName];

        if (typeof targetField !== 'string') {
            if (targetField !== null) {
                console.warn(`Trying to search on field that doesn't look like string (${fieldName})`);
            }

            return;
        }

        const searchResultsCount = targetField.split(searchPattern).length - 1;

        if (searchResultsCount > 0) {
            results.push([`${resultKeyPrefix}-${fieldName}`, searchResultsCount]);
        }
    });

    return results;
}
