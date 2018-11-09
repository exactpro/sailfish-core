export default interface IActionParameter {
    name: string;
    parameters?: Array<IActionParameter>;
    value?: string;
}