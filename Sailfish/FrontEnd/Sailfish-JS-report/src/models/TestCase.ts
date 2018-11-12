import Action from "./Action";
import Message from "./Message";

export default interface TestCase {
    actions: Array<Action>;
    logs: Array<any>;
    messages: Array<Message>;
    bugs: Array<any>;
    type: string;
    reference: string;
    order: number;
    matrixOrder: number;
    id: number;
    hash: number;
    description: string;
    status: string;
    statusDescription: string;
    startTime: string;
    finishTime: string;
}