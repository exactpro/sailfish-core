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
// support async/await in runtime:
import "regenerator-runtime/runtime";

import FakeWindow from "../mock/FakeWindow.js";
import TheMatrixList from 'state/TheMatrixList.js';
import TheAppState from 'state/TheAppState.js';
import * as c from 'consts';

import TheEditor from 'actions/TheEditor.js';

const startValuex = TheMatrixList["0"].get("6");

const restoreStartValues = () => {
    TheMatrixList[0].select("6").set(startValuex);
    TheAppState.select('editor', 'dictionary', 'Example').set({loaded: false});
};

    /*
    * edit
    * editAndAddRefsIf
    * add
    * start
    * stop
    * remove
    * insert
    * move
    * replace
    * closePanel
    * addPanel
    * checkoutPanel
    */

describe ('copy fields tests', function() {

    const copyInfo = {
        sourcePath: "6" + c.PATH_SEP + "items" + c.PATH_SEP + 0,
        destinationPath: "6" + c.PATH_SEP + "items" + c.PATH_SEP + 1
    };

    it('copyFieldValues without dictionary and reference', (done) => {
        const copyCursor = TheAppState.select('panels', 'dataSources', 'items', "0", 'copyFields');
        expect(copyCursor.get('sourcePath')).toBeUndefined();
        expect(copyCursor.get('destinationPath')).toBeUndefined();
        let sourceAction = {
            errors:[],
            values: {
                "#id": "122",
                "#action": "send",
                "Field": "qwerty"
            }
        };
        let destinationAction1 = {
            errors:[],
            values: {
                "#id": "122",
                "#action": "receive"
            }
        };
        let destinationAction2 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "Field": null
            }
        };
        let res1 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "Field": "qwerty"
            }
        };
        let destinationAction3 = {
            errors:[],
            values: {
                "#id": "122",
                "#action": "receive",
                "Field": undefined,
                "Field1": "qwerty1"
            }
        };
        let res2 = {
            errors:[],
            values: {
                "#id": "122",
                "#action": "receive",
                "Field": "qwerty",
                "Field1": "qwerty1"
            }
        };

        copyCursor.set(copyInfo);
        TheMatrixList[0].select('data', "6", 'items').set([sourceAction, destinationAction1]);
        TheEditor.copyFieldValues("0");
        setTimeout(() => {
            expect(copyCursor.get('sourcePath')).toBeUndefined();
            expect(copyCursor.get('destinationPath')).toBeUndefined();
            expect(TheMatrixList[0].get('data', "6", 'items', "1")).toEqual(destinationAction1);
            copyCursor.set(copyInfo);
            TheMatrixList[0].select('data', "6", 'items').set([sourceAction, destinationAction2]);
            TheEditor.copyFieldValues("0");
            setTimeout(() => {
                expect(TheMatrixList[0].get('data', "6", 'items', "1")).toEqual(res1);
                copyCursor.set(copyInfo);
                TheMatrixList[0].select('data', "6", 'items').set([sourceAction, destinationAction3]);
                TheEditor.copyFieldValues("0");
                setTimeout(() => {
                    expect(TheMatrixList[0].get('data', "6", 'items', "1")).toEqual(res2);
                    done();
                }, 10);
            }, 10);
        }, 10);
    });

    it('copyFieldValues without dictionary, but with reference', (done) => {
        const copyCursor = TheAppState.select('panels', 'dataSources', 'items', "0", 'copyFields');
        expect(copyCursor.get('sourcePath')).toBeUndefined();
        expect(copyCursor.get('destinationPath')).toBeUndefined();
        let sourceAction = {
            errors:[],
            values: {
                "#id": "122",
                "#action": "send",
                "Field": "qwerty",
                "#reference": "ref1"
            }
        };
        let destinationAction1 = {
            errors:[],
            values: {
                "#id": "122",
                "#action": "receive"
            }
        };
        let destinationAction2 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "Field": null
            }
        };
        let res1 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "Field": "${ref1.Field}"
            }
        };
        let destinationAction3 = {
            errors:[],
            values: {
                "#id": "122",
                "#action": "receive",
                "Field": undefined,
                "Field1": "qwerty1"
            }
        };
        let res2 = {
            errors:[],
            values: {
                "#id": "122",
                "#action": "receive",
                "Field": "${ref1.Field}",
                "Field1": "qwerty1"
            }
        };

        copyCursor.set(copyInfo);
        TheMatrixList[0].select('data', "6", 'items').set([sourceAction, destinationAction1]);
        TheEditor.copyFieldValues("0");
        setTimeout(() => {
            expect(copyCursor.get('sourcePath')).toBeUndefined();
            expect(copyCursor.get('destinationPath')).toBeUndefined();
            expect(TheMatrixList[0].get('data', "6", 'items', "1")).toEqual(destinationAction1);
            copyCursor.set(copyInfo);
            TheMatrixList[0].select('data', "6", 'items').set([sourceAction, destinationAction2]);
            TheEditor.copyFieldValues("0");
            setTimeout(() => {
                expect(TheMatrixList[0].get('data', "6", 'items', "1")).toEqual(res1);
                copyCursor.set(copyInfo);
                TheMatrixList[0].select('data', "6", 'items').set([sourceAction, destinationAction3]);
                TheEditor.copyFieldValues("0");
                setTimeout(() => {
                    expect(TheMatrixList[0].get('data', "6", 'items', "1")).toEqual(res2);
                    done();
                }, 10);
            }, 10);
        }, 10);
    });

    it('copyFieldValues: with dictionary and message_type, without reference', (done) => {
        const fakeDictionaryName = "Example";   //existing dictionary, not loaded
        const fakeDict = {
            name: fakeDictionaryName,
            namespace: [fakeDictionaryName],
            loaded: true,
            messages: {
                "GreatMessage": {
                    name: "GreatMessage",
                    namespace: fakeDictionaryName,
                    fields: {
                        Account: {
                            coll: false,
                            idx: 4,
                            name: "Account",
                            req: false,
                            type: "STRING"
                        },
                        Side: {
                            coll: false,
                            idx: 12,
                            name: "Side",
                            req: true,
                            type: "CHAR"
                        }
                    }
                }
            }
        }
        TheAppState.select('editor', 'dict', fakeDictionaryName).set(fakeDict);
        const copyCursor = TheAppState.select('panels', 'dataSources', 'items', "0", 'copyFields');
        let src1 = {
            errors:[],
            values: {
                "#id": "122",
                "#action": "send",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": "qwerty",
                "Account": "ASD",
                "Side": "B"
            }
        };
        let dst1 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example"
            }
        };
        let res1 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Side": "B"
            }
        };
        let dst2 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": null
            }
        };
        let res2 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": "qwerty",
                "Side": "B"
            }
        };
        let dst3 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": null,
                "Account": null
            }
        };
        let res3 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": "qwerty",
                "Side": "B",
                "Account": "ASD"
            }
        };
        copyCursor.set(copyInfo);
        TheMatrixList[0].select('data', "6", 'items').set([src1, dst1]);
        TheEditor.copyFieldValues("0");
        setTimeout(() => {
            expect(TheMatrixList[0].get('data', "6", 'items', "1")).toEqual(res1);
            copyCursor.set(copyInfo);
            TheMatrixList[0].select('data', "6", 'items').set([src1, dst2]);
            TheEditor.copyFieldValues("0");
            setTimeout(() => {
                expect(TheMatrixList[0].get('data', "6", 'items', "1")).toEqual(res2);
                copyCursor.set(copyInfo);
                TheMatrixList[0].select('data', "6", 'items').set([src1, dst3]);
                TheEditor.copyFieldValues("0");
                setTimeout(() => {
                    expect(TheMatrixList[0].get('data', "6", 'items', "1")).toEqual(res3);
                    done();
                }, 10);
            }, 10);
        }, 10);
    });

    it('copyFieldValues: with dictionary, message_type and reference', (done) => {
        const fakeDictionaryName = "Example";   //existing dictionary, not loaded
        const fakeDict = {
            name: fakeDictionaryName,
            namespace: [fakeDictionaryName],
            loaded: true,
            messages: {
                "GreatMessage": {
                    name: "GreatMessage",
                    namespace: fakeDictionaryName,
                    fields: {
                        Account: {
                            coll: false,
                            idx: 4,
                            name: "Account",
                            req: false,
                            type: "STRING"
                        },
                        Side: {
                            coll: false,
                            idx: 12,
                            name: "Side",
                            req: true,
                            type: "CHAR"
                        }
                    }
                }
            }
        }
        TheAppState.select('editor', 'dict', fakeDictionaryName).set(fakeDict);
        const copyCursor = TheAppState.select('panels', 'dataSources', 'items', "0", 'copyFields');
        let src1 = {
            errors:[],
            values: {
                "#id": "122",
                "#action": "send",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": "qwerty",
                "Account": "ASD",
                "Side": "B",
                "#reference": "ref1"
            }
        };
        let dst1 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example"
            }
        };
        let res1 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Side": "${ref1.Side}"
            }
        };
        let dst2 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": null
            }
        };
        let res2 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": "${ref1.Field}",
                "Side": "${ref1.Side}"
            }
        };
        let dst3 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": null,
                "Account": null
            }
        };
        let res3 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": "${ref1.Field}",
                "Side": "${ref1.Side}",
                "Account": "${ref1.Account}"
            }
        };
        copyCursor.set(copyInfo);
        TheMatrixList[0].select('data', "6", 'items').set([src1, dst1]);
        TheEditor.copyFieldValues("0");
        setTimeout(() => {
            expect(TheMatrixList[0].get('data', "6", 'items', "1")).toEqual(res1);
            copyCursor.set(copyInfo);
            TheMatrixList[0].select('data', "6", 'items').set([src1, dst2]);
            TheEditor.copyFieldValues("0");
            setTimeout(() => {
                expect(TheMatrixList[0].get('data', "6", 'items', "1")).toEqual(res2);
                copyCursor.set(copyInfo);
                TheMatrixList[0].select('data', "6", 'items').set([src1, dst3]);
                TheEditor.copyFieldValues("0");
                setTimeout(() => {
                    expect(TheMatrixList[0].get('data', "6", 'items', "1")).toEqual(res3);
                    done();
                }, 10);
            }, 10);
        }, 10);
    });

    it('copyFieldValues (any value): with dictionary, message_type and reference', (done) => {
        const fakeDictionaryName = "Example";   //existing dictionary, not loaded
        const fakeDict = {
            name: fakeDictionaryName,
            namespace: [fakeDictionaryName],
            loaded: true,
            messages: {
                "GreatMessage": {
                    name: "GreatMessage",
                    namespace: fakeDictionaryName,
                    fields: {
                        Account: {
                            coll: false,
                            idx: 4,
                            name: "Account",
                            req: false,
                            type: "STRING"
                        },
                        Side: {
                            coll: false,
                            idx: 12,
                            name: "Side",
                            req: true,
                            type: "CHAR"
                        }
                    }
                }
            }
        }
        TheAppState.select('editor', 'dict', fakeDictionaryName).set(fakeDict);
        const copyCursor = TheAppState.select('panels', 'dataSources', 'items', "0", 'copyFields');
        let src1 = {
            errors:[],
            values: {
                "#id": "122",
                "#action": "send",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": "qwerty",
                "Account": "ASD",
                "Side": "B",
                "#reference": "ref1"
            }
        };
        let dst2 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": "*"
            }
        };
        let res2 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": "*",
                "Side": "${ref1.Side}"
            }
        };
        let dst3 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": null,
                "Account": "*"
            }
        };
        let res3 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": "${ref1.Field}",
                "Side": "${ref1.Side}",
                "Account": "*"
            }
        };
        copyCursor.set(copyInfo);
        TheMatrixList[0].select('data', "6", 'items').set([src1, dst2]);
        TheEditor.copyFieldValues("0");
        setTimeout(() => {
            expect(TheMatrixList[0].get('data', "6", 'items', "1")).toEqual(res2);
            copyCursor.set(copyInfo);
            TheMatrixList[0].select('data', "6", 'items').set([src1, dst3]);
            TheEditor.copyFieldValues("0");
            setTimeout(() => {
                expect(TheMatrixList[0].get('data', "6", 'items', "1")).toEqual(res3);
                done();
            }, 10);
        }, 10);
    });

    it('copyFieldValues (with submessage): with dictionary, message_type and reference', (done) => {
        const fakeDictionaryName = "Example";   //existing dictionary, not loaded
        const fakeDict = {
            name: fakeDictionaryName,
            namespace: [fakeDictionaryName],
            loaded: true,
            messages: {
                "GreatMessage": {
                    name: "GreatMessage",
                    namespace: fakeDictionaryName,
                    fields: {
                        Account: {
                            coll: false,
                            idx: 4,
                            name: "Account",
                            req: false,
                            type: "STRING"
                        },
                        Side: {
                            coll: false,
                            idx: 12,
                            name: "Side",
                            req: true,
                            type: "CHAR"
                        },
                        TargetPartyID: {
                            coll: false,
                            idx: 12,
                            name: "Side",
                            req: true,
                            type: "SUBMESSAGE"
                        }
                    }
                }
            }
        }
        TheAppState.select('editor', 'dict', fakeDictionaryName).set(fakeDict);
        const copyCursor = TheAppState.select('panels', 'dataSources', 'items', "0", 'copyFields');
        let src1 = {
            errors:[],
            values: {
                "#id": "122",
                "#action": "send",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": "qwerty",
                "Account": "ASD",
                "Side": "B",
                "TargetPartyID": "[TP_ID_1]",
                "#reference": "ref1"
            }
        };
        let dst2 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": "*",
                "TargetPartyID": "[TP_ID_2]"
            }
        };
        let res2 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": "*",
                "Side": "${ref1.Side}",
                "TargetPartyID": "[TP_ID_2]"
            }
        };
        let dst3 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": null,
                "Account": "*"
            }
        };
        let res3 = {
            errors:[],
            values: {
                "#id": "123",
                "#action": "receive",
                "#message_type": "GreatMessage",
                "#dictionary": "Example",
                "Field": "${ref1.Field}",
                "Side": "${ref1.Side}",
                "Account": "*",
                "TargetPartyID": undefined
            }
        };
        copyCursor.set(copyInfo);
        TheMatrixList[0].select('data', "6", 'items').set([src1, dst2]);
        TheEditor.copyFieldValues("0");
        setTimeout(() => {
            expect(TheMatrixList[0].get('data', "6", 'items', "1")).toEqual(res2);
            copyCursor.set(copyInfo);
            TheMatrixList[0].select('data', "6", 'items').set([src1, dst3]);
            TheEditor.copyFieldValues("0");
            setTimeout(() => {
                expect(TheMatrixList[0].get('data', "6", 'items', "1")).toEqual(res3);
                restoreStartValues();
                done();
            }, 10);
        }, 10);
    });
});
