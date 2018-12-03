import Exception from './Exception';

export type StatusType = 'PASSED' | 'FAILED' | 'CONDITIONALLY_PASSED' | 'N/A';

export default interface Status {
    status: StatusType;
    reason?: string;
    details?: string;
    description?: string;
    cause?: Exception;
}