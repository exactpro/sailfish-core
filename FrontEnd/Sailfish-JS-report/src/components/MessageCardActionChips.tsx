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

import { h } from 'preact';
import { Chip } from './Chip';
import Action from '../models/Action';
import { StatusType, statusValues } from '../models/Status';
import "../styles/messages.scss";

interface ActionChipsProps {
    actions: Action[];
    selectedStatus?: StatusType;
}

export const MessageCardActionChips = ({ actions, selectedStatus }: ActionChipsProps) => {
    return (
        <div class="message-card-header-action">
            {
                statusValues.map(status => renderChip(
                    status, 
                    actions.filter(action => action.status.status == status), 
                    selectedStatus)
                )
            }
        </div>
    )
}

function renderChip(status: StatusType, statusActions: Action[], selectedStatus: StatusType): JSX.Element {

    if (!statusActions || statusActions.length == 0) {
        return null;
    }

    return (
        <Chip
            count={statusActions.length}
            status={status}
            isSelected={status == selectedStatus} />
    )
}
