import ActionParameter from "./ActionParameter"; 
import Status from './Status';
import Verification from './Verification';
import Exception from './Exception';
import MessageAction from './MessageAction';
import Link from './Link';

export type ActionType = 'action' | 'verification' | 'message' | 'link';

export type ActionNode = Action | MessageAction | Verification | Link;

export default interface Action {
    id?: number;
    actionNodeType: ActionType;
    bugs: any[];
    name: string;
    description: string;
    parameters?: ActionParameter[];
    relatedMessages: number[];
    logs?: any;
    startTime?: number;
    finishTime?: number;
    status: Status;
    subNodes?: ActionNode[];
    checkPointId?: number;
}