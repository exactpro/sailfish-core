export function getSecondsPeriod(startTime: string, finishTime: string) {

    if (!startTime || !finishTime) {
        return '';
    }

    const date =  new Date(new Date(finishTime).getTime() - new Date(startTime).getTime());

    return `${date.getSeconds()}.${date.getMilliseconds()}s`;
}