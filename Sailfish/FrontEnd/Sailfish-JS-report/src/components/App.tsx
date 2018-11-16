import { h, Component } from "preact";
import "../styles/styles.scss";
import { testCase } from "../test/testCase";
import TestCaseLayout from "./TestCaseLayout";
import Message from '../models/Message';
import Action from '../models/Action';

export class App extends Component<{}, {}> {

    //this function set status of parent action to related messsages
    setStatusToMessages(messages: Array<Message>, actions: Array<Action>): Array<Message> {
        return [...messages.map((message) => {
            const baseAction = actions.find(action =>
                action.relatedMessages ? 
                action.relatedMessages.includes(message.uuid) : false);
            return {
                ...message,
                status: baseAction ? baseAction.status : "NA"
            };
        })]
    }

    render(props: {}, state: {}) {
        //jsonp load handler
       //window.loadJsonp = (data) => console.log(data);
       const loadedTestCase = {
           ...testCase,
           messages: this.setStatusToMessages(testCase.messages, testCase.actions)
       }
        return(
            <div class="root">
                <TestCaseLayout testCase={loadedTestCase}/>
                <script src="report.js"></script>
            </div>
        );
    };
}