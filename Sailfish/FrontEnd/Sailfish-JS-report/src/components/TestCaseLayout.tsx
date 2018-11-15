import { h, Component } from 'preact';
import { Header } from './Header';
import { SplitView } from './SplitView'
import ActionsList from './ActionsList';
import TestCase from '../models/TestCase';
import {MessagesTable} from './MessagesTable';
import Message from '../models/Message';
import '../styles/layout.scss'
import Action from '../models/Action';

interface LayoutProps {
    testCase: TestCase;
}

interface LayoutState {
    messages: Array<Message>;
    selectedActionId: string;
}

export default class TestCaseLayout extends Component<LayoutProps, LayoutState> {

    constructor(props: LayoutProps) {
        super(props);
        this.state = {
            messages: props.testCase.messages,
            selectedActionId: ""
        }
    }

    actionSelectedHandler(id: string) {
        this.setState({
            ...this.state,
            selectedActionId: id
        })
    }

    render({testCase} : LayoutProps, {selectedActionId} : LayoutState) {
        return (
            <div class="layout-root">
                <div class="layout-header">
                    <Header {...testCase}
                        name="someName"/>
                </div>
                <div class="layout-left-buttons">
                </div>
                <div class="layout-right-buttons">
                </div>
                <div class="layout-content split">
                        <SplitView>
                            <ActionsList {...testCase}
                                onSelect={(id) => this.actionSelectedHandler(id)}/>
                            <MessagesTable messages={testCase.messages}
                                selectedActionID={selectedActionId}/>
                        </SplitView>
                </div>
            </div>
        )
    }
}