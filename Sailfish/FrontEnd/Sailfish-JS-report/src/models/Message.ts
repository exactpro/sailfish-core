export default interface Message {
    id: number;
    checkPoint?: any;
    raw: string;
    relatedActions?: any;
    from: string;
    to: string;
    msgName: string;
    content: string;
    contentHumanReadable: string;
    timestamp: string;
    status?: string;
}