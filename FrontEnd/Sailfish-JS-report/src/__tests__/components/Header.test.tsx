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

import * as React from 'react';
import { shallow } from 'enzyme';
import { HeaderBase } from '../../components/Header';
import { createTestCase } from '../util/creators';

describe('[React] <HeaderBase/>', () => {

    // Component's props type
    const defaultProps: Parameters<typeof HeaderBase>[0] = {
        prevTestCaseHandler: jest.fn(),
        nextTestCaseHandler: jest.fn(),
        testCase: createTestCase(),
        backToListHandler: jest.fn(),
        isNavigationEnabled: true,
        isFilterApplied: false,
        isMessageFilterApplied: false,
        isFilterHighlighted: false,
        filterResultsCount: 0,
        messages: []
    };

    test('TestCase nav button click', () => {
        const prevHandlerMock = jest.fn(),
            nextHandlerMock = jest.fn();

        const wrapper = shallow(
            <HeaderBase 
                {...defaultProps}
                prevTestCaseHandler={prevHandlerMock}
                nextTestCaseHandler={nextHandlerMock}
                isNavigationEnabled={true}/>
        );

        const elements = wrapper.find('.header-button');
        
        elements.forEach(btn => btn.simulate('click'));

        expect(prevHandlerMock).toHaveBeenCalled();
        expect(nextHandlerMock).toHaveBeenCalled();
    })
    
    test('Disabled TestCase nav button click', () => {
        const prevHandlerMock = jest.fn(),
            nextHandlerMock = jest.fn();

        const wrapper = shallow(
            <HeaderBase 
                {...defaultProps}
                prevTestCaseHandler={prevHandlerMock}
                nextTestCaseHandler={nextHandlerMock}
                isNavigationEnabled={false}/>
        );

        const elements = wrapper.find('.header-button');

        
        elements.forEach(btn => btn.simulate('click'));

        expect(prevHandlerMock).not.toHaveBeenCalled();
        expect(nextHandlerMock).not.toHaveBeenCalled();
    })
})

