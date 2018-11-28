import Action, { ActionType } from './Action';
import Exception from './Exception';

export default interface MessageAction {
    actionNodeType: ActionType;
    message: string;
    level: string;
    exception: Exception;
}