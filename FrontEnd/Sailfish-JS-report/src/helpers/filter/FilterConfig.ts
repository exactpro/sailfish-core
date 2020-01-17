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

export const enum FilterType {
    ACTION = 'ACTION',
    MESSAGE = 'MESSAGE',
    VERIFICATION = 'VERIFICATION'
}

export const enum FilterPath {
    ALL = 'ALL',
    SERVICE = 'SERVICE',
    STATUS = 'STATUS'
}

export const FILTER_PATH_VALUES: FilterPath[] = [FilterPath.ALL, FilterPath.STATUS, FilterPath.SERVICE];

export interface FilterBlock {
    path: FilterPath;
    values: string[];
}

export interface FilterConfig {
    types: FilterType[];
    blocks: FilterBlock[];
    isTransparent: boolean;
}
