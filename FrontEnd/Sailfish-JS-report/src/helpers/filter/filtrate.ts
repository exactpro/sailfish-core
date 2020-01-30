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

import TestCase from "../../models/TestCase";
import Action, { ActionNodeType, isAction } from "../../models/Action";
import Verification from "../../models/Verification";
import { keyForAction, keyForMessage, keyForVerification } from "../keys";
import getActionCondition from "./conditions/action";
import getVerificationCondition from "./conditions/verification";
import getMessageCondition from "./conditions/message";
import FilterCondition from "./conditions/FilterCondition";
import { asyncFlatMap } from "../array";
import FilterType from "../../models/filter/FilterType";
import { FilterBlock } from "../../models/filter/FilterBlock";
import Message from "../../models/Message";

/**
 * Filtrates target TestCase according to filter config.
 * @param testCase target TestCase
 * @param config configurations for filter
 * @param config.types list of all types, that will be used for filtrating
 * @param config.blocks list of filter 'blocks' - combination of path and list of string values
 */
export default async function filtrate(testCase: TestCase, blocks: FilterBlock[]): Promise<string[]> {
    const results: string[] = [];

    const notEmptyBlocks = blocks.filter(({ values }) => values.length > 0),
        actionBlocks = notEmptyBlocks.filter(({ types }) => types.includes(FilterType.ACTION)),
        messageBlocks = notEmptyBlocks.filter(({ types }) => types.includes(FilterType.MESSAGE));

    if (actionBlocks.length > 0) {
        const actionConditions = actionBlocks
            .map(({ path, values }) => getActionCondition(path, values));

        const verificationConditions = actionBlocks
            .map(({ path, values }) => getVerificationCondition(path, values));

        const actionMapper = createActionMapper(actionConditions, verificationConditions);

        results.push(...await asyncFlatMap(testCase.actions, actionMapper));
    }

    if (messageBlocks.length > 0) {
        const messageConditions = messageBlocks
            .map(({ path, values }) => getMessageCondition(path, values));

        const messageMapper = (msg: Message) => (
            messageConditions.every(cond => cond(msg)) ? keyForMessage(msg.id) : []
        );

        results.push(...await asyncFlatMap(testCase.messages, messageMapper));
    }

    return results;
}

function createActionMapper(actionConditions: FilterCondition<Action>[], verificationConditions: FilterCondition<Verification>[]) {
    return function actionMapper(action: Action): string[] {

        const subNodeResults = action.subNodes?.flatMap(subNode => {
            switch (subNode.actionNodeType) {
                case ActionNodeType.ACTION:
                    return actionMapper(subNode);

                case ActionNodeType.VERIFICATION:
                    if (verificationConditions.length > 0 && verificationConditions.every(condition => condition(subNode))) {
                        return keyForVerification(action.id, subNode.messageId);
                    }

                    return [];

                default:
                    return [];
            }
        }) ?? [];

        if ((actionConditions.length > 0 && actionConditions.every(condition => condition(action))) || subNodeResults.length > 0) {
            return [
                ...subNodeResults,
                keyForAction(action.id)
            ];
        }

        return [];
    }
}
