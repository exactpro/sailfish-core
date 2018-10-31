export default interface IActionParameter {
    Name: string;
    SubParameters?: Array<IActionParameter>;
    Value?: string;
}