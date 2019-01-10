import Exception from './Exception';

export type StatusType = 'PASSED' | 'FAILED' | 'CONDITIONALLY_PASSED' | 'NA' | 'SKIPPED' | 'CONDITIONALLY_FAILED';

export const statusValues : StatusType[] = ['PASSED', 'FAILED', 'CONDITIONALLY_PASSED', 'NA', 'SKIPPED', 'CONDITIONALLY_FAILED'];

export default interface Status {
    status: StatusType;
    reason?: string;
    details?: string;
    description?: string;
    cause?: Exception;
}