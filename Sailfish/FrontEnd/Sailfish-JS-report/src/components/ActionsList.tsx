import { h } from 'preact';
import Action from '../models/Action';
import { ActionCard } from './ActionCard';
import '../styles/action.scss';

interface ListProps {
    actions: Array<Action>;
    onSelect: (messages: Action) => void;
    onMessageSelect: (id: number) => void;
    selectedActionId: number;
    selectedStatus?: string;
    selectedMessage: number;
}

export const ActionsList = ({ actions, selectedActionId, onSelect, onMessageSelect, selectedMessage }: ListProps) => {
    return (
        <div class="actions-list">
            {actions.map(action => {
                const className = ["action-card", action.status.status.toLowerCase(), 
                    (action.id === selectedActionId ? "selected" : "")].join(' ');
                return (<div class={className}
                    onClick={e => onSelect(action)}
                    key={action.id}>
                    <ActionCard action={action}
                        onMessageSelect={id => onMessageSelect(id)}
                        selectedMessage={selectedMessage}/>
                </div>)
            })}
        </div>)
}