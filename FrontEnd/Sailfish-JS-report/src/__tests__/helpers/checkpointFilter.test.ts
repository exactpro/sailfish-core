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

import { createAction } from "../util/creators";
import { getCheckpointActions } from "../../helpers/checkpointFilter";

describe('[Helpers] checkpointFilter', () => {
    test('getCheckpointActions(): no checkpoints list', () => {
        const firstAction = createAction(1, [], 'test'),
            secondAction = createAction(2, [], 'test2'),
            actions = [firstAction, secondAction];

        const resultCheckpoints = getCheckpointActions(actions);

        expect(resultCheckpoints).toEqual([]);
    })
    
    test('getCheckpointActions(): flat checkpoint list', () => {
        const firstAction = createAction(1, [], 'test1'),
            secondCheckpoint = createAction(2, [], '2_GetCheckPoint', [{ name: 'Checkpoint' }]),
            thirdCheckpoint = createAction(3, [], '3_GetCheckPoint', [{ name: 'Checkpoint' }]),
            actions = [firstAction, secondCheckpoint, thirdCheckpoint];

        const resultCheckpoints = getCheckpointActions(actions);

        const expectedCheckpoints = [secondCheckpoint, thirdCheckpoint];

        expect(resultCheckpoints).toEqual(expectedCheckpoints);
    })
    
    test('getCheckpointActions(): checkpoint in tree', () => {
        const checkpoint = createAction(0, [], 'GetCheckPoint', [{ name: 'Checkpoint' }]),
            secondAction = createAction(2, [checkpoint], 'test'),
            firstAction = createAction(1, [secondAction], 'test'),
            actions = [firstAction];

        const resultCheckpoitns = getCheckpointActions(actions);

        const expectedCheckpoints = [checkpoint];

        expect(resultCheckpoitns).toEqual(expectedCheckpoints);
    })
    
})
