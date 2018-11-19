export default interface IActionParameter {
    name: string;
    subParameters?: Array<IActionParameter>;
    value?: string;
}