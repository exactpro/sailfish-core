/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
 *  limitations under the License.
 ******************************************************************************/

import * as React from 'react';
import {
    SearchInputBase,
    Props as SearchInputProps,
    REACTIVE_SEARCH_DELAY
} from "../../../components/search/SearchInput";
import SearchResult from "../../../helpers/search/SearchResult";
import { mount } from "enzyme";
import SearchToken from "../../../models/search/SearchToken";
import KeyCodes from "../../../util/KeyCodes";
import { timer } from "../../util/timer";
import * as SearchPanelsControlModule from '../../../components/search/SearchPanelsControl';

// we need to mock this import because SearchPanelControl's Connect wrapper uses redux context
(SearchPanelsControlModule as any).default = () => null;

describe('[React] <SearchInput/>', () => {

    const defaultProps: SearchInputProps = {
        searchTokens: [],
        currentIndex: -1,
        resultsCount: 0,
        searchResults: new SearchResult(),
        updateSearchTokens: () => {
        },
        nextSearchResult: () => {
        },
        prevSearchResult: () => {
        },
        clear: () => {
        }
    };

    test('Should render input on click', () => {

        const wrapper = mount(
            <SearchInputBase
                {...defaultProps}
            />
        );

        expect(wrapper.find('input').length).toEqual(0);

        const field = wrapper.find('.search-field');
        field.simulate('click', { target: field.getDOMNode() });

        expect(wrapper.find('input').length).toEqual(1);
    });

    test('Reactive delay test', (done) => {
        const updateMock = jest.fn();

        const wrapper = mount(
            <SearchInputBase
                {...defaultProps}
                updateSearchTokens={updateMock}
            />
        );


        const field = wrapper.find('.search-field');
        field.simulate('click', { target: field.getDOMNode() });

        const input = wrapper.find('input');
        input.simulate('change', { target: { value: 'test' } });

        expect(updateMock.mock.calls.length).toEqual(0);

        setTimeout(() => {
            expect(updateMock).toHaveBeenCalled();

            const tokens: SearchToken[] = updateMock.mock.calls[0][0];
            expect(tokens[0].pattern).toEqual('test');
            done()
        }, REACTIVE_SEARCH_DELAY);
    });

    test('Submit by SPACE key down', () => {
        const updateMock = jest.fn();

        const wrapper = mount(
            <SearchInputBase
                {...defaultProps}
                updateSearchTokens={updateMock}
            />
        );

        const field = wrapper.find('.search-field');
        field.simulate('click', { target: field.getDOMNode() });

        const input = wrapper.find('input');
        input.simulate('change', { target: { value: 'test' } });

        expect(updateMock).not.toHaveBeenCalled();

        input.simulate('keydown', { keyCode: KeyCodes.SPACE });

        expect(updateMock).toHaveBeenCalled();

        const tokens: SearchToken[] = updateMock.mock.calls[0][0];
        expect(tokens[0]?.pattern).toEqual('test');
    });

    test('Submit by reactive delay, change input and after that submit by space', async () => {
        const updateMock = jest.fn();

        const wrapper = mount(
            <SearchInputBase
                {...defaultProps}
                updateSearchTokens={updateMock}
            />
        );

        const field = wrapper.find('.search-field');
        field.simulate('click', { target: field.getDOMNode() });

        const input = wrapper.find('input');
        input.simulate('change', { target: { value: 'test' } });

        expect(updateMock).not.toHaveBeenCalled();

        await timer(REACTIVE_SEARCH_DELAY);

        expect(updateMock).toHaveBeenCalled();

        const reactiveTokens: SearchToken[] = updateMock.mock.calls[0][0];
        expect(reactiveTokens[0]?.pattern).toEqual('test');
        expect(reactiveTokens[0]?.isActive).toEqual(true);

        wrapper.setProps({
            searchTokens: reactiveTokens
        });

        input.simulate('change', { target: { value: 'testsubmit' } });
        input.simulate('keydown', { keyCode: KeyCodes.SPACE });

        expect(updateMock.mock.calls.length).toEqual(2);

        const submittedTokens: SearchToken[] = updateMock.mock.calls[1][0];
        expect(submittedTokens[0]?.pattern).toEqual('testsubmit');
        expect(submittedTokens[0]?.isActive).toEqual(false);
    });
});
