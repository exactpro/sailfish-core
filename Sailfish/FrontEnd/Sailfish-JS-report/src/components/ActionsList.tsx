import { h, Component } from 'preact';
import Action from '../models/Action';
import '../styles/action.scss';
import { ActionTree } from './ActionTree';
import { StatusType } from '../models/Status';
import AppState from '../state/AppState';
import { connect } from 'preact-redux';
import { selectAction, selectMessages } from '../actions/actionCreators';

interface ListProps {
    actions: Array<Action>;
    onSelect: (messages: Action) => void;
    onMessageSelect: (id: number, status: StatusType) => void;
    selectedActionId: number;
    selectedMessageId: number;
    actionsFilter: StatusType[];
    filterFields: StatusType[];
}

class ActionsListBase extends Component<ListProps, {}> {

    private elements: ActionTree[] = [];

    scrollToAction = (actionId: number) => {
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

        return nextProps.actions !== this.props.actions ||
            nextProps.selectedActionId !== this.props.selectedActionId ||
            nextProps.selectedMessageId !== this.props.selectedMessageId;
    }

    render({ actions, selectedActionId, selectedMessageId, onSelect, actionsFilter, filterFields, onMessageSelect }: ListProps) {
        return (
            <div class="actions">
                <div class="actions-controls"></div>
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
}   

export const ActionsList = connect((state: AppState) => ({
        actions: state.testCase.actions,
        selectedActionId: state.selected.actionId,
        selectedMessageId: state.selected.actionId ? null : state.selected.messagesId[0],
        actionsFilter: state.actionsFilter
    }),
    dispatch => ({
        onSelect: (action: Action) => dispatch(selectAction(action)),
        onMessageSelect: (id: number, status: StatusType) => dispatch(selectMessages([id], status))
    })
)(ActionsListBase as any);