import Exception from './Exception';
import { StatusType } from './Status';

export default interface Entry {
    name: string;
    actual: string;
    expected: string;
    status?: StatusType;
    precision?: string;
    systemPrecision?: string;
    subEntries?: Entry[];
    exception: any;
}