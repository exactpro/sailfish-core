import ActionParameter from "./ActionParameter"; 

export default interface Action {
    Name: string;
    StartTime: string;
    Description?: string;
    Status: string;
    FinishTime: string;
    InputParameters?: ActionParameter; 
}