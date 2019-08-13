import KnownBug from "./KnownBug";
import { ActionNodeType } from "./Action";

export default interface KnownBugCategory {
    actionNodeType: ActionNodeType;
    name: string;
    subNodes: (KnownBug | KnownBugCategory) [];
}