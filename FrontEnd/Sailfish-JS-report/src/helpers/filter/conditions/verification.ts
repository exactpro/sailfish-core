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

import filterEntry from "../filterEntry";
import { VERIFICATION_FIELDS, VERIFICATION_NODE_FIELDS } from "../../search/searchEngine";
import Verification from "../../../models/Verification";
import FilterCondition from "./FilterCondition";
import VerificationEntry from "../../../models/VerificationEntry";
import {toRegExpArray} from "../../regexp";
import FilterPath from "../../../models/filter/FilterPath";

const STUB_FUNCTION = () => false;

export default function getVerificationCondition(path: FilterPath, values: string[]): FilterCondition<Verification> {
    switch (path) {
        case FilterPath.SERVICE:
            return STUB_FUNCTION;

        case FilterPath.STATUS:
            return verification => values.includes(verification.status.status);

        case FilterPath.ALL:
            return verification => 
                filterEntry(verification, VERIFICATION_FIELDS, toRegExpArray(values)) ||
                verification.entries.some(entry => filterVerificationsEntry(entry, toRegExpArray(values)));

        default:
            return STUB_FUNCTION;
    }
}

function filterVerificationsEntry(entry: VerificationEntry, values: RegExp[]): boolean {
    if (filterEntry(entry, VERIFICATION_NODE_FIELDS, values)) {
        return true;
    }

    return entry.subEntries?.some(entry => filterVerificationsEntry(entry, values)) ?? false;
}
