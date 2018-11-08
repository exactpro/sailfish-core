import ActionParameter from "./ActionParameter"; 
import MessageParameter from "./MessageParameter";

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
    ComparsionParameters?: Array<MessageParameter>; 
}