import { h, Component } from 'preact';
import { Header } from './Header';
import { SplitView } from './SplitView'
import { ActionsList } from './ActionsList';
import TestCase from '../models/TestCase';
import {MessagesTable} from './MessagesTable';
import {TogglerButton} from './TogglerButton';
import Message from '../models/Message';
import {MessagesCardList} from './MessagesCardList';
import '../styles/layout.scss'
import Action from '../models/Action';

interface LayoutProps {
    testCase: TestCase;
    backToReportHandler: Function;
    nextHandler?: Function;
    prevHandler?: Function;
}

/**
 * @param {Pane} primaryPane - in non split mode name of rendered panel, in split mode - name of left pane
 * @param {Pane} secondaryPane - in split mode - name of right pane
 */
interface LayoutState {
    messages: Array<Message>;
    selectedActionId: number;
    isSplitMode: boolean;
    primaryPane: Pane;
    secondaryPane: Pane;
}

enum Pane {Actions, Status, Messages, Logs}

export default class TestCaseLayout extends Component<LayoutProps, LayoutState> {

    constructor(props: LayoutProps) {
        super(props);
        this.state = {
            messages: this.setStatusToMessages(props.testCase.messages, props.testCase.actions),
            selectedActionId: -1,
            isSplitMode: true,
            primaryPane: Pane.Actions,
            secondaryPane: Pane.Messages
        }
    }

    //this function set status of parent action to related messsages
    setStatusToMessages(messages: Array<Message>, actions: Array<Action>): Array<Message> {
        return [...messages.map((message) => {
            const baseAction = actions.find(action =>
                action.relatedMessages ? 
                action.relatedMessages.includes(message.id) : false);
            return {
                ...message,
                status: baseAction ? baseAction.status.status : "NA"
            };
        })]
    }

    actionSelectedHandler(id: number) {
        this.setState({
            ...this.state,
            selectedActionId: id
        });
    }

    splitModeHandler() {
        this.setState({
            ...this.state,
            isSplitMode: !this.state.isSplitMode
        });
    }

    togglePane(pane: Pane, isPrimary: boolean) {
        this.setState({
            ...this.state,
            primaryPane: isPrimary ? pane : this.state.primaryPane,
            secondaryPane: !isPrimary ? pane : this.state.secondaryPane
        })
    }

    render({testCase, backToReportHandler, nextHandler, prevHandler} : LayoutProps,
        {selectedActionId, isSplitMode, primaryPane, secondaryPane, messages} : LayoutState) {

        const actionsElement = (<ActionsList actions={testCase.actions}
                onSelect={(id) => this.actionSelectedHandler(id)}
                selectedActionId={selectedActionId}/>);
        const messagesElement = (<MessagesCardList messages={messages}
            selectedActionId={selectedActionId}/>);
        const statusElement = (<div>
            STATUS
        </div>);
        const logsElement = (
            <div>LOGS</div>
        );

        let primaryPaneElement;
        let secondaryPaneElement;

        switch (primaryPane) {
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

        switch (secondaryPane) {
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
        return (
            <div class="layout-root">
                <div class="layout-header">
                    <Header 
                        testCase={testCase}
                        name={testCase.name ? testCase.name : "TestCase"}
                        splitMode={isSplitMode}
                        splitModeHandler={() => this.splitModeHandler()}
                        backToListHandler={() => backToReportHandler()}
                        nextHandler={nextHandler}
                        prevHandler={prevHandler}/>
                </div>
                <div class={"layout-buttons" + (isSplitMode ? " split" : "")}>
                    <div class="layout-buttons-left">
                        <TogglerButton
                            isToggled={primaryPane == Pane.Actions}
                            click={() => this.togglePane(Pane.Actions, true)}
                            text="Actions"/>
                        <TogglerButton
                            isToggled={primaryPane == Pane.Status}
                            click={() => this.togglePane(Pane.Status, true)}
                            text="Status"/>
                    </div>
                    <div class="layout-buttons-right">
                        <TogglerButton
                            isToggled={primaryPane == Pane.Messages || 
                                (secondaryPane == Pane.Messages && isSplitMode)}
                            click={() => this.togglePane(Pane.Messages, !isSplitMode)}
                            text="Messages"/>
                        <TogglerButton
                            isToggled={primaryPane == Pane.Logs || 
                                (secondaryPane == Pane.Logs && isSplitMode)}
                            click={() => this.togglePane(Pane.Logs, !isSplitMode)}
                            text="Logs"/>
                    </div>
                </div>
                {isSplitMode ?
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