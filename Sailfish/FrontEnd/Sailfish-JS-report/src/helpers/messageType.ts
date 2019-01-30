import Message from '../models/Message';

// FIXME : function should look at the message name in content, not on the session
export function isCheckpoint(message: Message) : boolean {
    return !message.from && !message.to;
}