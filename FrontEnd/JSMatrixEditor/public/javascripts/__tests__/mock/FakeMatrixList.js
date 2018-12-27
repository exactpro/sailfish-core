/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
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
import Baobab from 'baobab';
import storeFactory from 'state/storeFactory.js';

export const TheMatrixList = {
    '0': new Baobab(
        {
            data: [
                    {
                        "values": {                         //Autocomplete-spec.js
                            "#execute": "true",
                            "#reference": "Test_Case",
                            "#description": "TC",
                            "#action": "Test Case Start"
                        },
                        "items": [
                            {
                                "values": {
                                    "#execute": "y",
                                    "#dictionary": "TestAML",
                                    "#service_name": "fake",
                                    "#action": "receive",
                                    "#message_type": "Instrument",
                                    "#reference": "ref1"
                                },
                                "metadata": {
                                    "dictionary": "TestAML"
                                },
                                "errors": []
                            },
                            {
                                "values": {
                                    "#execute": "y",
                                    "#dictionary": "TestAML",
                                    "#service_name": "fake",
                                    "#action": "receive",
                                    "#message_type": "Instrument",
                                    "#reference": "ref2"
                                },
                                "metadata": {
                                    "dictionary": "TestAML"
                                },
                                "errors": []
                            },
                            {
                                "values": {
                                    "#execute": "y",
                                    "#dictionary": "TestAML",
                                    "#service_name": "fake",
                                    "#action": "receive",
                                    "#message_type": "SnapshotComplete",
                                    "#reference": "ref3",
                                },
                                "metadata": {
                                    "dictionary": "TestAML"
                                },
                                "errors": []
                            }
                        ],
                        "errors": []
                    }, {
                        "items": [],
                        "values": {},
                        "errors": [],
                        "historyValue": 1
                    }, {
                        "items": [],
                        "values": {},
                        "errors": [],
                        "historyValue": 1
                    }, {
                        "items": [{
                            "values": {},
                            "errors": []
                        }],
                        "values": {},
                        "errors": [],
                        "historyValue": 1
                    }, {
                        "items": [],            //History-spec.js
                        "values": {},
                        "errors": [],
                        "historyValue": {
                            a: 1,
                            b: 1,
                            c: 1
                        }
                    }, {
                        "items": [{
                            "values": {
                                "#action": "Sleep",
                                "#timeout": "5000",
                                "#description": "Case_21"
                            },
                            "errors": []
                        }, {
                            "values": {
                                "#action": "Sleep",
                                "#timeout": "6000",
                                "#description": "another"
                            },
                            "errors": []
                        }, {
                            "values": {
                                "#action": "Sleep",
                                "#timeout": "6000",
                                "#description": "anotherCase"
                            },
                            "errors": []
                        }],
                        "values": {
                            "#action": "Global Block start",
                            "#description": "",
                            "#execute": "",
                            "#fail_on_unexpected_message": "n",
                            "#id": "",
                            "#reference": "Global_Block_1"
                        },
                        "errors": []
                    }, {
                        "values": {
                            "#execute": "true",
                            "#reference": "Test_6",
                            "#description": "TC",
                            "#action": "TestStart"
                        },
                        "items": [],
                        "errors": []
                    }
                ],
            isHistoryBlocked: false
        },
        {
            syncwrite: true,  // Applying modifications immediately
            asynchronous: true, // commit on next tick
            storeId: "0"
        }
    )
};

Object.defineProperty(TheMatrixList["0"], 'isLoaded', {
    configurable: false,
    get: function() {
        return this.get('data') !== undefined;
    }
});

Object.defineProperty(TheMatrixList["0"], 'isLoadingInProgress', {
    configurable: false,
    get: function() {
        return this.get('data') === null;
    },
    set: function(value) {
        if (value === true) {
            this.set('data', null);
        }
    }
});

export default TheMatrixList;

window.TheMatrixList = TheMatrixList;
