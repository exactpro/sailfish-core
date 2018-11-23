import { h, Component } from "preact";
import "../styles/styles.scss";
import TestCaseLayout from "./TestCaseLayout";
import Report from '../models/Report';
import TestCase from "../models/TestCase";
import ReportLayout from '../components/ReportLayout'

interface AppState {
    report: Report;
    selectedTestCase: TestCase;
    isLoading: boolean;
}

export class App extends Component<{}, AppState> {

    constructor(props) {
        super(props);
        this.state = {
            report: null,
            selectedTestCase: null,
            isLoading: true
        }
        window['loadJsonp'] = this.loadJsonpHandler.bind(this);
    }

    loadJsonpHandler(jsonReport: Report) {
        let count = 0;
        this.setState({
            report: {
                ...jsonReport,
                testCases: jsonReport.testCases.map(testCase => {
                    return {...testCase, name: "TestCase" + count++}
                })
            },
            isLoading: false
        })
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

    render(props: {}, {report, selectedTestCase, isLoading}: AppState) {
        if (isLoading) return (
            <div class="root">
                <p>Loading json...</p>
                <script src="report.js"></script>
            </div>
        );

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