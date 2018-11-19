import Action from "./Action";
import Message from "./Message";
import Log from "./Log";

export default interface TestCase {
    statuses: any[];
    actions: Action[];
    logs: Log[];
    messages: Message[];
    bugs: any[];
    type: string;
    reference?: any;
    order: number;
    matrixOrder: number;
    id: string;
    hash: number;
    description: string;
    status: string;
    statusDescription: string;
    startTime: string;
    finishTime: string;
}