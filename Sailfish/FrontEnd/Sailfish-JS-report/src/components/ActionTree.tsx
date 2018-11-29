import {h, Component} from 'preact';
import Action from '../models/Action';
import { ActionTreeProps } from './ActionTree';
import { ActionCard } from './ActionCard';
import MessageAction from '../models/MessageAction';
import '../styles/action.scss';
import ExpandablePanel from './ExpandablePanel';

export interface ActionTreeProps {
    action: Action | MessageAction;
    actionSelectHandler: (action: Action) => void;
    messageSelectHandler: (id: number) => void; 
    selectedMessageId: number;
    selectedActionId: number;
}

export const ActionTree = (props : ActionTreeProps) : JSX.Element => {
    return ActionNode(props);
}

const ActionNode = (props: ActionTreeProps) : JSX.Element => {
    const {actionSelectHandler, messageSelectHandler, selectedActionId, selectedMessageId} = props;
    
    switch (props.action.actionNodeType) {
        case 'action': {
            const action = props.action as Action;
            return (
                <ActionCard action={action}
                    isSelected={action.id === selectedActionId}
                    selectedMessage={selectedMessageId}
                    onMessageSelect={messageSelectHandler}
                    onSelect={actionSelectHandler}>
                    {
                        action.actions ? action.actions.map(action => <ActionNode {...props} action={action}/>) : null
                    }
                </ActionCard>
            );
        }

        case 'group': {
            const action = props.action as Action;
            return (
                <ActionCard action={action}
                    selectedMessage={selectedMessageId}
                    onMessageSelect={messageSelectHandler}>
                    {
                        action.actions ? action.actions.map(action => <ActionNode {...props} action={action}/>) : null
                    }
                </ActionCard>
            );
        }

        case 'message': {
            const {message, level, exception} = props.action as MessageAction;
            
            return (
                <div class="action-card">
                    <ExpandablePanel>
                        <div class="action-card-header">
                            <h3>{message} - {level}</h3>
                        </div>
                        <div class="action-card-body">
                            <p>{exception.stacktrace}</p>
                        </div>
                    </ExpandablePanel>
                </div> 
            )
        }

        default: {
            return null;
        }
    }
}