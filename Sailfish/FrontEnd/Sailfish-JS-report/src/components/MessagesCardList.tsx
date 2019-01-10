import { h, Component } from 'preact';
import Message from '../models/Message';
import MessageCard from './MessageCard';
import '../styles/messages.scss';
import Action from '../models/Action';
import { StatusType, statusValues } from '../models/Status';

interface MessagesListProps {
    messages: Message[];
    selectedMessages: number[];
    actionsMap: Map<number, Action>;
}

export class MessagesCardList extends Component<MessagesListProps, {}> {

    private elements: MessageCard[] = [];

    scrollToMessage(messageId: number) {
        if (this.elements[messageId]) {
            this.elements[messageId].base.scrollIntoView({block: 'center', behavior: 'smooth', inline: 'nearest'});
        }
    }

    getMessageActions(message: Message) : Map<number, Action> {
        return new Map<number, Action>(message.relatedActions.map(
            (actionId) : [number, Action] => [actionId, this.props.actionsMap.get(actionId)]));
    }

    render({ messages, selectedMessages, actionsMap }: MessagesListProps) {
        return (
            <div class="messages-list">
                {messages.map(message => <MessageCard
                            ref={element => this.elements[message.id] = element}
                            message={message}
                            isSelected={selectedMessages.includes(message.id)}
                            key={message.id} 
                            actionsMap={this.getMessageActions(message)}/>)
                }
            </div>
        );
    }
}