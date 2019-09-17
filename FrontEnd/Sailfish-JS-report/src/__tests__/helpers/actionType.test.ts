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

import { getActions, getStatusChipDescription } from "../../helpers/action";
import { ActionNode, ActionNodeType } from "../../models/Action";
import Action from '../../models/Action';
import { StatusType } from "../../models/Status";

describe('[Helpers] Action type', () => {
    test('getActions() ', () => {
        const actionNode: ActionNode = {
            actionNodeType: ActionNodeType.LINK,
            link: ''
        };

        const action: Action = {
            actionNodeType: ActionNodeType.ACTION
        } as Action;

        const actionNodes: ActionNode[] = [actionNode, action];

        const resultAcitons = getActions(actionNodes);

        const excpectActions = [action];

        expect(resultAcitons).toEqual(excpectActions);
    });
    

    test('getStatusChipDescription() ', () => {
        

        const resultDescription = getStatusChipDescription(StatusType.PASSED);

        const expectedDescription = 'Passed actions count. Click to select related passed actions.'

        expect(resultDescription).toEqual(expectedDescription);
    })
    
})
