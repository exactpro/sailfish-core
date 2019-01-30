import { h, Component } from 'preact';
import Message from '../models/Message';
import MessageCard from './MessageCard';
import '../styles/messages.scss';
import Action from '../models/Action';
import { StatusType, statusValues } from '../models/Status';
import { connect } from 'preact-redux';
import AppState from '../state/AppState';
import { generateActionsMap } from '../helpers/mapGenerator';
import { Checkpoint } from './Checkpoint';
import { isCheckpoint } from '../helpers/messageType';

interface MessagesListProps {
    messages: Message[];
    selectedMessages: number[];
    selectedCheckpointId: number;
    actionsMap: Map<number, Action>;
    selectedStatus: StatusType;
}

export class MessagesCardListBase extends Component<MessagesListProps, {}> {

    private elements: MessageCard[] = [];

    componentDidUpdate(prevProps: MessagesListProps) {
        if (prevProps.selectedMessages !== this.props.selectedMessages) {
            this.scrollToMessage(this.props.selectedMessages[0]);
        }

        if (prevProps.selectedCheckpointId !== this.props.selectedCheckpointId) {
            this.scrollToMessage(this.props.selectedCheckpointId);
        }
    }

    scrollToMessage(messageId: number) {
        if (this.elements[messageId]) {
            this.elements[messageId].base.scrollIntoView({block: 'center', behavior: 'smooth', inline: 'nearest'});
        }
    }

    getMessageActions(message: Message) : Map<number, Action> {
        return new Map<number, Action>(message.relatedActions.map(
            (actionId) : [number, Action] => [actionId, this.props.actionsMap.get(actionId)]));
    }

    render({ messages, selectedMessages, selectedStatus, selectedCheckpointId }: MessagesListProps) {
        const checkpoints = messages.filter(isCheckpoint);

        return (
            <div class="messages">
                <div class="messages-control"></div>
                <div class="messages-list">
                    {messages.map(message => checkpoints.includes(message) ? 
                        this.renderCheckpoint(message, checkpoints.indexOf(message) + 1, selectedCheckpointId) : 
                        this.renderMessage(message, selectedMessages, selectedStatus))}
                </div>
            </div>
        );
    }

    private renderMessage(message: Message, selectedMessages: number[], selectedStatus: StatusType) {
        const isSelected = selectedMessages.includes(message.id);

        return (
            <MessageCard
                ref={element => this.elements[message.id] = element}
                message={message}
                isSelected={isSelected}
                status={isSelected ? selectedStatus : null}
                key={message.id} 
                actionsMap={this.getMessageActions(message)}
                />);
    }

    private renderCheckpoint(message: Message, checkpointCount: number, selectedCheckpointId: number) {
        const isSelected = message.id === selectedCheckpointId;

        return (
            <Checkpoint name={message.msgName}
                count={checkpointCount}
                isSelected={isSelected}
                ref={ref => this.elements[message.id] = ref}/>
        )
    }
}

export const MessagesCardList = connect(
    (state: AppState) : MessagesListProps => ({
        messages: state.testCase.messages,
        selectedMessages: state.selected.messagesId,
        selectedCheckpointId: state.selected.checkpointMessageId,
        selectedStatus: state.selected.status,
        actionsMap: generateActionsMap(state.testCase.actions)
    }),
    dispatch => ({})
)(MessagesCardListBase as any);