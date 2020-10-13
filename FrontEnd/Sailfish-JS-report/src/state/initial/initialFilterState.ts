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

import FilterState from '../models/FiltersState';
import FilterType from "../../models/filter/FilterType";
import FilterPath from '../../models/filter/FilterPath';
import { FilterBlock } from '../../models/filter/FilterBlock';

const initialFilterState : FilterState = {
    results: [],
    blocks: [{
        types: [FilterType.MESSAGE],
        path: FilterPath.NAME,
        values: [],
        isSimpleFilter: true,
    }, {
        types: [FilterType.MESSAGE],
        path: FilterPath.SERVICE,
        values: [],
        isSimpleFilter: true,
    }, {
        types: [FilterType.MESSAGE],
        path: FilterPath.CONTENT,
        values: [],
        isSimpleFilter: true,
    }],
    isTransparent: false,
    isHighlighted: true
};

export default initialFilterState;
