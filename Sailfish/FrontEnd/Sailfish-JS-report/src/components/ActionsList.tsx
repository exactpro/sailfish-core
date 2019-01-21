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
    filterFields: StatusType[];
}

class ActionsListBase extends Component<ListProps, {}> {

    private elements: ActionTree[] = [];

    scrollToAction = (actionId: number) => {
        if (this.elements[actionId]) {
            // smooth behavior is disabled here
            this.elements[actionId].base.scrollIntoView({block: "start"});
        }    
    }

    shouldComponentUpdate(nextProps: ListProps) {
        if (nextProps.filterFields !== this.props.filterFields) {
            return true;
        }

        return nextProps.actions !== this.props.actions ||
            nextProps.selectedActionId !== this.props.selectedActionId ||
            nextProps.selectedMessageId !== this.props.selectedMessageId;
    }

    render({ actions, selectedActionId, selectedMessageId, onSelect, filterFields, onMessageSelect }: ListProps) {
        return (
            <div class="actions-list">
                {actions.map(action => (
                    <ActionTree 
                        action={action}
                        selectedActionId={selectedActionId}
                        selectedMessageId={selectedMessageId}
                        actionSelectHandler={onSelect}
                        messageSelectHandler={onMessageSelect}
                        filterFields={filterFields} 
                        ref={ref => this.elements[action.id] = ref}/>))}
            </div>)
    }
}   

export const ActionsList = connect((state: AppState) => ({
        actions: state.testCase.actions,
        selectedActionId: state.selected.actionId,
        selectedMessageId: state.selected.actionId ? null : state.selected.messagesId[0]
    }),
    dispatch => ({
        onSelect: (action: Action) => dispatch(selectAction(action)),
        onMessageSelect: (id: number, status: StatusType) => dispatch(selectMessages([id], status))
    })
)(ActionsListBase as any);