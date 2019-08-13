import { ActionNodeType } from "./Action";
import { KnownBugStatus } from "./KnownBugStatus";

export default interface KnownBug {
    actionNodeType: ActionNodeType;
    status: KnownBugStatus;
    subject: string;
}