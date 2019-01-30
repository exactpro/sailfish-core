import { h, Component, rerender } from 'preact';
import Action from '../models/Action';
import '../styles/action.scss';
import { ActionTree } from './ActionTree';
import { StatusType } from '../models/Status';
import AppState from '../state/AppState';
import { connect } from 'preact-redux';
import { selectAction, selectMessages, selectCheckpoint } from '../actions/actionCreators';
import { isCheckpoint } from '../helpers/messageType';

interface ListProps {
    actions: Array<Action>;
    checkpointMessagesIds: Array<number>;
    selectedActionId: number;
    selectedMessageId: number;
    selectedCheckpointId: number;
    actionsFilter: StatusType[];
    filterFields: StatusType[];
    onSelect: (messages: Action) => any;
    onMessageSelect: (id: number, status: StatusType) => any;
    setCheckpointId: (id: number) => any;
}

export class ActionsListBase extends Component<ListProps, {}> {

    private elements: ActionTree[] = [];

    scrollToAction(actionId: number) {
        if (this.elements[actionId]) {
            // smooth behavior is disabled here
            // base - get HTMLElement by ref
            this.elements[actionId].base.scrollIntoView({block: "start"});
        }    
    }

    shouldComponentUpdate(nextProps: ListProps) {
        if (nextProps.filterFields !== this.props.filterFields) {
            return true;
        }

        if (nextProps.actionsFilter !== this.props.actionsFilter) {
            return true;
        }

        if (nextProps.selectedCheckpointId !== this.props.selectedCheckpointId) {
            return true;
        }

        return nextProps.actions !== this.props.actions ||
            nextProps.selectedActionId !== this.props.selectedActionId ||
            nextProps.selectedMessageId !== this.props.selectedMessageId;
    }

    render({ actions, checkpointMessagesIds, selectedCheckpointId, selectedActionId, selectedMessageId, onSelect, actionsFilter, filterFields, onMessageSelect, setCheckpointId }: ListProps) {

        const cpIndex = checkpointMessagesIds.indexOf(selectedCheckpointId);

        return (
            <div class="actions">
                <div class="actions-controls">
                    {
                        checkpointMessagesIds && checkpointMessagesIds.length ? 
                        (
                            <div class="actions-controls-checkpoints">
                                <div class="actions-controls-checkpoints-icon"/>
                                <div class="actions-controls-checkpoints-title">
                                    <p>Checkpoints</p>
                                </div>
                                <div class="actions-controls-checkpoints-btn prev"
                                    onClick={this.prevCpHandler(checkpointMessagesIds, setCheckpointId, cpIndex)}/>
                                <div class="actions-controls-checkpoints-count">
                                    <p>{cpIndex === -1 ? 0 : cpIndex + 1} of {checkpointMessagesIds.length}</p>
                                </div>
                                <div class="actions-controls-checkpoints-btn next"
                                    onClick={this.nextCpHandler(checkpointMessagesIds, setCheckpointId, cpIndex)}/>
                            </div>
                        )
                        : null
                    }
                </div>
                <div class="actions-list">
                    {actions.map(action => (
                        <ActionTree 
                            action={action}
                            selectedActionId={selectedActionId}
                            selectedMessageId={selectedMessageId}
                            actionSelectHandler={onSelect}
                            messageSelectHandler={onMessageSelect}
                            actionsFilter={actionsFilter}
                            filterFields={filterFields} 
                            ref={ref => this.elements[action.id] = ref}/>))}
                </div>
            </div> 
        )
    }

    private nextCpHandler = (checkpointMessagesIds: number[], setCheckpointId: (id: number) => any, currentCpIndex: number) => {
        return () => {
            if (currentCpIndex === -1) {
                setCheckpointId(checkpointMessagesIds[0]);
            } else {
                setCheckpointId(checkpointMessagesIds[currentCpIndex + 1] || checkpointMessagesIds[0]);
            }
        }
    }

    private prevCpHandler = (checkpointMessagesIds: number[], setCheckpointId: (id: number) => any, currentCpIndex: number) => {
        return () => {
            if (currentCpIndex === -1) {
                setCheckpointId(checkpointMessagesIds[checkpointMessagesIds.length - 1]);
            } else {
                setCheckpointId(checkpointMessagesIds[currentCpIndex - 1] || checkpointMessagesIds[checkpointMessagesIds.length - 1]);
            }
        }
    }
}   

export const ActionsList = connect((state: AppState) => ({
        actions: state.testCase.actions,
        checkpointMessagesIds: state.testCase.messages.filter(isCheckpoint).map(msg => msg.id),
        selectedActionId: state.selected.actionId,
        selectedMessageId: state.selected.actionId ? null : state.selected.messagesId[0],
        selectedCheckpointId: state.selected.checkpointMessageId,
        actionsFilter: state.actionsFilter,
        filterFields: state.fieldsFilter
    }),
    dispatch => ({
        onSelect: (action: Action) => dispatch(selectAction(action)),
        onMessageSelect: (id: number, status: StatusType) => dispatch(selectMessages([id], status)),
        setCheckpointId: (id: number) => dispatch(selectCheckpoint(id))
    })
)(ActionsListBase);