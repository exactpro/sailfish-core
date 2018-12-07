import { h, Component } from 'preact';
import { Header } from './Header';
import { SplitView } from './SplitView'
import { ActionsList } from './ActionsList';
import TestCase from '../models/TestCase';
import {TogglerButton} from './TogglerButton';
import {MessagesCardList} from './MessagesCardList';
import '../styles/layout.scss'
import Action from '../models/Action';
import { StatusType } from '../models/Status';
import { StatusPane } from './StatusPane';
import { LogsPane } from './LogsPane';

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
    selectedActionId: number;
    selectedMessages: number[];
    isSplitMode: boolean;
    showFilter: boolean;
    primaryPane: Pane;
    secondaryPane: Pane;
    actionsFilter: StatusType[];
    fieldsFilter: StatusType[];
    filtredActions: Action[];
}

enum Pane {Actions, Status, Messages, Logs}

export default class TestCaseLayout extends Component<LayoutProps, LayoutState> {

    constructor(props: LayoutProps) {
        super(props);
        this.state = {
            selectedActionId: -1,
            selectedMessages: [],
            isSplitMode: true,
            showFilter: false,
            primaryPane: Pane.Actions,
            secondaryPane: Pane.Messages,
            fieldsFilter: ['PASSED', 'FAILED', 'CONDITIONALLY_PASSED', 'NA'],
            actionsFilter: ['PASSED', 'FAILED', 'CONDITIONALLY_PASSED'],
            filtredActions: props.testCase.actions
        }
    }

    actionSelectedHandler(action: Action) {
        this.setState({
            ...this.state,
            selectedActionId: action.id,
            selectedMessages: action.relatedMessages
        });
    }

    messageSelectedHandler(id: number) {
        this.setState({
            ...this.state,
            selectedActionId: -1,
            selectedMessages: [id]
        });
    }

    splitModeHandler() {
        this.setState({
            ...this.state,
            isSplitMode: !this.state.isSplitMode
        });
    }

    showFilterHandler() {
        this.setState({
            ...this.state,
            showFilter: !this.state.showFilter
        });
    }

    actionFilterHandler(status: StatusType) {
        if (this.state.actionsFilter.includes(status)) {
            const newActionsFilter = this.state.actionsFilter.filter(action => action != status);

            this.setState({
                ...this.state,
                actionsFilter: newActionsFilter,
                filtredActions: this.props.testCase.actions.filter(action =>
                    newActionsFilter.includes(action.status.status)) 
            });
        } else {
            const newActionsFilter = [...this.state.actionsFilter, status];

            this.setState({
                ...this.state,
                actionsFilter: newActionsFilter,
                filtredActions: this.props.testCase.actions.filter(action =>
                    newActionsFilter.includes(action.status.status)) 
            });
        }
    }

    fieldFiterHandeler(status: StatusType) {
        if (this.state.fieldsFilter.includes(status)) {
            this.setState({
                ...this.state,
                fieldsFilter: this.state.fieldsFilter.filter(field => field != status)
            });
        } else {
            this.setState({
                ...this.state,
                fieldsFilter: [...this.state.fieldsFilter, status]
            })
        }
    }

    togglePane(pane: Pane, isPrimary: boolean) {
        this.setState({
            ...this.state,
            primaryPane: isPrimary ? pane : this.state.primaryPane,
            secondaryPane: !isPrimary ? pane : this.state.secondaryPane
        });
    }

    // it used to update actions after switching the test case: 
    // its bad approach, but we dont have memorize feature in preact.
    componentWillReceiveProps(nextProps: LayoutProps) {
        if (nextProps.testCase !== this.props.testCase) {
            this.setState({
                ...this.state,
                filtredActions: nextProps.testCase.actions.filter(action =>
                    this.state.actionsFilter.includes(action.status.status)) 
            });
        }
    }

    render({testCase, backToReportHandler, nextHandler, prevHandler} : LayoutProps,
        {selectedActionId, 
            isSplitMode,
            primaryPane, 
            secondaryPane, 
            selectedMessages, 
            showFilter, 
            actionsFilter, 
            fieldsFilter,
            filtredActions} : LayoutState) {

        // if some action is selected, all messages inside this action should not be highlighted
        const actionsElement = (<ActionsList actions={filtredActions}
                onSelect={action => this.actionSelectedHandler(action)}
                selectedActionId={selectedActionId}
                onMessageSelect={id => this.messageSelectedHandler(id)}
                selectedMessageId={selectedActionId >= 0 ? -1 : selectedMessages[0]}
                filterFields={fieldsFilter}/>);

        const messagesElement = (<MessagesCardList messages={testCase.messages}
            selectedMessages={selectedMessages}/>);

        const statusElement = (
            <StatusPane status={testCase.status}/>
        );

        const logsElement = (
            <LogsPane logs={testCase.logs}/>
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

        const rootClassName = ["layout", (showFilter ? "filter" : "")].join(' ');

        return (
            <div class={rootClassName}>
                <div class="layout-header">
                    <Header 
                        testCase={testCase}
                        name={testCase.name ? testCase.name : "TestCase"}
                        splitMode={isSplitMode}
                        splitModeHandler={() => this.splitModeHandler()}
                        showFilter={showFilter}
                        showFilterHandler={() => this.showFilterHandler()}
                        backToListHandler={() => backToReportHandler()}
                        nextHandler={nextHandler}
                        prevHandler={prevHandler}
                        actionsFilter={actionsFilter}
                        fieldsFilter={fieldsFilter}
                        actionsFilterHandler={status => this.actionFilterHandler(status)}
                        fieldsFilterHandler={status => this.fieldFiterHandeler(status)}/>
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