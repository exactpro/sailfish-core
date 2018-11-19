import TestCase from "./TestCase";

export default interface Report {
    alerts: any[];
    startTime: string;
    finishTime: string;
    plugins: any;
    testCases: TestCase[];
    bugs: any[];
    hostName: string;
    userName: string;
    name: string;
    scriptRunId: number;
    version: string;
    branchName: string;
    description: string;
}