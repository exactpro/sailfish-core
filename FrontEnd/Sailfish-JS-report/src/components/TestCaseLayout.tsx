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

const MIN_PANEL_WIDTH = 600;

interface TestCaseLayoutProps {
    testCaseLoadingProgress: number;
    isConnectionError: boolean;
}

const TestCaseLayout = ({ testCaseLoadingProgress, isConnectionError }: TestCaseLayoutProps) => {
    return (
        <div className="layout">
            <div className="layout__header">
                {!isConnectionError && <LinearProgressBar progress={testCaseLoadingProgress}/>}
                {isConnectionError && <NetworkError />}
                <Header/>
            </div>
            <div className="layout__body">
                <SplitView minPanelWidth={MIN_PANEL_WIDTH}>
                    <LeftPanel/>
                    <RightPanel/>
                </SplitView>
            </div>
        </div>
    )
}

export default connect((state: AppState) => ({
    testCaseLoadingProgress: getTestCaseLoadingProgress(state),
    isConnectionError: getIsConnectionError(state),
}))(TestCaseLayout);
