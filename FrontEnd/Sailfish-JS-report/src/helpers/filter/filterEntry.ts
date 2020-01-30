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

import Action from "../../models/Action";
import Message from "../../models/Message";
import Verification from "../../models/Verification";
import VerificationEntry from "../../models/VerificationEntry";
import ActionParameter from "../../models/ActionParameter";

type Entry = Action | Message | Verification | VerificationEntry | ActionParameter;

export default function filterEntry<T extends Entry>(entry: T, fields: (keyof T)[], values: RegExp[]): boolean {
    for (let field of fields) {
        const targetField = entry[field];

        if (typeof targetField != 'string') {
            continue;
        }

        for (let value of values) {
            if (targetField.match(value)) {
                return true;
            }
        }
    }

    return false;
}
