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
}

export class App extends Component<{}, AppState> {

    constructor(props) {
        super(props);
        let count = 1;
        this.state = {
            report: {
                ...testReport,
                testCases: testReport.testCases.map(testCase => {
                    return {...testCase, name: testCase.name || "TEST CASE " + count++}
                })
            },
            selectedTestCase: null,
        }
    }

    selectTestCase(testCase: TestCase) : void {
        this.setState({
            ...this.state,
            selectedTestCase: testCase,
        })
    }

    backToReport() : void {
        this.setState({
            ...this.state,
            selectedTestCase: null
        })
    }

    getNextTestCase() : TestCase {
        const currentId = this.state.report.testCases.findIndex(
            testCase => testCase == this.state.selectedTestCase);
        
        return currentId != this.state.report.testCases.length - 1 &&
            this.state.report.testCases[currentId + 1];
    }

    getPrevTestCase() : TestCase {
        const currentId = this.state.report.testCases.findIndex(
            testCase => testCase == this.state.selectedTestCase);
        
        return currentId != 0 && this.state.report.testCases[currentId - 1];
    }

    render(props: {}, {selectedTestCase, report}: AppState) {

        if (!selectedTestCase) {
            return (<ReportLayout report={report}
                onTestCaseSelect={(testCase) => this.selectTestCase(testCase)}/>);
        }

        const next = this.getNextTestCase(),
            prev = this.getPrevTestCase();

        return(
            <div class="root">
                <TestCaseLayout testCase={selectedTestCase}
                    backToReportHandler={() => this.backToReport()}
                    nextHandler={next ? () => this.selectTestCase(next) : null}
                    prevHandler={prev ? () => this.selectTestCase(prev) : null}/>
            </div>
        );
    };
}