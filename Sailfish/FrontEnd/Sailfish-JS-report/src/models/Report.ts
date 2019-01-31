import TestCase from "./TestCase";

export default interface Report {
    alerts?: any[];
    startTime: number;
    finishTime: number;
    plugins: any;
    testCases?: TestCase[];
    bugs: any[];
    hostName: string;
    userName: string;
    name: string;
    scriptRunId: number;
    version: string;
    branchName: string;
    description: string;
    exception?: string;
    outcomes?: any;
    testCaseLinks?: string[];
    reportProperties?: any;
}

export function isReport(report: Report | TestCase): report is Report {
    return (<Report>report).testCaseLinks !== undefined;
}