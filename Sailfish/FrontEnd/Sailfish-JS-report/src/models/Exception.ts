export default interface Exception {
    message: string;
    cause?: any;
    stacktrace?: string;
}