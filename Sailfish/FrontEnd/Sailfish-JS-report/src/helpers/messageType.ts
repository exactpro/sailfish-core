import Message from '../models/Message';

export enum MessageType {
    MESSAGE = '',
    CHECKPOINT = 'checkpoint',
    REJECTED = 'rejected',
    ADMIN = 'admin'
}

// FIXME : function should look at the message name in content, not on the session
export function isCheckpoint(message: Message) : boolean {
    return !message.from && !message.to;
}

export function getRejectReason(message: Message) : string {
    const content = JSON.parse(message.content);
    return content['rejectReason'];
}

export function getMessageType(message: Message) : MessageType {
    if (isCheckpoint(message)) {
        return MessageType.CHECKPOINT;
    }

    if (message.isRejected) {
        return MessageType.REJECTED;
    }

    return MessageType.MESSAGE;
}