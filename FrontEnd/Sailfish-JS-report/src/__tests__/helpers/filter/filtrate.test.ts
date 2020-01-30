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

import filtrate from "../../../helpers/filter/filtrate";
import TestCase from "../../../models/TestCase";
import {
    createAction,
    createMessage,
    createTestCase,
    createVerification,
    createVerificationEntry
} from "../../util/creators";
import { keyForAction, keyForMessage, keyForVerification } from "../../../helpers/keys";
import { StatusType } from "../../../models/Status";
import FilterType from "../../../models/filter/FilterType";
import FilterPath from "../../../models/filter/FilterPath";
import { FilterBlock } from "../../../models/filter/FilterBlock";

describe('[Helpers] filtrate', () => {

    const testCaseBase: TestCase = createTestCase();

    test('Filter all in plain action', async () => {
        const blocks: FilterBlock[] = [{
            types: [FilterType.ACTION],
            path: FilterPath.ALL,
            values: ['test']
        }];

        const testCase: TestCase = {
            ...testCaseBase,
            actions: [
                createAction(0, [], 'test name')
            ]
        };

        const results = await filtrate(testCase, blocks);

        expect(results).toEqual([keyForAction(0)]);
    });

    test('Service filter in plain action', async () => {
        const blocks: FilterBlock[] = [{
            types: [FilterType.ACTION],
            path: FilterPath.SERVICE,
            values: ['test']
        }];

        const testCase: TestCase = {
            ...testCaseBase,
            actions: [{
                ...createAction(0),
                serviceName: 'some test service'
            }]
        };

        const results = await filtrate(testCase, blocks);

        expect(results).toEqual([keyForAction(0)]);
    });

    test('Filter actions by status', async () => {
        const blocks: FilterBlock[] = [{
            types: [FilterType.ACTION],
            path: FilterPath.STATUS,
            values: [StatusType.PASSED]
        }];

        const testCase: TestCase = {
            ...testCaseBase,
            actions: [{
                ...createAction(0),
                status: { status: StatusType.PASSED }
            }, {
                ...createAction(1),
                status: { status: StatusType.FAILED }
            }]
        };

        const results = await filtrate(testCase, blocks);

        expect(results).toEqual([keyForAction(0)]);
    });

    test("Filter all in action's subnodes", async () => {
        const blocks: FilterBlock[] = [{
            types: [FilterType.ACTION],
            path: FilterPath.ALL,
            values: ['test']
        }];

        const testCase: TestCase = {
            ...testCaseBase,
            actions: [{
                ...createAction(0),
                name: 'parent node',
                subNodes: [{
                    ...createAction(1),
                    name: 'test action'
                }]
            }]
        };

        const results = await filtrate(testCase, blocks);

        expect(results.sort()).toEqual([keyForAction(0), keyForAction(1)].sort());
    });

    test("Filter service in action's subnodes", async () => {
        const blocks: FilterBlock[] = [{
            types: [FilterType.ACTION],
            path: FilterPath.SERVICE,
            values: ['test']
        }];

        const testCase: TestCase = {
            ...testCaseBase,
            actions: [{
                ...createAction(0),
                name: 'parent node',
                subNodes: [{
                    ...createAction(1),
                    serviceName: 'some test service'
                }]
            }]
        };

        const results = await filtrate(testCase, blocks);

        expect(results.sort()).toEqual([keyForAction(0), keyForAction(1)].sort());
    });

    test('Filter verification by name', async () => {
        const blocks: FilterBlock[] = [{
            types: [FilterType.ACTION],
            path: FilterPath.ALL,
            values: ['test']
        }];

        const testCase: TestCase = {
            ...testCaseBase,
            actions: [{
                ...createAction(0),
                subNodes: [{
                    ...createVerification(1),
                    name: 'test name'
                }]
            }]
        };

        const results = await filtrate(testCase, blocks);

        expect(results.sort()).toEqual([keyForVerification(0, 1), keyForAction(0)].sort());
    });

    test('Filter verifications by some entry value', async () => {
        const blocks: FilterBlock[] = [{
            types: [FilterType.ACTION],
            path: FilterPath.ALL,
            values: ['test']
        }];

        const testCase: TestCase = {
            ...testCaseBase,
            actions: [{
                ...createAction(0),
                subNodes: [{
                    ...createVerification(1),
                    entries: [{
                        ...createVerificationEntry(),
                        name: 'some test name'
                    }]
                }]
            }]
        };

        const results = await filtrate(testCase, blocks);

        expect(results.sort()).toEqual([keyForVerification(0, 1), keyForAction(0)].sort());
    });

    test('Filter verification by some deep entry value', async () => {
        const blocks: FilterBlock[] = [{
            types: [FilterType.ACTION],
            path: FilterPath.ALL,
            values: ['test']
        }];

        const testCase: TestCase = {
            ...testCaseBase,
            actions: [{
                ...createAction(0),
                subNodes: [{
                    ...createVerification(1),
                    entries: [{
                        ...createVerificationEntry(),
                        subEntries: [{
                            ...createVerificationEntry(),
                            name: 'some test name'
                        }]
                    }]
                }]
            }]
        };

        const results = await filtrate(testCase, blocks);

        expect(results.sort()).toEqual([keyForVerification(0, 1), keyForAction(0)].sort());
    });

    test('Filter messages by name', async () => {
        const blocks: FilterBlock[] = [{
            types: [FilterType.MESSAGE],
            path: FilterPath.ALL,
            values: ['test']
        }];

        const testCase: TestCase = {
            ...testCaseBase,
            messages: [createMessage(0, 'some test name')]
        };

        const results = await filtrate(testCase, blocks);

        expect(results).toEqual([keyForMessage(0)]);
    });

    test('Filter both actions and messages by service', async () => {
        const blocks: FilterBlock[] = [{
            types: [FilterType.MESSAGE, FilterType.ACTION],
            path: FilterPath.SERVICE,
            values: ['test']
        }];

        const testCase: TestCase = {
            ...testCaseBase,
            messages: [{
                ...createMessage(0),
                from: 'test service'
            }],
            actions: [{
                ...createAction(1),
                serviceName: 'test service'
            }]
        };

        const results = await filtrate(testCase, blocks);

        expect(results.sort()).toEqual([keyForAction(1), keyForMessage(0)].sort());
    });

    test('Filter by several block values', async () => {
        const blocks: FilterBlock[] = [{
            types: [FilterType.ACTION],
            path: FilterPath.SERVICE,
            values: ['one', 'two']
        }];

        const testCase: TestCase = {
            ...testCaseBase,
            actions: [{
                ...createAction(1),
                serviceName: 'one service'
            }, {
                ...createAction(2),
                serviceName: 'two service'
            }, {
                ...createAction(3),
                serviceName: 'three service'
            }]
        };

        const results = await filtrate(testCase, blocks);

        expect(results.sort()).toEqual([keyForAction(1), keyForAction(2)].sort());
    });

    test('Filter action by service and status', async () => {
        const blocks: FilterBlock[] = [{
            types: [FilterType.ACTION],
            path: FilterPath.SERVICE,
            values: ['one']
        }, {
            types: [FilterType.ACTION],
            path: FilterPath.STATUS,
            values: [StatusType.FAILED]
        }];

        const testCase: TestCase = {
            ...testCaseBase,
            actions: [{
                ...createAction(1),
                serviceName: 'one service',
                status: { status: StatusType.PASSED }
            }, {
                ...createAction(2),
                serviceName: 'one service',
                status: { status: StatusType.FAILED }
            }, {
                ...createAction(3),
                serviceName: 'two service',
                status: { status: StatusType.FAILED }
            }]
        };

        const results = await filtrate(testCase, blocks);

        expect(results).toEqual([keyForAction(2)]);
    });

    test('Filter action with nested verifications', async () => {
        const blocks: FilterBlock[] = [{
            types: [FilterType.ACTION],
            path: FilterPath.ALL,
            values: ['test']
        }];

        const testCase: TestCase = {
            ...testCaseBase,
            actions: [{
                ...createAction(1),
                subNodes: [createVerification(2, 'test verification')]
            }, {
                ...createAction(3),
                name: 'test action'
            }]
        };

        const results = await filtrate(testCase, blocks);

        expect(results.sort()).toEqual([keyForVerification(1, 2), keyForAction(3), keyForAction(1)].sort());
    });

    test('Filter action by some input param value', async () => {
        const blocks: FilterBlock[] = [{
            types: [FilterType.ACTION],
            path: FilterPath.ALL,
            values: ['test']
        }];

        const testCase: TestCase = {
            ...testCaseBase,
            actions: [{
                ...createAction(1),
                parameters: [{
                    name: 'param',
                    subParameters: [{
                        name: 'param2',
                        value: 'test'
                    }]
                }]
            }]
        };

        const results = await filtrate(testCase, blocks);

        expect(results).toEqual([keyForAction(1)]);
    })
});
