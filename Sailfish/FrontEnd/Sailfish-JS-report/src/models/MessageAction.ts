import Action, { ActionType } from './Action';
import Exception from './Exception';

export type MessageTextColor =  'BLACK' | 'BLUE' | 'RED' | 'ORANGE' | 'GRAY';

export type MessageTextStyle = 'BOLD' | 'NORMAL' | 'ITALIC';

export default interface MessageAction {
    message: string;
    color: string;
    style: string;
    level?: any;
    exception?: any;
    actionNodeType: string;
}