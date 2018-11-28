import {h} from 'preact';
import Message from '../models/Message';
import MessageCard from './MessageCard';
import '../styles/messages.scss';

interface MessagesListProps {
    messages: Message[];
    selectedMessages: number[];
}

export const MessagesCardList = ({messages, selectedMessages}: MessagesListProps) => {
    return (
        <div class="messages-list">
            {messages.map(message => <MessageCard 
                message={message}
                isSelected={selectedMessages.includes(message.id)}
                key={message.id}/>)}
        </div>
    )
}