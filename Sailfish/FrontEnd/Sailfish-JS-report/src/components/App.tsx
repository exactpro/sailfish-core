import { h, Component } from "preact";
import "../styles/styles.scss";
import TestCaseLayout from "./TestCaseLayout";
import Report, { isReport } from '../models/Report';
import TestCase from "../models/TestCase";
import ReportLayout from '../components/ReportLayout';

interface AppState {
    report: Report;
    selectedTestCase: TestCase;
    isLoading: boolean;
    currentTestCasePath: string;
}

export class App extends Component<{}, AppState> {

    constructor(props) {
        super(props);
        this.state = {
            report: null,
            selectedTestCase: null,
            isLoading: true,
            currentTestCasePath: null
        }
        window['loadJsonp'] = this.loadJsonpHandler.bind(this);
    }

    loadJsonpHandler(jsonp: Report | TestCase) {
        if (isReport(jsonp)) {
            this.setState({
                ...this.state,
                report: (jsonp as Report),
                isLoading: false
            })
        } else {
            this.setState({
                ...this.state,
                selectedTestCase: (jsonp as TestCase)
            })
        }
    }

    setTestCasePath(testCaseName: string) : void {
        this.setState({
            ...this.state,
            currentTestCasePath: testCaseName,
            selectedTestCase: null
        });
    }

    backToReport() : void {
        this.setState({
            ...this.state,
            currentTestCasePath: null
        });
    }

    getNextTestCasePath() : string {
        const currentIndex = this.state.report.testCaseLinks.indexOf(this.state.currentTestCasePath);
        return this.state.report.testCaseLinks[currentIndex + 1];
    }

    getPrevTestCasePath() : string {
        const currentIndex = this.state.report.testCaseLinks.indexOf(this.state.currentTestCasePath);
        return currentIndex !== 0 && this.state.report.testCaseLinks[currentIndex - 1];
    }

    render(props: {}, {report, selectedTestCase, isLoading, currentTestCasePath}: AppState) {
        if (isLoading) return (
            <div class="root">
                <p>Loading json...</p>
                <script src="report/report.js"></script>
            </div>
        );

        if (!currentTestCasePath) {
            return (<ReportLayout report={report}
                onTestCaseSelect={(testCase) => this.setTestCasePath(testCase)}/>);
        }

        if (selectedTestCase) {
            const next = this.getNextTestCasePath(),
                prev = this.getPrevTestCasePath();

            return (
                <div class="root">
                    <TestCaseLayout testCase={selectedTestCase}
                        backToReportHandler={() => this.backToReport()}
                        nextHandler={next ? () => this.setTestCasePath(next) : null}
                        prevHandler={prev ? () => this.setTestCasePath(prev) : null}/>
                </div>
            );
        }

        return (
            <div class="root">
                <p>Loading json...</p>
                <script src={"report/" + currentTestCasePath}></script>
            </div>
        )
    };
}