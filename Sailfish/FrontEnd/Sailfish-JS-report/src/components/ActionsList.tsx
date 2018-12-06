import { h } from 'preact';
import Action from '../models/Action';
import { ActionCard } from './ActionCard';
import '../styles/action.scss';
import { ActionTree } from './ActionTree';
import { StatusType } from '../models/Status';

interface ListProps {
    actions: Array<Action>;
    onSelect: (messages: Action) => void;
    onMessageSelect: (id: number) => void;
    selectedActionId: number;
    selectedMessageId: number;
    filterFields: StatusType[];
}

export const ActionsList = ({ actions, selectedActionId, onSelect, onMessageSelect, selectedMessageId, filterFields }: ListProps) => {
    return (
        <div class="actions-list">
            {actions.map(action => (
                <ActionTree action={action}
                    selectedActionId={selectedActionId}
                    selectedMessageId={selectedMessageId}
                    actionSelectHandler={onSelect}
                    messageSelectHandler={onMessageSelect}
                    filterFields={filterFields}/>))}
        </div>)
}   