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

/**
 * This function can be used to save target function call results, it use Map to save it.
 * The key in Map is calculating using target function parameters through key mapper funcitons.
 * @param fn target function
 * @param keyMappers optional key mapper for each target fn argument
 */
function memoize<F extends (...args: any[]) => any>(fn: F, ...keyMappers: ((obj: any) => string)[]): F {
    const argsMap = new Map<string, any>();

    return ((...args: any[]) => {
        const key = args.reduce((key, arg, index) =>  key + '-' + (keyMappers[index] ? keyMappers[index](arg) : arg.toString()), '');

        if (argsMap.has(key)) {
            return argsMap.get(key);
        }

        const result = fn(...args);
        argsMap.set(key, result);

        return result;
    }) as F;
}

export default memoize;
