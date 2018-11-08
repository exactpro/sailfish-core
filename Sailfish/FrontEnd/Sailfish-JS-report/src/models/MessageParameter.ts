export default interface MessageParameter {
    Name: string;
    Expected?: string;
    Actual?: string;
    Result?: string;
    SubParameters?: Array<MessageParameter>;
}