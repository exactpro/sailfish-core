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
import { Header } from './Header';
import { SplitView } from './SplitView'
import { LeftPanel } from './LeftPanel';
import { RightPanel } from './RightPanel';
import '../styles/layout.scss';

const TestCaseLayout = () =>  (
    <div class="layout">
        <div class="layout-header">
            <Header/>
        </div>
            <div class="layout-body split">
                <SplitView
                    minPanelPercentageWidth={30}>
                    <LeftPanel/>
                    <RightPanel/>
                </SplitView>
            </div>
    </div>
)

export default TestCaseLayout;