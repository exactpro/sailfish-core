import { StatusType } from "../models/Status";

export default interface SelectedState {
    actionId: number;
    messagesId: number[];
    status: StatusType;
}