export default interface Message {
    id: string;
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