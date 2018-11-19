import { h, Component } from "preact";
import "../styles/styles.scss";
import TestCaseLayout from "./TestCaseLayout";
import Message from '../models/Message';
import Action from '../models/Action';
import Report from '../models/Report';
import TestCase from "../models/TestCase";
import {ReportLayout} from '../components/ReportLayout'

interface AppState {
    report: Report;
    selectedTestCase: TestCase;
    selectedTestCaseName: string;
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

    loadJsonpHandler(json: Report) {
        this.setState({
            report: json,
            isLoading: false
        })
    }

    render(props: {}, {report, selectedTestCase, selectedTestCaseName, isLoading}: AppState) {
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
                    backToReportHandler={() => this.backToReport()}
                    testCaseName={selectedTestCaseName}/>) : 
                (<ReportLayout report={report}
                    onTestCaseSelect={(testCase, name) => this.onTestCaseSelected(testCase, name)}/>)
            }
        </div>
        );
    };
}