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

import beautify from "../../helpers/contentBeautifier";

// WARN: file indent with tabs is important here - some tests using template literals to represent expected result 
describe('beautifier', () => {

    test('Line breaks after ";"', () => {
        const origin = 'header=15; body=14;';

        const result = beautify(origin);

		const expectedResult = 
			`header=15;
			body=14;
			`.replace(/\t/g, '');

        expect(result).toBe(expectedResult);
    })
    
    test('Formatting for simple object', () => {
        const origin = 'header={prop=15; prop2=10};';

        const result = beautify(origin);

		const expectedResult = 
			`header={
			    prop=15;
			    prop2=10
			};
			`.replace(/\t/g, '');

        expect(result).toBe(expectedResult);
    })

    test('Formatting for simple array', () => {
        const origin = '[value1; value2; value3]';

        const result = beautify(origin);

		const expectedResult = 
			`[
			    value1;
			    value2;
			    value3
			]`.replace(/\t/g, '');

        expect(result).toBe(expectedResult);
    })

    test("Formatting for object's array", () => {
        const origin = '[{prop1: value; prop2: value}; {prop3: value; prop4: value}]';

        const result = beautify(origin);

        const expectedResult = 
			`[{
			    prop1: value;
			    prop2: value
			};
			{
			    prop3: value;
			    prop4: value
			}]`.replace(/\t/g, '');

        expect(result).toBe(expectedResult);
    })
    
})
