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
"use strict";
var sfQueryStorage = {};
(function () {
    var sfHqlQueries = [];
    var localStorageAvaible;
    $(document).ready(function() {
        $('#builder-basic').queryBuilder({
            plugins: ['sortable', 'bt-tooltip-errors'],
            filters: [{
                id: 'msg_timestamp',
                label: 'Timestamp',
                type: 'date'
            }, {
                id: 'from_service',
                label: 'From',
                type: 'string'
            }, {
                id: 'to_service',
                label: 'To',
                type: 'string'
            }, {
                id: 'is_admin',
                label: 'Admin',
                type: 'boolean'
            }, {
                id: 'msg_namespace',
                label: 'Namespace',
                type: 'string'
            }, {
                id: 'msg_name',
                label: 'Name',
                type: 'string'
            }, {
                id: 'human_msg',
                label: 'HumanMessage',
                type: 'string'
            }, {
                id: 'raw_msg',
                label: 'RawMessage',
                type: 'string'
            }
            ]
        });
        storageAvailable();
        loadHqlQueries();
        sfQueryStorage.onWizardRetrieve = onWizardRetrieve;
        sfQueryStorage.saveHqlQuery = saveHqlQuery;
    });
    function onWizardRetrieve() {
        var builder = $('#builder-basic').queryBuilder('getSQL', false);
        var sqlQuery = builder.sql.replace(/(\r\n|\n|\r)/gm, ' ');
        $('#whereInput_input').val(sqlQuery);
        saveHqlQuery();
    };

    function saveHqlQuery() {
        if (localStorageAvaible) {
            var query = $('#whereInput_input').val();
            if (query) { //not empty
                var index = sfHqlQueries.indexOf(query);
                if (index == -1) { // it's a new query
                    index = 9; //remove last element
                }
                sfHqlQueries.splice(index, 1);
                sfHqlQueries.unshift(query);
                window.localStorage.setItem("SF_hql_query", JSON.stringify(sfHqlQueries));
                $('#hiddenQuery').val(sfHqlQueries.join(";"));
            }
        }
    }
    function loadHqlQueries() {
        if(localStorageAvaible){
            var json = window.localStorage.getItem("SF_hql_query");
            if(json){
                sfHqlQueries = JSON.parse(json);
                $('#hiddenQuery').val(sfHqlQueries.join(";"));
            }
        }
    }
    function storageAvailable() {
        try {
            var storage = window.localStorage,
                x = '__storage_test__';
            storage.setItem(x, x);
            storage.removeItem(x);
            localStorageAvaible = true;
        }
        catch(e) {
            localStorageAvaible = false;
        }
    };
})();
