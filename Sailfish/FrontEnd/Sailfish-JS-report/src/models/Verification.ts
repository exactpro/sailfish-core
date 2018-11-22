import Status from './Status';
import Entry from './Entry';

export default interface Verification {
    messageId: number;
    name: string;
    description: string;
    status: Status;
    entries: Entry[];
}