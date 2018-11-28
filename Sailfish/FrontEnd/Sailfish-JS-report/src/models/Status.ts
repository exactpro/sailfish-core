import Exception from './Exception';

export default interface Status {
    status: string;
    reason?: string;
    details?: string;
    description?: string;
    cause?: Exception;
}