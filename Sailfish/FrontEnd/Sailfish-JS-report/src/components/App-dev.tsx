import { h, Component } from "preact";
import "../styles/styles.scss";
import TestCaseLayout from "./TestCaseLayout";
import Message from '../models/Message';
import Action from '../models/Action';
import TestCase from "../models/TestCase";
import {testCase} from '../test/testCase';

interface AppState {
    testCase: TestCase;
}

export class App extends Component<{}, {}> {

    constructor(props) {
        super(props);
        this.state = {
            testCase: testCase
        }
    }

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

    render(props: {}, {testCase}: AppState) {
        const loadedTestCase = {
            ...testCase,
            messages: this.setStatusToMessages(testCase.messages, testCase.actions)
        }
        return(
            <div class="root">
                <TestCaseLayout testCase={loadedTestCase}/>
            </div>
        );
    };
}