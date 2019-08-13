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
import Verification from '../../models/Verification';
import {StatusType} from "../../models/Status";
import {createSelector} from '../../helpers/styleCreators';
import {SearchExpandablePanel} from "../ExpandablePanel";
import {VerificationTable} from "./VerificationTable";
import '../../styles/action.scss';
import {keyForVerification} from '../../helpers/keys';
import SearchableContent from '../search/SearchableContent';
import {VerificationMlUploadButton} from "../machinelearning/VerificationMlUploadButton";

interface VerificationCardProps {
    verification: Verification;
    isSelected: boolean;
    isTransparent: boolean;
    parentActionId: number;
    onSelect: (messageId: number, rootActionId: number, status: StatusType) => any;
    onExpand: () => void;
}

const VerificationCard = ({ verification, onSelect, isSelected, isTransparent, parentActionId, onExpand }: VerificationCardProps) => {

    const { status, messageId, entries, name } = verification;

    const className = createSelector(
        "ac-body__verification",
        status && status.status,
        isSelected ? "selected" : null,
        isTransparent && !isSelected ? "transparent" : null
    );

    const key = keyForVerification(parentActionId, messageId);

    return (
        <div className="action-card">
            <div className={className}
                onClick={e => {
                    onSelect(messageId, parentActionId, status.status);
                    
                    // here we cancel handling by parent divs
                    e.stopPropagation();
                }}>
                <SearchExpandablePanel
                    searchKeyPrefix={key}
                    stateKey={key}
                    onExpand={onExpand}>
                    <div className="ac-body__verification-title-wrapper">
                        <div className="ac-body__verification-title">
                            <span>{"Verification — "}</span>
                            <SearchableContent
                                contentKey={`${key}-name`}
                                content={name}/>
                            <span>{" — " + status.status}</span>
                        </div>
                        <VerificationMlUploadButton targetActionId={parentActionId} targetMessageId={messageId}/>
                    </div>
                    <VerificationTable
                        keyPrefix={key}
                        actionId={parentActionId}
                        messageId={messageId}
                        stateKey={key + '-nodes'}
                        params={entries}
                        status={status.status}
                        onExpand={onExpand}/>
                </SearchExpandablePanel>
            </div>
        </div>
    )
};

export default VerificationCard;
