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

export class App extends Component<{}, {}> {

    constructor(props) {
        super(props);
        this.state = {
            report: null,
            selectedTestCase: null,
            selectedTestCaseName: "",
            isLoading: true
        }
        window['loadJsonp'] = this.loadJsonpHandler.bind(this);
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
            selectedTestCase: null
        })
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

    render(props: {}, {report, selectedTestCase, isLoading}: AppState) {
        if (isLoading) return (
            <div class="root">
                <p>Loading json...</p>
                <script src="report.js"></script>
            </div>
        );

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