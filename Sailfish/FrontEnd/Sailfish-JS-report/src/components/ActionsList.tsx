import { h } from 'preact';
import Action from '../models/Action';
import { ActionCard } from './ActionCard';
import '../styles/action.scss';

interface ListProps {
    actions: Array<Action>;
    onSelect: (id: number) => void;
    selectedActionId: number;
}

export const ActionsList = ({ actions, selectedActionId, onSelect }: ListProps) => {
    return (
        <div class="actions-list">
            {actions.map(action => {
                const className = "card-root " + action.status.status.toLowerCase() + 
                    (action.id === selectedActionId ? " selected" : "");
                return (<div class={className}
                    onClick={e => onSelect(action.id)}>
                    <ActionCard action={action} />
                </div>)
            })}
        </div>)
}