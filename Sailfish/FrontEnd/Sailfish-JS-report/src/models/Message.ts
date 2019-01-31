export default interface Message {
    actionNodeType: string;
    isAdmin: boolean;
    isRejected: boolean;
    id: number;
    checkPoint?: any;
    raw: string;
    relatedActions: number[];
    from: string;
    to: string;
    msgName: string;
    content: string;
    contentHumanReadable: string;
    timestamp: string;
    status?: string;
}