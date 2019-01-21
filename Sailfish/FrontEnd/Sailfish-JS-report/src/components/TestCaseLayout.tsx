import { h, Component } from 'preact';
import { connect } from 'preact-redux';
import { Header } from './Header';
import { SplitView } from './SplitView'
import { ActionsList } from './ActionsList';
import TestCase from '../models/TestCase';
import {TogglerButton} from './TogglerButton';
import {MessagesCardList} from './MessagesCardList';
import { StatusPane } from './StatusPane';
import { LogsPane } from './LogsPane';
import AppState from '../state/AppState';
import { setLeftPane, setRightPane } from '../actions/actionCreators';
import { Pane } from '../helpers/Pane';
import '../styles/layout.scss';

interface LayoutProps {
    testCase: TestCase;
    splitMode: boolean;
    showFilter: boolean;
    leftPane: Pane;
    rightPane: Pane;
    leftPaneHandler: (pane: Pane) => any;
    rightPaneHandler: (pane: Pane) => any;
}

class TestCaseLayoutBase extends Component<LayoutProps, any> {

    constructor(props: LayoutProps) {
        super(props);
    }

    render({testCase, splitMode, showFilter, leftPane, rightPane, leftPaneHandler, rightPaneHandler} : LayoutProps) {

        // if some action is selected, all messages inside this action should not be highlighted
        const actionsElement = (<ActionsList/>);

        const messagesElement = (<MessagesCardList/>);

        const statusElement = (
            <StatusPane status={testCase.status}/>
        );

        const logsElement = (
            <LogsPane logs={testCase.logs}/>
        );

        let primaryPaneElement;
        let secondaryPaneElement;

        switch (leftPane) {
            case Pane.Actions:
                primaryPaneElement = actionsElement;
                break;
            case Pane.Messages:
                primaryPaneElement = messagesElement;
                break;
            case Pane.Status: 
                primaryPaneElement = statusElement;
                break;
            case Pane.Logs:
                primaryPaneElement = logsElement;
                break;
            default:
                primaryPaneElement = null;
                break;
        }

        switch (rightPane) {
            case Pane.Messages:
                secondaryPaneElement = messagesElement;
                break;
            case Pane.Logs:
                secondaryPaneElement = logsElement;
                break;
            default:
                primaryPaneElement = null;
                break;
        }

        const rootClassName = ["layout", (showFilter ? "filter" : "")].join(' ');

        return (
            <div class={rootClassName}>
                <div class="layout-header">
                    <Header/>
                </div>
                <div class={"layout-buttons" + (splitMode ? " split" : "")}>
                    <div class="layout-buttons-left">
                        <TogglerButton
                            isToggled={leftPane == Pane.Actions}
                            click={() => leftPaneHandler(Pane.Actions)}
                            text="Actions"/>
                        <TogglerButton
                            isToggled={leftPane == Pane.Status}
                            click={() => leftPaneHandler(Pane.Status)}
                            text="Status"/>
                    </div>
                    <div class="layout-buttons-right">
                        <TogglerButton
                            isToggled={leftPane == Pane.Messages || 
                                (rightPane == Pane.Messages && splitMode)}
                            click={() => splitMode ? rightPaneHandler(Pane.Messages) : leftPaneHandler(Pane.Messages)}
                            text="Messages"/>
                        <TogglerButton
                            isToggled={leftPane == Pane.Logs || 
                                (rightPane == Pane.Logs && splitMode)}
                            click={() => splitMode ? rightPaneHandler(Pane.Logs) : leftPaneHandler(Pane.Logs)}
                            text="Logs"/>
                    </div>
                </div>
                {splitMode ?
                    (<div class="layout-content split">
                        <SplitView>
                            {primaryPaneElement}
                            {secondaryPaneElement}
                        </SplitView>
                    </div>) : 
                    (<div class="layout-content">
                        {primaryPaneElement}
                    </div>)}
            </div>
        )
    }
}

const TestCaseLayout = connect(
    (state: AppState) => ({
        testCase: state.testCase,
        splitMode: state.splitMode,
        showFilter: state.showFilter,
        leftPane: state.leftPane,
        rightPane: state.rightPane
    }),
    dispatch => ({ 
        leftPaneHandler: (pane: Pane) => dispatch(setLeftPane(pane)),
        rightPaneHandler: (pane: Pane) => dispatch(setRightPane(pane))
    })
)(TestCaseLayoutBase);

export default TestCaseLayout;