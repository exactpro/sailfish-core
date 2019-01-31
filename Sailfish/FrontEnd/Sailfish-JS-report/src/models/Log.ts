export default interface Log {
    actionNodeType: string;
    timestamp: number;
    level: string;
    thread: string;
    message: string;
    exception?: any;
    class: string;
}