export default interface MessageParameter {
    name: string;
    expected?: string;
    actual?: string;
    result?: string;
    parameters?: Array<MessageParameter>;
}