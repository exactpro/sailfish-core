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
import Action from '../models/Action';
import { StatusType, statusValues } from '../models/Status';
import { Chip } from './Chip';
import { stopPropagationHandler } from '../helpers/react';

interface Props {
    actions: Action[];
    selectedStatus?: StatusType; 
    onStatusSelect: (status: StatusType) => void;
}

export default function ChipsList({ actions, onStatusSelect, selectedStatus }: Props) {
    return (
        <React.Fragment>
            {
                statusValues.map((status, index) => {
                    const count = actions.filter(action => action.status.status === status).length;

                    if (count < 1) {
                        return null;
                    }

                    return (
                        <Chip
                            key={index}
                            status={status}
                            text={count.toString()}
                            isSelected={status === selectedStatus}
                            onClick={stopPropagationHandler(onStatusSelect, status)}/>
                    )
                })
            }
        </React.Fragment>
    )
}
