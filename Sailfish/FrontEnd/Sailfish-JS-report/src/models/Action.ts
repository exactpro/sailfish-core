import ActionParameter from "./ActionParameter"; 

export default interface Action {
    Name: string;
    StartTime: string;
    Description?: string;
    Status: {
        Status: string,
        Description?: string
    };
    FinishTime: string;
    InputParameters?: ActionParameter; 
}