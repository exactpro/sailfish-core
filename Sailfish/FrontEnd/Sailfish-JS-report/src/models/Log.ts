export default interface Log {
    timestamp: string;
    level: string;
    thread: string;
    message: string;
    exception?: any;
    class: string;
}