import { h, Component } from 'preact';
import Message from '../models/Message';
import { MessageCard } from './MessageCard';
import '../styles/messages.scss';
import Action from '../models/Action';
import { StatusType, statusValues } from '../models/Status';
import { connect } from 'preact-redux';
import AppState from '../state/AppState';
import { generateActionsMap } from '../helpers/mapGenerator';
import { Checkpoint } from './Checkpoint';
import { isCheckpoint, getRejectReason } from '../helpers/messageType';
import { selectRejectedMessageId } from '../actions/actionCreators';
import { AdminMessageWrapper } from './AdminMessageWrapper';

interface MessagesListProps {
    messages: Message[];
    checkpoints: Message[];
    rejectedMessages: Message[];
    adminMessages: Message[];
    selectedMessages: number[];
    selectedCheckpointId: number;
    selectedRejectedMessageId: number;
    actionsMap: Map<number, Action>;
    selectedStatus: StatusType;
    selectRejectedMessage: (messageId: number) => any;
}

interface MessagesListState {
    adminFilter: boolean;
}

export class MessagesCardListBase extends Component<MessagesListProps, MessagesListState> {

    private elements: MessageCard[] = [];

    constructor(props: MessagesListProps) {
        super(props);

        this.state = {
            adminFilter: false
        }
    }

    componentDidUpdate(prevProps: MessagesListProps) {
        if (prevProps.selectedMessages !== this.props.selectedMessages) {
            this.scrollToMessage(this.props.selectedMessages[0]);
        }

        if (prevProps.selectedCheckpointId !== this.props.selectedCheckpointId) {
            this.scrollToMessage(this.props.selectedCheckpointId);
        }

        if (prevProps.selectedRejectedMessageId !== this.props.selectedRejectedMessageId) {
            this.scrollToMessage(this.props.selectedRejectedMessageId);
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

    render({ messages, rejectedMessages, adminMessages, selectedRejectedMessageId, selectRejectedMessage }: MessagesListProps, { adminFilter }: MessagesListState) {

        const currentRejectedIndex = rejectedMessages.findIndex(msg => msg.id === selectedRejectedMessageId);

        const adminIconClass = [
                "messages-controls-admin-icon",
                adminFilter ? "active" : ""
            ].join(' '),
            adminTitleClass = [
                "messages-controls-admin-title",
                adminFilter ? "active" : ""
            ].join(' ');

        return (
            <div class="messages">
                <div class="messages-controls">
                    {
                        rejectedMessages && rejectedMessages.length ?
                        (
                            <div class="messages-controls-rejected">
                                <div class="messages-controls-rejected-icon"
                                    onClick={() => this.scrollToMessage(selectedRejectedMessageId)}/>
                                <div class="messages-controls-rejected-title">
                                    <p>Rejected</p>
                                </div>
                                <div class="messages-controls-rejected-btn prev"
                                    onClick={this.prevRejectedHandler(rejectedMessages, currentRejectedIndex, selectRejectedMessage)}/>
                                <div class="messages-controls-rejected-count">
                                    <p>{currentRejectedIndex === -1 ? 0 : currentRejectedIndex + 1} of {rejectedMessages.length}</p>
                                </div>
                                <div class="messages-controls-rejected-btn next"
                                    onClick={this.nextRejectedHandler(rejectedMessages, currentRejectedIndex, selectRejectedMessage)}/>
                            </div>
                        )
                        : null
                    }
                    {
                        adminMessages && adminMessages.length ? 
                        (
                            <div class="messages-controls-admin"
                                onClick={this.adminFilterHandler}>
                                <div class={adminIconClass}/>
                                <div class={adminTitleClass}>
                                    <p>Admin Messages</p>
                                </div>
                            </div>
                        )
                        : null
                    }
                </div>
                <div class="messages-list">
                    {messages.map(message => this.renderMessage(message))}
                </div>
            </div>
        );
    }

    private renderMessage(message: Message) {

        const {selectedMessages, selectedStatus, checkpoints, rejectedMessages, selectedCheckpointId, selectRejectedMessage, selectedRejectedMessageId} = this.props;

        if (checkpoints.includes(message)) {
            return this.renderCheckpoint(message, checkpoints, selectedCheckpointId)
        }

        if (message.isAdmin) {
            return (
                <AdminMessageWrapper
                    ref={ref => this.elements[message.id] = ref}
                    message={message}
                    key={message.id}
                    actionsMap={this.getMessageActions(message)}
                    isExpanded={this.state.adminFilter}
                    isSelected={selectedRejectedMessageId === message.id}/>
            )
        }

        if (rejectedMessages.includes(message)) {
            return this.renderRejected(message, rejectedMessages, selectedRejectedMessageId);
        }

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

    private renderCheckpoint(message: Message, checkpoints: Message[], selectedCheckpointId: number) {
        const isSelected = message.id === selectedCheckpointId,
            checkpointCount = checkpoints.indexOf(message) + 1;

        return (
            <Checkpoint name={message.msgName}
                count={checkpointCount}
                isSelected={isSelected}
                ref={ref => this.elements[message.id] = ref}/>
        )
    }

    private renderRejected(message: Message, rejectedMessages: Message[], selectedRejectedMessageId: number) {
        const isSelected = message.id === selectedRejectedMessageId,
            rejectedCount = rejectedMessages.indexOf(message) + 1;

        return (
            <MessageCard
                ref={element => this.elements[message.id] = element}
                message={message}
                isSelected={isSelected}
                key={message.id} 
                actionsMap={this.getMessageActions(message)}
                rejectedMessagesCount={rejectedCount}/>
        )
    }

    private nextRejectedHandler = (rejectedMessages: Message[], currentRejectedIndex: number, selectRejectedHandler: (id: number) => any) => {
        return () => {
            if (currentRejectedIndex === -1) {
                selectRejectedHandler(rejectedMessages[0].id);
            } else {
                selectRejectedHandler((rejectedMessages[currentRejectedIndex + 1] || rejectedMessages[0]).id)
            }
        }
    }

    private prevRejectedHandler = (rejectedMessages: Message[], currentRejectedIndex: number, selectRejectedMessage: (id: number) => any) => {
        return () => {
            if (currentRejectedIndex === -1) {
                selectRejectedMessage(rejectedMessages[rejectedMessages.length - 1].id);
            } else {
                selectRejectedMessage((rejectedMessages[currentRejectedIndex - 1] || rejectedMessages[rejectedMessages.length - 1]).id)
            }
        }
    }

    private adminFilterHandler = () => {
        this.setState({
            adminFilter: !this.state.adminFilter
        })
    }
}

export const MessagesCardList = connect(
    (state: AppState) => ({
        messages: state.testCase.messages,
        checkpoints: state.testCase.messages.filter(isCheckpoint),
        rejectedMessages: state.testCase.messages.filter(message => message.isRejected),
        adminMessages: state.testCase.messages.filter(message => message.isAdmin),
        selectedMessages: state.selected.messagesId,
        selectedCheckpointId: state.selected.checkpointMessageId,
        selectedRejectedMessageId: state.selected.rejectedMessageId,
        selectedStatus: state.selected.status,
        actionsMap: generateActionsMap(state.testCase.actions)
    }),
    dispatch => ({
        selectRejectedMessage: (messageId: number) => dispatch(selectRejectedMessageId(messageId))
    })
)(MessagesCardListBase as any);