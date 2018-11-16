import {h} from 'preact';
import Message from '../models/Message';
import {MessageCard} from './MessageCard';
import '../styles/messages.scss';

interface MessagesListProps {
    messages: Array<Message>;
    selectedActionId: string;
}

export const MessagesCardList = ({messages, selectedActionId}: MessagesListProps) => {
    return (
        <div class="messages-list">
            {messages.map(message => <MessageCard 
                message={message}
                isSelected={message.relatedActions.includes(selectedActionId)}/>)}
        </div>
    )
}