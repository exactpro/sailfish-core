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
import Action, { isAction, ActionNodeType } from "../../models/Action";
import Verification from "../../models/Verification";
import { keyForAction, keyForMessage, keyForVerification } from "../keys";
import getActionCondition from "./conditions/action";
import getVerificationCondition from "./conditions/verification";
import getMessageCondition from "./conditions/message";
import FilterCondition from "./conditions/FilterCondition";
import { asyncFlatMap } from "../array";
import FilterType from "../../models/filter/FilterType";
import {FilterConfig} from "../../models/filter/FilterConfig";

/**
 * Filtrates target TestCase according to filter config.
 * @param testCase target TestCase
 * @param config configurations for filter 
 * @param config.types list of all types, that will be used for filtrating
 * @param config.blocks list of filter 'blocks' - combination of path and list of string values
 */
export default async function filtrate(testCase: TestCase, { types, blocks }: FilterConfig): Promise<string[]> {
    const results: string[] = [];

    if (types.includes(FilterType.ACTION) || types.includes(FilterType.VERIFICATION)) {
        const actions: Action[] = testCase.actions.filter(isAction);

        const actionsConditions = types.includes(FilterType.ACTION) ?
            blocks.map(({ path, values }) => getActionCondition(path, values)) :
            [];

        const verificationConditions = types.includes(FilterType.VERIFICATION) ? 
            blocks.map(({ path, values }) => getVerificationCondition(path, values)) :
            [];

        const mapper = createActionMapper(actionsConditions, verificationConditions);

        results.push(...await asyncFlatMap(actions, action => mapper(action)));
    }

    if (types.includes(FilterType.MESSAGE)) {
        const messageConditions = blocks.map(({ path, values }) => getMessageCondition(path, values));

        results.push(...await asyncFlatMap(testCase.messages, msg => 
            messageConditions.every(cond => cond(msg)) ? keyForMessage(msg.id) : []
        ));
    }

    return results;
}

function createActionMapper(actionConditions: FilterCondition<Action>[], verificationConditions: FilterCondition<Verification>[]) {
    return function actionMapper(action: Action): string[] {
        const results: string[] = [],
            key = keyForAction(action.id);

        if (actionConditions.length > 0 && actionConditions.every(condition => condition(action))) {
            results.push(key);
        }

        action.subNodes?.forEach(subNode => {
            switch (subNode.actionNodeType) {
                case ActionNodeType.ACTION:
                    const subNodeResults = actionMapper(subNode);

                    if (subNodeResults.length > 0) {
                        results.push(...subNodeResults);
                        results.push(key);
                    }

                    return;

                case ActionNodeType.VERIFICATION:
                    if (verificationConditions.length > 0 && verificationConditions.every(condition => condition(subNode))) {
                        results.push(keyForVerification(action.id, subNode.messageId));
                        results.push(key);
                    }

                    return;

                default:
                    return;
            }
        });

        return results;
    }
}
