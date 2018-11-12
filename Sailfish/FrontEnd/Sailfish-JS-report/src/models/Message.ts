export default interface Message {
    uuid: string;
    fields: any;
    rawContent: string;
    type: string;
    relatedActions: Array<string>;
    timestamp: string;
    from: string;
    to: string;
    msgName: string;
    contentHumanReadable: string;
    status?: string;
}