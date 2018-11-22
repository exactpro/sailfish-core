import ActionParameter from "./ActionParameter"; 
import Status from './Status';
import Verification from './Verification';

export default interface Action {
    id: number;
    bugs: any[];
    name: string;
    description: string;
    parameters: ActionParameter[];
    relatedMessages: number[];
    verifications?: Verification[];
    subActions?: Action[];
    logs?: any;
    startTime: string;
    finishTime: string;
    status: Status;
    subNodes?: any;
}