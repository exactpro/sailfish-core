/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
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

// support async/await in runtime:
import "regenerator-runtime/runtime";

import FakeWindow from "../mock/FakeWindow.js";
import FakeServer from '../mock/FakeServer.js';

import TheMatrixList from 'state/TheMatrixList.js';
import TheRender from 'actions/TheRender.js';
import TheHelper from 'actions/TheHelper.js';
import TheAppState from 'state/TheAppState.js';
import TheDictManager from 'actions/TheDictManager.js';
import TheStoreManager from 'actions/TheStoreManager.js';
import {AutoCompleteOption} from "../../actions/__models__";

describe ('Autocomplete check', function() {
    var _ = require('lodash');
    var emptyFn =  Function.prototype;

    it('Autocomplete>TestCase> should suggest BlockTypes', function(done) {
        setTimeout(function() {
            let options = TheRender.getAutocomplete(
                TheHelper.createPath('0', 'values', '#action'),
                '#',
                {}, // empty definition
                undefined);

            // refer to TestTools/com.exactpro.sf.aml.AMLBlockBrace
            expect(_.difference(_.map(options.options, (option) => option.value || option), [
                    'Test Case Start',
                    'Global Block start',
                    'Before Test Case Block start',
                    'After Test Case Block start',
                    'Block Start',
                    'First Block Start',
                    'Last Block Start'])).toEqual([]);
            done();
        }, 10);
    });

    it('Autocomplete>Action> should suggest new #fields', function(done) {
        setTimeout(function() {
            let options = TheRender.getAutocomplete(
                TheHelper.createPath('0', 'items', '2', 'values'),
                '#',
                {}, // empty definition
                undefined);

            expect(_.difference(options.options, [
                    '#action', '#message_type', '#dictionary', '#service_name', '#execute', '#id',
                    '#messages_count', '#outcome', '#reference', '#static_type', '#static_value',
                    '#description', '#timeout', '#check_point'])).toEqual([]);
            done();
        }, 10);
    });

    it('Autocomplete>Action> should suggest dictionaries', function(done) {
        setTimeout(function() {
            let options = TheRender.getAutocomplete(
                TheHelper.createPath('0', 'items', '2', 'values', '#dictionary'),
                '',
                {}, // empty definition
                undefined);

            expect(
                _.difference(
                    options.options,
                    Object.keys(TheDictManager.getAllDictionaries())
                )).toEqual([]);
            done();
        }, 10);
    });

    it('Autocomplete>Action> should suggest actions', function(done) {
        setTimeout(function() {
            let options = TheRender.getAutocomplete(
                TheHelper.createPath('0', 'items', '2', 'values', '#action'),
                '',
                {}, // empty definition
                undefined);

            expect(_.findIndex(options.options, new AutoCompleteOption({value: 'receive', help: undefined}))).not.toEqual(-1);
            expect(_.findIndex(options.options, new AutoCompleteOption({value: 'send', help: undefined}))).not.toEqual(-1);
            done();
        }, 10);
    });

    it('Autocomplete>Action> should suggest message type', function(done) {
        setTimeout(function() {
            let options = TheRender.getAutocomplete(
                TheHelper.createPath('0', 'items', '2', 'values', '#message_type'),
                '',
                {}, // empty definition
                undefined);

            expect(_.indexOf(options.options, 'OrderMassCancelReport')).not.toEqual(-1);
            expect(_.indexOf(options.options, 'Statistics')).not.toEqual(-1);
            // and 52 more cases
            done();
        }, 10);
    });

    it('Autocomplete>Action> should suggest fields for message', function(done) {
        setTimeout(function() {
            let options = TheRender.getAutocomplete(
                TheHelper.createPath('0', 'items', '2', 'values'),
                '',
                {}, // empty definition
                undefined);


            expect(_.difference(options.options, [
                    '#id', '#messages_count', '#outcome', '#reference', '#static_type', '#static_value', '#description', '#timeout', '#check_point',
                    'Symbol', 'SecurityID', 'SecurityIDSource', 'NoSecurityAltID', '#action', '#message_type', '#dictionary', '#service_name', '#execute'])).toEqual([]);
            done();
        }, 10);
    });

    it('Autocomplete>Action> should autocomplete references', function(done) {
        setTimeout(function() {
            var options = TheRender.getAutocomplete(
                TheHelper.createPath('0', 'items', '1', 'values', '_any_field_'),
                '${',
                {}, // empty definition
                undefined);

            expect(options.options.length).toEqual(4);
            expect(options.options[0]).toEqual("${ref1}"); // no ref2 and ref3

            options = TheRender.getAutocomplete(
                TheHelper.createPath('0', 'items', '2', 'values', '_any_field_'),
                '${',
                {}, // empty definition
                undefined);

            expect(_.difference(options.options, ["${ref1}", "${ref2}"]).length).toEqual(3); // templates
            done();
        }, 10);
    });

    it('Autocomplete>Action> should autocomplete deep-references', function(done) {
        setTimeout(function() {
            var options = TheRender.getAutocomplete(
                TheHelper.createPath('0', 'items', '1', 'values', '_any_field_'),
                '${ref1.',
                {}, // empty definition
                undefined);

            expect(_.difference(options.options, ["${ref1.NoSecurityAltID}", "${ref1.SecurityIDSource}", "${ref1.SecurityID}", "${ref1.Symbol}"]).length).toEqual(3);


            options = TheRender.getAutocomplete(
                TheHelper.createPath('0', 'items', '1', 'values', '_any_field_'),
                '${ref1.NoSecurityAltID.', // Short form
                {}, // empty definition
                undefined);

            expect(_.difference(options.options, ["${ref1.NoSecurityAltID[0].SecurityAltIDSource}", "${ref1.NoSecurityAltID[0].SecurityAltID}"]).length).toEqual(3);

            options = TheRender.getAutocomplete(
                TheHelper.createPath('0', 'items', '1', 'values', '_any_field_'),
                '${ref1.NoSecurityAltID[1].', // long form
                {}, // empty definition
                undefined);

            expect(_.difference(options.options, ["${ref1.NoSecurityAltID[1].SecurityAltIDSource}", "${ref1.NoSecurityAltID[1].SecurityAltID}"]).length).toEqual(3);
            done();

        }, 10);
    });

    it('Autocomplete>Action> should autocomplete nested references', function(done) {
        setTimeout(function() {
            var options = TheRender.getAutocomplete(
                TheHelper.createPath('0', 'items', '1', 'values', '_any_field_'),
                '#{substr(${ref1.',
                {}, // empty definition
                undefined);

            expect(_.difference(options.options, ["${ref1.NoSecurityAltID}", "${ref1.SecurityIDSource}", "${ref1.SecurityID}", "${ref1.Symbol}"]).length).toEqual(3);
            done();
        }, 10);
    });

});
