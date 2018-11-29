import { h } from 'preact';
import Action from '../models/Action';
import { ActionCard } from './ActionCard';
import '../styles/action.scss';
import { ActionTree } from './ActionTree';

interface ListProps {
    actions: Array<Action>;
    onSelect: (messages: Action) => void;
    onMessageSelect: (id: number) => void;
    selectedActionId: number;
    selectedMessageId: number;
}

export const ActionsList = ({ actions, selectedActionId, onSelect, onMessageSelect, selectedMessageId }: ListProps) => {
    return (
        <div class="actions-list">
            {actions.map(action => (
                <ActionTree action={action}
                    selectedActionId={selectedActionId}
                    selectedMessageId={selectedMessageId}
                    actionSelectHandler={onSelect}
                    messageSelectHandler={onMessageSelect}/>))}
        </div>)
}   