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
(function beforeLoad() {
    var parameters = {};
    var query = location.search.substring(1);
    var keyValuePairs = query.split(/=/);
    var value = keyValuePairs[1];
    document.write("<div id=" + keyValuePairs[0] + "><iframe id='reportFrame' name='report' src=" + value + "></iframe></div>");
})();

