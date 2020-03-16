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
import { connect } from 'react-redux';
import { Header } from './Header';
import { SplitView } from './SplitView'
import { LeftPanel } from './LeftPanel';
import { RightPanel } from './RightPanel';
import LinearProgressBar from './LinearProgressBar';
import NetworkError from './NetworkError';
import AppState from '../state/models/AppState';
import { getIsConnectionError, getTestCaseLoadingProgress } from '../selectors/view';
import '../styles/layout.scss';
import { stopWatchingTestCase } from '../thunks/loadTestCase';
import { ThunkDispatch } from 'redux-thunk';
import StateActionType from '../actions/stateActions';
import { setPanelArea } from "../actions/actionCreators";
import PanelArea from "../util/PanelArea";

interface StateProps {
    testCaseLoadingProgress: number;
    isConnectionError: boolean;
    panelArea: PanelArea;
}

interface DispatchProps {
    stopWatchingTestCase: () => void;
    setPanelArea: (panelArea: PanelArea) => void;
}

interface Props extends StateProps, DispatchProps {}

const TestCaseLayout = ({ testCaseLoadingProgress, isConnectionError, stopWatchingTestCase, setPanelArea, panelArea }: Props) => {
    React.useEffect(() => {
        return () => {
            stopWatchingTestCase();
        }
    }, []);

    return (
        <div className="layout">
            <div className="layout__header">
                {!isConnectionError && <LinearProgressBar progress={testCaseLoadingProgress}/>}
                {isConnectionError && <NetworkError />}
                <Header/>
            </div>
            <div className="layout__body">
                <SplitView
                    panelArea={panelArea}
                    onPanelAreaChange={setPanelArea}
                    leftPanelMinWidth={500}
                    rightPanelMinWidth={620}>
                    <LeftPanel/>
                    <RightPanel/>
                </SplitView>
            </div>
        </div>
    )
};

export default connect(
    (state: AppState) : StateProps=> ({
        testCaseLoadingProgress: getTestCaseLoadingProgress(state),
        isConnectionError: getIsConnectionError(state),
        panelArea: state.view.panelArea
    }),
    (dispatch: ThunkDispatch<AppState, {}, StateActionType>): DispatchProps => ({
        stopWatchingTestCase: () => dispatch(stopWatchingTestCase()),
        setPanelArea: panelArea => dispatch(setPanelArea(panelArea))
    })
)(TestCaseLayout);
