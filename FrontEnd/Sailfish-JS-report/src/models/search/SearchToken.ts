/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
 *  limitations under the License.
 ******************************************************************************/

import Panel from "../../util/Panel";

export default interface SearchToken {
    pattern: string;
    color: string;
    isActive: boolean;
    /**
     * If true, search results produced by this token will not be scrolled
     */
    isScrollable: boolean;
}

export interface PanelSearchToken extends SearchToken{
    panels: Panel[];
}
