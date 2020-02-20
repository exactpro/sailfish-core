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

import * as React from 'react';
import UserTable, { isUserTable } from '../../models/UserTable';
import Action from '../../models/Action';
import { RecoverableExpandablePanel } from '../ExpandablePanel';
import { CustomTable } from './CustomTable';
import { keyForUserTable } from '../../helpers/keys';
import { stopPropagationHandler } from '../../helpers/react';

interface UserTableCardProps {
    table: UserTable;
    parent: Action;
}

function UserTableCard({ parent, table }: UserTableCardProps) {
    return (
        <div className="ac-body__table">
            <RecoverableExpandablePanel
                stateKey={keyForUserTable(table, parent)}>
                {toggleExpand => (
                    <div className="ac-body__item-title"
                        onClick={stopPropagationHandler(toggleExpand)}>{table.name || "Custom table"}</div>
                )}
                <CustomTable
                    content={table.content} />
            </RecoverableExpandablePanel>
        </div>
    )
}

export default UserTableCard;
