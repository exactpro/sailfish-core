/******************************************************************************
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
 * limitations under the License.
 ******************************************************************************/

import * as React from 'react';
import Action from '../../models/Action';
import { StatusType } from '../../models/Status';
import "../../styles/messages.scss";
import { createStyleSelector } from '../../helpers/styleCreators';
import Message from '../../models/Message';
import { connect } from 'react-redux';
import AppState from '../../state/models/AppState';
import { selectMessage } from '../../actions/actionCreators';
import ChipsList from '../ChipsList';
import { Chip } from "../Chip";

type SelectHandler = (status: StatusType) => void;

interface OwnProp {
    message: Message;
}

interface StateProps {
    actions: Action[];
    selectedStatus?: StatusType;
}

interface DispatchProps {
    selectHandler: SelectHandler;
}

interface Props extends OwnProp, StateProps, DispatchProps {
}

function MessageCardActionChipsBase({ actions, selectedStatus, selectHandler }: Props) {

    return (
        <div className="mc-header__info">
            {
                actions.length ? (
                    <ChipsList
                        actions={actions}
                        selectedStatus={selectedStatus}
                        onStatusSelect={selectHandler}/>
                ) : (
                    <Chip text='0' title='No related actions'/>
                )
            }
        </div>
    )
}

export const MessageCardActionChips = connect(
    (state: AppState, ownProps: OwnProp): StateProps => ({
        actions: ownProps.message.relatedActions
            .reduce((actions, actionId) =>
                state.selected.actionsMap.get(actionId) ?
                    [...actions, state.selected.actionsMap.get(actionId)] :
                    actions, []),
        selectedStatus: state.selected.messagesId.includes(ownProps.message.id) ? state.selected.selectedActionStatus : null
    }),
    (dispatch, ownProps: OwnProp): DispatchProps => ({
        selectHandler: (status: StatusType) => dispatch(selectMessage(ownProps.message, status))
    })
)(MessageCardActionChipsBase);
