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

import { h, Component } from 'preact';
import { connect } from 'preact-redux';
import { Header } from './Header';
import { SplitView } from './SplitView'
import { ActionsListBase } from './ActionsList';
import TestCase from '../models/TestCase';
import { MessagesCardListBase } from './MessagesCardList';
import AppState from '../state/AppState';
import '../styles/layout.scss';
import { LeftPanel } from './LeftPanel';
import { RightPanel } from './RightPanel';

interface LayoutProps {
    testCase: TestCase;
    showFilter: boolean;
}

class TestCaseLayoutBase extends Component<LayoutProps> {

    private actionsListRef: ActionsListBase;
    private messagesListRef: MessagesCardListBase;

    constructor(props: LayoutProps) {
        super(props);
    }

    // FIXME : need to move this logic to redux
    scrollToTopHandler() {
        if (this.actionsListRef) {
            this.actionsListRef.scrollToAction(this.props.testCase.actions[0].id);
        }

        if (this.messagesListRef) {
            this.messagesListRef.scrollToMessage(this.props.testCase.messages[0].id);
        }
    }

    render({ showFilter }: LayoutProps) { 

        const rootClassName = ["layout", (showFilter ? "filter" : "")].join(' ');

        return (
            <div class={rootClassName}>
                <div class="layout-header">
                    <Header goTopHandler={() => this.scrollToTopHandler()} />
                </div>
                    <div class="layout-body split">
                        <SplitView
                            minPanelPercentageWidth={30}
                            resizeHandler={this.splitResizeHandler}>
                            <LeftPanel/>
                            <RightPanel/>
                        </SplitView>
                    </div>
            </div>
        )
    }

    private splitResizeHandler = (leftPanelWidth: number, rightPanelWidth: number) => {
        this.setState({
            rightPanelWidth: rightPanelWidth
        });
    }
}

const TestCaseLayout = connect(
    (state: AppState) => ({
        testCase: state.testCase,
        showFilter: state.showFilter
    }),
    dispatch => ({})
)(TestCaseLayoutBase);

export default TestCaseLayout;