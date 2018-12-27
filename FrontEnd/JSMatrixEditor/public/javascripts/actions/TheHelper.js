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

import pathHelpers from './helperPath.js';
import notificationHelpers from './helperNotification.js';
import dataHelpers from './helperData.js';
import NodeHelper from './helperNode.js'; // node helper needs initialization in constructor

import {stripText, getNextRefName} from 'utils';

class Helper extends NodeHelper {
    constructor(config) {
        const union = Object.assign({},
            pathHelpers,
            notificationHelpers,
            dataHelpers,
            config
        );
        super(union);
        this.getNextRefName = getNextRefName;
        this.stripText = stripText;
    }
}

export const TheHelper = new Helper();
export default TheHelper;
