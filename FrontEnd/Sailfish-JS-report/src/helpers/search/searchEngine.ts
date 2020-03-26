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

import SearchResult from './SearchResult';
import Message from '../../models/Message';
import Action, { isAction, ActionNodeType } from '../../models/Action';
import {
    keyForMessage,
    keyForAction,
    keyForActionParameter,
    keyForVerification,
    keyForLog,
    keyForKnownBug
} from '../keys';
import ActionParameter from '../../models/ActionParameter';
import VerificationEntry from '../../models/VerificationEntry';
import Verification from '../../models/Verification';
import { isCheckpointAction } from '../action';
import { asyncFlatMap } from '../array';
import multiTokenSplit from "./multiTokenSplit";
import SearchToken, { PanelSearchToken } from "../../models/search/SearchToken";
import SearchSplitResult from "../../models/search/SearchSplitResult";
import SearchContent from "../../models/search/SearchContent";
import Log from "../../models/Log";
import KnownBug, { KnownBugNode } from "../../models/KnownBug";
import { isKnownBugCategory } from "../../models/KnownBugCategory";
import Panel from "../../util/Panel";

// list of fields that will be used to search (order is important!)
export const MESSAGE_FIELDS: Array<keyof Message> = ['msgName', 'from', 'to', 'contentHumanReadable', 'rawHex', 'rawHumanReadable'],
    ACTION_FIELDS: Array<keyof Action> = ['matrixId', 'serviceName', 'name', 'messageType', 'description'],
    LOG_FIELDS: Array<keyof Log> = ['thread', 'class', 'message'],
    KNOWN_BUG_FIELDS: Array<keyof KnownBug> = ['subject'],
    VERIFICATION_FIELDS: Array<keyof Verification> = ['name'],
    VERIFICATION_NODE_FIELDS: Array<keyof VerificationEntry> = ['name', 'expected', 'actual', 'status', 'actualType', 'expectedType'],
    INPUT_PARAM_VALUE_FIELDS: Array<keyof ActionParameter> = ['name', 'value'],
    // we need to ignore all fields besides 'name' in parent nodes because it doesn't render
    INPUT_PARAM_NODE_FIELD: Array<keyof ActionParameter> = ['name'];

export async function findAll(tokens: ReadonlyArray<PanelSearchToken>, content: SearchContent): Promise<SearchResult> {
    if (!tokens) {
        return new SearchResult();
    }

    const filteredActions = content.actions.filter(
        actionNode => isAction(actionNode) && !isCheckpointAction(actionNode)
    ) as Action[];

    const actionsTokens = tokens.filter(({panels}) => panels.includes(Panel.ACTIONS));
    const messageTokens = tokens.filter(({panels}) => panels.includes(Panel.MESSAGES));
    const kbTokens = tokens.filter(({panels}) => panels.includes(Panel.KNOWN_BUGS));
    const logTokens = tokens.filter(({panels}) => panels.includes(Panel.LOGS));

    const actionResults = await asyncFlatMap(filteredActions, item => findAllInAction(item, actionsTokens));
    const messageResults = await asyncFlatMap(content.messages, item => findAllInMessage(item, messageTokens));
    const kbResults = await asyncFlatMap(content.bugs, item => findAllInKnownBugNode(item, kbTokens));
    const logResults = await asyncFlatMap(content.logs, (item, index) => findAllInLog(item, logTokens, index));

    return new SearchResult([
        ...actionResults,
        ...messageResults,
        ...kbResults,
        ...logResults
    ]);
}

function findAllInMessage(message: Message, searchTokens: ReadonlyArray<SearchToken>): Array<[string, SearchSplitResult[]]> {
    return findAllInObject(
        message,
        MESSAGE_FIELDS,
        searchTokens,
        keyForMessage(message.id)
    );
}

