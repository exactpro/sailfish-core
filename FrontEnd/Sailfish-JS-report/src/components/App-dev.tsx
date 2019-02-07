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

import { h, Component } from "preact";
import "../styles/styles.scss";
import TestCaseLayout from "./TestCaseLayout";
import ReportLayout from './ReportLayout';
import TestCase from "../models/TestCase";
import Report from "../models/Report";
import { connect } from 'preact-redux';
import AppState from "../state/AppState";
import { setTestCase, setTestCasePath, selectAction } from "../actions/actionCreators";
import { selectActionById } from '../actions/actionCreators';
import { selectMessages } from '../actions/actionCreators';

const TEST_CASE_PARAM_KEY = 'tc',
      ACTION_PARAM_KEY = 'action',
      MESSAGE_PARAM_KEY = 'message';

interface AppProps {
    report: Report;
    testCase: TestCase;
    testCaseFilePath: string;
    selectedActionId: number;
    selectedMessages: number[];
    updateTestCase: (testCase: TestCase) => any;
    updateTestCasePath: (testCasePath: string) => any;
    selectAction: (actionId: number) => any;
    selectMessage: (messageId: number) => any;
}

class AppBase extends Component<AppProps> {

    private searchParams: URLSearchParams;

    constructor(props: AppProps) {
        super(props);
    
        this.searchParams = new URLSearchParams(window.location.search);
    }

    componentDidMount() {
        // shared URL
        const testCaseId = this.searchParams.get(TEST_CASE_PARAM_KEY),
            actionId = this.searchParams.get(ACTION_PARAM_KEY),
            msgId = this.searchParams.get(MESSAGE_PARAM_KEY);

        if (testCaseId != null) {
            this.selectTestCaseById(testCaseId)
        }

        if (Number(msgId)) {
            this.props.selectMessage(Number(msgId))
        }

        if (Number(actionId)) {
            this.props.selectAction(Number(actionId));
        }
    }

    componentDidUpdate(prevProps: AppProps) {

        if (prevProps.testCase == this.props.testCase && prevProps.selectedActionId == this.props.selectedActionId) {
            return;
        }

        if (prevProps.testCase != this.props.testCase) {
            if (this.props.testCase) {
                this.searchParams.set(TEST_CASE_PARAM_KEY, this.props.testCase.id);
            } else {
                this.searchParams.delete(TEST_CASE_PARAM_KEY);
            }
        }

        if (prevProps.selectedActionId != this.props.selectedActionId) {
            if (this.props.selectedActionId != null) {
                this.searchParams.set(ACTION_PARAM_KEY, this.props.selectedActionId.toString());
            } else {
                this.searchParams.delete(ACTION_PARAM_KEY);
            }
        }

        // verification message selection handling
        if (this.props.selectedActionId == null && prevProps.selectedMessages != this.props.selectedMessages) {
            if (this.props.selectedMessages && this.props.selectedMessages.length != 0) {
                this.searchParams.set(MESSAGE_PARAM_KEY, this.props.selectedMessages[0].toString());
            } else {
                this.searchParams.delete(MESSAGE_PARAM_KEY);
            }
        }

        let newUrl = "";

        if (window.location.search) {
            // replacing old search params with the new search params
            const oldParams = new URLSearchParams(window.location.search);

            newUrl = window.location.href.replace(oldParams.toString(), this.searchParams.toString());
        } else {
            // creating new search params and appending it to the current url 
            const href = window.location.href;
            newUrl = [href, 
                href[href.length - 1] != '?' ? '?' : null,
                this.searchParams.toString()
            ].join('');
        }

        window.history.pushState({}, "", newUrl);
    }

    selectTestCaseById(testCaseId: string) {
        const testCaseMetadata = this.props.report.metadata.find(metadata => metadata.id === testCaseId);
        
        if (testCaseMetadata) {
            this.props.updateTestCasePath(testCaseMetadata.jsonpFileName);
        }
    }

    render({ report, testCase, testCaseFilePath, updateTestCase }: AppProps) {

        if (testCase) {
            return (
                <div class="root">
                    <TestCaseLayout />
                </div>
            );
        }

        if (testCaseFilePath) {
            const currentMetadata = report.metadata.find(metadata => metadata.jsonpFileName === testCaseFilePath),
                currentTestCase = report.testCases.find(testCase => testCase.name === currentMetadata.name);

            updateTestCase(currentTestCase)
            return null;
        }

        return (
            <div class="root">
                <ReportLayout />
            </div>
        );
    }
}

export const App = connect(
    (state: AppState) => ({
        report: state.report,
        testCase: state.testCase,
        testCaseFilePath: state.currentTestCasePath,
        selectedActionId: state.selected.actionId,
        selectedMessages: state.selected.messagesId
    }),
    dispatch => ({
        updateTestCase: (testCase: TestCase) => dispatch(setTestCase(testCase)),
        updateTestCasePath: (testCasePath: string) => dispatch(setTestCasePath(testCasePath)),
        selectAction: (actionId: number) => dispatch(selectActionById(actionId)),
        selectMessage: (messageId: number) => dispatch(selectMessages([messageId]))
    })
)(AppBase)