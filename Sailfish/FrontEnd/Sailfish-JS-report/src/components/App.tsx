import { h, Component } from "preact";
import "../styles/styles.scss";
import { testCase } from "../test/testCase";
import ActionCard from "./ActionCard";
import TestCaseLayout from "./TestCaseLayout";

export class App extends Component<{}, {}> {

    render(props: {}, state: {}) {
        //jsonp load handler
       // window.loadJsonp = (data) => console.log(data);
        return(
            <div class="root">
                <TestCaseLayout testCase={testCase}/>
                <script src="report.js"></script>
            </div>
        );
    };
}