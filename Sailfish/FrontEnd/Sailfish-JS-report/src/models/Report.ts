import TestCase from "./TestCase";

export default interface Report {
    startTime: string;
    endTime: string;
    plugins: any;
    testCases: Array<TestCase>;
    hostName: string;
    userName: string;
    name: string;
    srciptRunId: string;
    version: string;
    branchName: string;
    description: string;
}