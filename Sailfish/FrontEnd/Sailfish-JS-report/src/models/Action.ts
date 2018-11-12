import ActionParameter from "./ActionParameter"; 
import MessageParameter from "./MessageParameter";

export default interface Action {
    uuid: string;
    name: string;
    startTime: string;
    description?: string;
    status: string;
    finishTime?: string;
    parameters?: Array<ActionParameter>;
    verifications?: Array<MessageParameter>; 
    relatedMessages?: Array<string>;
}