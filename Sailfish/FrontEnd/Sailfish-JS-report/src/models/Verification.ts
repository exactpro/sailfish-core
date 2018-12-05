import Status from './Status';
import Entry from './Entry';
import { ActionType } from './Action';

export default interface Verification {
    actionNodeType: ActionType;
    messageId: number;
    name: string;
    description: string;
    status: Status;
    entries: Entry[];
}