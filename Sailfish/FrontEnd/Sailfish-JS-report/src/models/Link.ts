import {ActionType} from './Action';

export default interface Link {
    actionNodeType: ActionType;
    link: string;
}