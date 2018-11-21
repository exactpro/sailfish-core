import { h, Component } from "preact";
import "../styles/styles.scss";
import TestCaseLayout from "./TestCaseLayout";
import ReportLayout from './ReportLayout';
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
        let count = 1;
        this.state = {
            report: {
                ...testReport,
                testCases: testReport.testCases.map(testCase => {
                    return {...testCase, name:"TestCase" + count++}
                })
            },
            selectedTestCase: null,
            selectedTestCaseName: ""
        }
    }

    onTestCaseSelected(testCase: TestCase) {
        this.setState({
            ...this.state,
            selectedTestCase: testCase,
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
                        backToReportHandler={() => this.backToReport()}/>) : 
                    (<ReportLayout report={report}
                        onTestCaseSelect={(testCase) => this.onTestCaseSelected(testCase)}/>)
                }
            </div>
        );
    };
}