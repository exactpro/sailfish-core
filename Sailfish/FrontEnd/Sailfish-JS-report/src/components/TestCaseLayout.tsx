import { h, Component } from 'preact';
import { Header } from './Header';
import { SplitView } from './SplitView'
import ActionsList from './ActionsList';
import TestCase from '../models/TestCase';
import {MessagesTable} from './MessagesTable';
import '../styles/layout.scss'

interface LayoutProps {
    testCase: TestCase;
}

interface LayoutState {

}

export default class TestCaseLayout extends Component<LayoutProps, LayoutState> {

    constructor(props: LayoutProps) {
        super(props);
        this.state = {

        }
    }

    render({testCase} : LayoutProps, {} : LayoutState) {
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
                            <ActionsList {...testCase}/>
                            <MessagesTable messages={testCase.messages}/>
                        </SplitView>
                </div>
            </div>
        )
    }
}