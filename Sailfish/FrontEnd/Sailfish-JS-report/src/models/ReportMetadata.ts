import Status from "./Status";

export interface ReportMetadata {
    startTime: string;
    finishTime: string;
    name: string;
    status: Status;
    id: string;
    hash: number;
    description: string;
    jsonFileName: string;
    jsonpFileName: string;
}