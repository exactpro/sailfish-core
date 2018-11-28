import ActionParameter from "./ActionParameter"; 
import Status from './Status';
import Verification from './Verification';
import Exception from './Exception';
import MessageAction from './MessageAction';

export type ActionType = 'action' | 'group' | 'message';

export default interface Action {
    id?: number;
    actionNodeType: ActionType;
    bugs: any[];
    name: string;
    description: string;
    parameters?: ActionParameter[];
    relatedMessages: number[];
    verifications?: Verification[];
    logs?: any;
    startTime?: string;
    finishTime?: string;
    status: Status;
    actions?: (Action | MessageAction)[];
}