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

import * as React from 'react';
import { connect } from "react-redux";
import AppState from "../../state/models/AppState";
import RejectedMessagesCarousel from "../RejectedMessagesCarousel";
import { getAdminMessages, getRejectedMessages } from "../../selectors/messages";
import { getIsPredictionsAvailable } from "../../selectors/machinelearning";
import {
    selectRejectedMessageId,
    setAdminMsgEnabled,
    togglePredictions,
    uglifyAllMessages
} from "../../actions/actionCreators";
import { createTriStateControlClassName } from "../../helpers/styleCreators";

interface OwnProps {
    showTitles: boolean;
}

interface StateProps {
    adminControlEnabled: boolean;
    selectedRejectedMessageId: number;
    adminMessagesEnabled: boolean;
    rejectedControlEnabled: boolean;
    predictionsEnabled: boolean;
    predictionsAvailable: boolean;
    hasKnownBugs: boolean;
    beautifiedMessagesEnabled: boolean;
}

interface DispatchProps {
    selectRejectedMessage: (messageId: number) => void;
    adminEnabledHandler: (adminEnabled: boolean) => void;
    togglePredictions: () => void;
    uglifyAllHandler: () => void;
}

interface Props extends StateProps, DispatchProps, OwnProps {
}

function MessagePanelControlsBase(props: Props) {

    const onPredictionClick = () => {
        if (props.predictionsAvailable) {
            props.togglePredictions();
        }
    };

    const adminRootClass = createTriStateControlClassName(
            "layout-control",
            props.adminMessagesEnabled,
            props.adminControlEnabled
        ),
        adminIconClass = createTriStateControlClassName(
            "layout-control__icon admin",
            props.adminMessagesEnabled,
            props.adminControlEnabled
        ),
        adminTitleClass = createTriStateControlClassName(
            "layout-control__title selectable",
            props.adminMessagesEnabled,
            props.adminControlEnabled
        ),
        rejectedRootClass = createTriStateControlClassName(
            "layout-control",
            true,
            props.rejectedControlEnabled
        ),
        rejectedIconClass = createTriStateControlClassName(
            "layout-control__icon rejected",
            true,
            props.rejectedControlEnabled
        ),
        rejectedTitleClass = createTriStateControlClassName(
            "layout-control__title",
            true,
            props.rejectedControlEnabled
        ),
        predictionRootClass = createTriStateControlClassName(
            "layout-control",
            props.predictionsEnabled,
            props.predictionsAvailable
        ),
        predictionIconClass = createTriStateControlClassName(
            "layout-control__icon prediction",
            props.predictionsEnabled,
            props.predictionsAvailable
        ),
        predictionTitleClass = createTriStateControlClassName(
            "layout-control__title prediction selectable",
            props.predictionsEnabled,
            props.predictionsAvailable
        );

    return (
        <React.Fragment>
            {
                props.beautifiedMessagesEnabled ? (
                    <div className="layout-control"
                         title="Back to plain text"
                         onClick={props.uglifyAllHandler}>
                        <div className="layout-control__icon beautifier"/>
                    </div>
                ) : null
            }
            <div className={adminRootClass}
                 onClick={() => props.adminControlEnabled && props.adminEnabledHandler(!props.adminMessagesEnabled)}
                 title={(props.adminMessagesEnabled ? "Hide" : "Show") + " Admin messages"}>
                <div className={adminIconClass}/>
                {
                    props.showTitles ?
                        <div className={adminTitleClass}>
                            <p>{props.adminControlEnabled ? "" : "No"} Admin Messages</p>
                        </div> :
                        null
                }
            </div>
            <div className={rejectedRootClass}>
                <div className={rejectedIconClass}
                     onClick={() => props.rejectedControlEnabled && props.selectRejectedMessage(props.selectedRejectedMessageId)}
                     style={{ cursor: props.rejectedControlEnabled ? 'pointer' : 'unset' }}
                     title={props.rejectedControlEnabled ? "Scroll to current rejected message" : null}/>
                {
                    props.showTitles ?
                        <div className={rejectedTitleClass}>
                            <p>{props.rejectedControlEnabled ? "" : "No "}Rejected</p>
                        </div> :
                        null
                }
                {
                    props.rejectedControlEnabled ?
                        <RejectedMessagesCarousel/> :
                        null
                }
            </div>
            <div className={predictionRootClass}
                 title={props.predictionsEnabled ? "Hide predictions" : "Show predictions"}
                 onClick={onPredictionClick}>
                <div className={predictionIconClass}/>
                <div className={predictionTitleClass}>
                    {
                        props.showTitles ?
                            <p>{props.predictionsAvailable ? "Predictions" : "No predictions"}</p> :
                            null
                    }
                </div>
            </div>
        </React.Fragment>
    )
}

const MessagePanelControl = connect(
    (state: AppState): StateProps => ({
        adminControlEnabled: getAdminMessages(state).length > 0,
        rejectedControlEnabled: getRejectedMessages(state).length > 0,
        selectedRejectedMessageId: state.selected.rejectedMessageId,
        adminMessagesEnabled: state.view.adminMessagesEnabled.valueOf(),
        beautifiedMessagesEnabled: state.view.beautifiedMessages.length > 0,
        predictionsAvailable: getIsPredictionsAvailable(state),
        predictionsEnabled: state.machineLearning.predictionsEnabled,
        hasKnownBugs: state.selected.testCase.bugs.length > 0
    }),
    (dispatch): DispatchProps => ({
        selectRejectedMessage: messageId => dispatch(selectRejectedMessageId(messageId)),
        adminEnabledHandler: adminEnabled => dispatch(setAdminMsgEnabled(adminEnabled)),
        togglePredictions: () => dispatch(togglePredictions()),
        uglifyAllHandler: () => dispatch(uglifyAllMessages()),
    })
)(MessagePanelControlsBase);

export default MessagePanelControl;
