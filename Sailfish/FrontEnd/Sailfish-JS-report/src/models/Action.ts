import ActionParameter from "./ActionParameter"; 
import MessageParameter from "./MessageParameter";

export default interface Action {
    id: number;
    bugs: any[];
    name: string;
    description: string;
    parameters: ActionParameter[];
    relatedMessages: number[];
    verifications?: any;
    subActions?: Action[];
    logs?: any;
    startTime: string;
    finishTime: string;
    status: {
        status: string;
        reason: string;
        details?: any;
    };
    subNodes?: any;
}