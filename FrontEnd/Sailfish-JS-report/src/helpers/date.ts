/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

export function getSecondsPeriod(startTime: string | Date, finishTime: string | Date, withMiliseconds: boolean = true) {

    if (!startTime || !finishTime) {
        return '';
    }

    const diffDate =  new Date(toDate(finishTime).getTime() - toDate(startTime).getTime()),
        seconds = (diffDate.getTime() / 1000).toFixed().toString(),
        miliseconds = diffDate.getMilliseconds().toString(),
        milisecondsFormatted = miliseconds !== '0' ? miliseconds.padStart(3, '0') : miliseconds;

    return withMiliseconds ? 
        `${seconds}.${milisecondsFormatted}s` :
        `${seconds}s`;
}

export function formatTime(time: string) {
    if (time == null) {
        return '';
    }

    return new Date(time).toISOString().replace('T', ' ').replace('Z', '');
}

export function isDateEqual(first: string | Date, second: string | Date): boolean {
    return toDate(first).getTime() === toDate(second).getTime();
}

function toDate(date: string | Date): Date {
    return typeof date === 'string' ? new Date(date) : date;
}