function findAllInAction(action: Action, searchTokens: ReadonlyArray<SearchToken>): Array<[string, SearchSplitResult[]]> {
    let results = new Array<[string, SearchSplitResult[]]>();

    results.push(...findAllInObject(action, ACTION_FIELDS, searchTokens, keyForAction(action.id)));

    action.parameters?.forEach((param, index) =>
        results.push(...findAllInParams(param, searchTokens, keyForActionParameter(action.id, index)))
    );

    action.subNodes.forEach(subAction => {
        switch (subAction.actionNodeType) {
            case ActionNodeType.ACTION: {
                results.push(...findAllInAction(subAction, searchTokens));
                return;
            }

            case ActionNodeType.VERIFICATION: {
                results.push(...findAllInVerification(subAction, searchTokens, action.id));
                return;
            }

            default: {
                return;
            }
        }
    });

    return results;
}

function findAllInLog(log: Log, searchTokens: ReadonlyArray<SearchToken>, index: number): Array<[string, SearchSplitResult[]]> {
    return findAllInObject(
        log,
        LOG_FIELDS,
        searchTokens,
        keyForLog(index)
    )
}

function findAllInKnownBugNode(node: KnownBugNode, searchTokens: ReadonlyArray<SearchToken>): Array<[string, SearchSplitResult[]]> {
    if (isKnownBugCategory(node)) {
        return node.subNodes
            .flatMap(subNode => findAllInKnownBugNode(subNode, searchTokens));
    }

    return findAllInObject(
        node,
        KNOWN_BUG_FIELDS,
        searchTokens,
        keyForKnownBug(node)
    )
}

function findAllInParams(param: ActionParameter, searchTokens: ReadonlyArray<SearchToken>, keyPrefix: string): Array<[string, SearchSplitResult[]]> {
    let results = new Array<[string, SearchSplitResult[]]>();

    results.push(...findAllInObject(
        param,
        param.subParameters?.length > 0 ? INPUT_PARAM_NODE_FIELD : INPUT_PARAM_VALUE_FIELDS,
        searchTokens,
        keyPrefix
    ));

    param.subParameters?.forEach((param, index) => {
        results.push(...findAllInParams(param, searchTokens, `${keyPrefix}-${index}`));
    });

    return results;
}

function findAllInVerification(verification: Verification, searchTokens: ReadonlyArray<SearchToken>, parentActionId: number): Array<[string, SearchSplitResult[]]> {
    let results = new Array<[string, SearchSplitResult[]]>(),
        key = keyForVerification(parentActionId, verification.messageId);

    results.push(...findAllInObject(
        verification,
        VERIFICATION_FIELDS,
        searchTokens,
        key
    ));

    verification.entries?.forEach((entry, index) => {
        results.push(...findAllInVerificationEntries(entry, searchTokens, `${key}-${index}`));
    });

    return results;
}

function findAllInVerificationEntries(entry: VerificationEntry, searchTokens: ReadonlyArray<SearchToken>, keyPrefix: string): Array<[string, SearchSplitResult[]]> {
    let results = new Array<[string, SearchSplitResult[]]>();

    results.push(...findAllInObject(
        entry,
        VERIFICATION_NODE_FIELDS,
        searchTokens,
        keyPrefix
    ));

    entry.subEntries?.forEach((entry, index) => {
        results.push(...findAllInVerificationEntries(entry, searchTokens, `${keyPrefix}-${index}`));
    });

    return results;
}

/**
 * This function preforms a search in a specific fields of target object
 * and returns result as array of ["<prefix> - <field_name>", number of search results]
 * @param target target object
 * @param fieldsList list of fields of target object that will be used to search in them
 * @param searchTokens target search tokens
 * @param resultKeyPrefix prefix for search result key
 */
export function findAllInObject<T>(
    target: T,
    fieldsList: Array<keyof T>,
    searchTokens: ReadonlyArray<SearchToken>,
    resultKeyPrefix: string
): Array<[string, SearchSplitResult[]]> {

    let results = new Array<[string, SearchSplitResult[]]>();

    fieldsList.forEach(fieldName => {
        const targetField = target[fieldName];

        if (typeof targetField !== 'string') {
            if (targetField != null) {
                console.warn(`Trying to search on field that doesn't look like string (${fieldName})`);
            }

            return;
        }

        const searchResultsCount = multiTokenSplit(targetField, searchTokens);

        if (searchResultsCount.some(({ token }) => token != null)) {
            results.push([`${resultKeyPrefix}-${fieldName}`, searchResultsCount]);
        }
    });

    return results;
}
