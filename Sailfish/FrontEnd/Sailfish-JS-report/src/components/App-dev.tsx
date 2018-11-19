import { h, Component } from "preact";
import "../styles/styles.scss";
import TestCaseLayout from "./TestCaseLayout";
import {ReportLayout} from './ReportLayout';
import Message from '../models/Message';
import Action from '../models/Action';
import TestCase from "../models/TestCase";
import {testReport} from '../test/testReport';
import Report from "../models/Report";

interface AppState {
    report: Report;
    selectedTestCase: TestCase;
    selectedTestCaseName: string;
}

export class App extends Component<{}, {}> {

    constructor(props) {
        super(props);
        this.state = {
            report: testReport,
            selectedTestCase: null,
            selectedTestCaseName: ""
        }
    }

    onTestCaseSelected(testCase: TestCase, name: string) {
        this.setState({
            ...this.state,
            selectedTestCase: testCase,
            selectedTestCaseName: name
        })
    }

    backToReport() {
        this.setState({
            ...this.state,
            selectedTestCase: null,
            selectedTestCaseName: ""
        })
    }

    render(props: {}, {selectedTestCase, report, selectedTestCaseName}: AppState) {

        return(
            <div class="root">
                {
                    selectedTestCase ?
                    (<TestCaseLayout testCase={selectedTestCase}
                        backToReportHandler={() => this.backToReport()}
                        testCaseName={selectedTestCaseName}/>) : 
                    (<ReportLayout report={report}
                        onTestCaseSelect={(testCase, name) => this.onTestCaseSelected(testCase, name)}/>)
                }
            </div>
        );
    };
}