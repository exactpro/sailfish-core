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
  * Returns regexp, that will ignore all special symbols from target string
  * @param str target string
  * @param flags RegExp flags
  */
export function createCaseInsensitiveRegexp(str: string): RegExp {
    const escapedStr = escapeSpecialSymbols(str);

    return new RegExp(escapedStr, 'gi');
}

/**
 * Escapes all special symbols in target string
 * @param str target string
 */
export function escapeSpecialSymbols(str: string): string {
    return str.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, '\\$&');
}

/**
 * Tagged template function for creating RegExp
 * @param regExp 
 * @param options 
 */
export function createRegExp(regExp: TemplateStringsArray, ...options: string[]): RegExp {
    return new RegExp(regExp.raw[0].replace(/\s/gm, ""), options.join(''))
}

/**
 * Converts pattern's string array to RegExp array.
 * @param values pattern values
 * @param flags RegExp flags (by default 'g' and 'i' flags are included)
 */
export function toRegExpArray(values: string[], flags = 'gi'): RegExp[] {
    return values.map(val => new RegExp(escapeSpecialSymbols(val), flags));
}
