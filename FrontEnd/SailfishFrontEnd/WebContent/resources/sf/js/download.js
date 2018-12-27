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
function onSortComplete() {
    var container = $('#folderContent').find('tbody');
    var rows = container.find('tr.ui-widget-content');
    var upRow = null;

    $(rows).each(function(index, row) {
        if($(row).attr('data-rk') === '..') {
            upRow = row;
        } else if($(row).hasClass('directory-row')) {
            pushTop(row, container);
        }
    });

    if(upRow !== null) {
        pushTop(upRow, container);
    }
}

function pushTop(row, container) {
    $($(row).detach()).prependTo($(container));
}