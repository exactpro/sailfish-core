export default interface Message {
    uuid: string;
    fields: any;
    rawContent: string;
    type: string;
    relatedActions: Array<string>;
}