export function getSecondsPeriod(startTime: number, finishTime: number) {

    if (!startTime || !finishTime) {
        return '';
    }

    const date =  new Date(finishTime * 1000 - startTime * 1000);

    return `${date.getSeconds()}.${date.getMilliseconds()}s`;
}