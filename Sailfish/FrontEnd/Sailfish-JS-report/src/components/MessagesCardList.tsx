import { h, Component } from 'preact';
import Message from '../models/Message';
import MessageCard from './MessageCard';
import '../styles/messages.scss';

interface MessagesListProps {
    messages: Message[];
    selectedMessages: number[];
}

export class MessagesCardList extends Component<MessagesListProps, {}> {

    private elements: MessageCard[] = [];

    scrollToMessage = (messageId: number) => {
        if (this.elements[messageId]) {
            this.elements[messageId].base.scrollIntoView({block: 'center', behavior: 'smooth', inline: 'nearest'});
        }
    }

    render({ messages, selectedMessages }: MessagesListProps) {
        return (
            <div class="messages-list">
                {messages.map(message => <MessageCard
                    ref={element => this.elements[message.id] = element}
                    message={message}
                    isSelected={selectedMessages.includes(message.id)}
                    key={message.id} />)}
            </div>
        );
    }
}