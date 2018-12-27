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
import React from 'react';
import {findDOMNode} from 'react-dom';
import Fuse from 'fuse.js';

import {AutoCompleteOption} from 'actions/__models__.js';
import TheHelper from 'actions/TheHelper.js';
import TheRender from 'actions/TheRender';
import {baseContextTypes} from 'contextFactory';

const MAX_DROPDOWN_HEIGHT = 150;
const MAX_DROPDOWN_HEIGHT_PX = `${MAX_DROPDOWN_HEIGHT}px`;

const emptyFn = function() {};

// wrapper to work with div.contentEditable (it was buggy in FF)
// based on http://stackoverflow.com/a/27255103
const ContentEditable = React.createClass({
    propTypes: {
        value: React.PropTypes.string.isRequired,
        contentEditable: React.PropTypes.bool,
        onInput: React.PropTypes.func
    },

    render: function(){
        var {hasFocus, contentEditable, ...rest} = this.props;
        var contentEditable = typeof contentEditable === 'boolean' ? contentEditable : true;

        return <div
            contentEditable={contentEditable}
            {...rest}
            ref="value"
            onInput={this.handleInput}
            dangerouslySetInnerHTML={{__html: this.props.value}}></div>;
    },

    shouldComponentUpdate: function(nextProps){
        return (
            (nextProps.value !== findDOMNode(this).innerHTML)
            || (this.props.contentEditable !== nextProps.contentEditable)
            || (this.props['data-autotext'] !== nextProps['data-autotext'])
        );
    },

    componentDidUpdate: function() {
        if ( this.props.html !== findDOMNode(this).innerHTML ) {
            findDOMNode(this).innerHTML = this.props.value;
            if (this.props.hasFocus)
                this.__endCaret();
        }
    },

    __endCaret: function() {
        const sel = window.getSelection();
        var range = document.createRange();
        if (this.refs.value.childNodes[0]) {
            range.setStart(this.refs.value.childNodes[0], this.refs.value.childNodes[0].length)
            range.collapse(true);
            sel.removeAllRanges();
            sel.addRange(range);
            this.focus();
            this.handleInput();
        }
    },

    handleInput: function(){
        var html = TheHelper.stripText(findDOMNode(this).innerHTML);
        var fireEvent = this.props.onInput && html !== this.lastHtml;
        this.lastHtml = html;

        if (fireEvent) {
            this.props.onInput(html);
        }
    },

    getValue: function() {
        return this.lastHtml;
    },

    focus: function() {
        this.refs.value.focus();
    },

    // based on http://stackoverflow.com/a/3976125
    _getCaretPosition: function() {
        //if (window.getSelection) { //FF,Chrome,Opera 9+,Safari,IE9+ // we dont support old browsers
        if (this.refs && this.refs.value) { // component can be unmounted
            const sel = window.getSelection();
            if (sel.rangeCount > 0) {
                const editableDiv = this.refs.value;
                const range = sel.getRangeAt(0);
                if (range.commonAncestorContainer.parentNode === editableDiv) {
                    return range.endOffset;
                }
            }
        }
        return 0;
    }
});

const AutoInput = React.createClass({
    displayName: 'AutoInput',

    contextTypes: baseContextTypes,

    propTypes: {
        path: React.PropTypes.string.isRequired,
        value: React.PropTypes.string,
        definition: React.PropTypes.object,
        isEditing: React.PropTypes.bool.isRequired,
        className: React.PropTypes.string,
        valueOnBlur: React.PropTypes.bool,
        // signature:
        //  onValue(newValue)
        onValue: React.PropTypes.func
    },

    getDefaultProps: function() {
        return {
            className: '',
            onValue: emptyFn,
            valueOnBlur: true,
            value: undefined,
            definition: undefined
        };
    },

    /**
     *
     * @param value
     * @param {bool} isEditing
     * @param {number} selectedIdx
     * @param {bool} hasFocus
     * @returns {{
            value: *,
            selectedIdx: number,
            options: Array[string],
            autotext: string,
            autoCompleteStart: number,
            autoCompleteEmd: number
        }}
     * @private
     */
    _getState: function(value = '', isEditing = false, selectedIdx = -1, hasFocus = false) {
        value = (value === undefined || value === null) ? '' : '' + value;
        const options = isEditing ? this._getOptions(value) : { options : [], start: -1, end: -1};
        const autotext = this._getAutotext(value, options, selectedIdx);
        const _hasFocus = isEditing || hasFocus;
        return {
            value: value,
            // index of option focused in OptionList
            selectedIdx: selectedIdx,
            // list of possible options
            options: options.options,
            autotext: autotext,
            hasFocus: _hasFocus,
            autoCompleteStart: options.start,
            autoCompleteEnd: options.end
        };
    },

    getInitialState: function() {
        return this._getState(this.props.value, this.props.isEditing);
    },

    _stopEdit: function(newValue) {
        newValue = newValue !== undefined ? newValue : this.props.value;
        this.setState({
            options: [],
            value: newValue,
            autotext: '',
            hasFocus: false,
            autoCompleteStart: -1,
            autoCompleteEnd: -1
        });
        if (this.props.isEditing) {
            this.props.onValue(newValue);
        }
    },

    getValue: function() {
        return this.state.value;
    },

    focus: function() {
        this.setState(
            {hasFocus: true},
            () => this.refs.value.focus()
        );
    },

    _onKeyDown: function(event) {
        var action = {
            32: '_showOptions', // Ctrl+Space
            38: '_selectPrevious',//up
            40: '_selectNext',//down
            27: '_hideOnEscape',//esc
            9: '_confirmValue', // TAB
            13: '_confirmValue',  // Enter
            37: '_updateAutocomplete', // left arrows
            39: '_updateAutocomplete' // left arrows
        } [event.keyCode];

        if (action && this[action](event)) {
            event.preventDefault();
        }
    },

    _selectByIndex: function(newIndex) {
        this.setState({
            selectedIdx: newIndex
        });
    },

    _showOptions: function(event) {
        if (!event.ctrlKey) {
            // handle as ' ' (Space)
            return false;
        }

        // don't show Options if they is already visible
        const numberOfOptions = this.state.options.length;
        const expanded = numberOfOptions !== 0;
        if (expanded) {
            return true;
        }

        this.setState( this._getState(this.props.value, this.props.isEditing) );
        return true; //prevent default
    },

    _selectPrevious: function(event) {
        const numberOfOptions = this.state.options.length;
        const expanded = numberOfOptions !== 0;

        if (!expanded || this.state.selectedIdx === -1) {
            return;
        }

        const newIndex = this.state.selectedIdx - 1;

        this.setState({
            selectedIdx: newIndex,
            autotext: this._getAutotext(this.state.value, this.state.options, newIndex)
        }, () => {
            this._scrollIntoView(newIndex);
        });

        return true; // preventDefault
    },

    _selectNext: function(event) {
        const numberOfOptions = this.state.options.length;
        const expanded = numberOfOptions !== 0;

        if (!expanded) {
            this._updateAutocomplete();
            return;
        }

        var newIndex = this.state.selectedIdx + 1;
        if (newIndex >= numberOfOptions) {
            newIndex = numberOfOptions-1;
        }

        this.setState({
            selectedIdx: newIndex,
            autotext: this._getAutotext(this.state.value, this.state.options, newIndex)
        }, () => {
            this._scrollIntoView(newIndex);
        });

        return true; // preventDefault
    },

    _scrollIntoView: function(optionIdx) {
        if (optionIdx === -1) {
            return;
        }
        const item = findDOMNode(this.refs.options).childNodes[optionIdx];
        const parent = item.parentNode;
        const parentRect = parent.getBoundingClientRect();
        const itemRect = item.getBoundingClientRect();
        if (itemRect.top < parentRect.top) {
            parent.scrollTop += itemRect.top - parentRect.top;
        } else if (itemRect.bottom > parentRect.bottom) {
            parent.scrollTop += itemRect.bottom - parentRect.bottom;
        }
    },

    _hideOnEscape: function() {
        this._stopEdit(); // cancel edit
        return true; // preventDefault
    },

    _confirmValue: function() {
        var option, newValue;

        if (this.state.options.length > 0 && this.state.selectedIdx != -1) {
            option = this.state.options[this.state.selectedIdx];
            newValue = option.brace || option.value || option;

            let start = this.state.autoCompleteStart;
            let end = this.state.autoCompleteEnd;
            if (start !== -1 && end !== -1) {
                newValue = this.state.value.substring(0, start) + newValue;
            }
        } else  {
            newValue = this.state.value;
        }
        if (option && option.isTemplate) {
            this.setState(this._getState(newValue, true, -1));
        } else {
            this._stopEdit(newValue);
        }
        return true; // preventDefault
    },

    _updateAutocomplete: function() {
        const selectedIdx = -1;
        this.setState( this._getState(this.state.value, true, selectedIdx) );
        return false; // don't preventDefault
    },

    handleInput: function(innerHtml) {
        const selectedIdx = -1;
        const newValue = this.refs.value.getValue();

        this.setState( this._getState(newValue, true, selectedIdx));
    },

    handleBlur: function(e) {
        const numberOfOptions = this.state.options.length;
        const idx = this.state.selectedIdx;

        var newValue = undefined;
        if (idx >=0 && idx < numberOfOptions) {
            newValue = this.state.options[this.state.selectedIdx];
            newValue = newValue.value || newValue;

            let start = this.state.autoCompleteStart;
            let end = this.state.autoCompleteEnd;
            if (start !== -1 && end !== -1) {
                newValue = this.state.value.substring(0, start) + newValue + this.state.value.substring(end);
            }
        }
        this._stopEdit(newValue); // cancel edit
    },

    /**
     *
     * @param value
     * @param options
     * @param selectedIdx
     * @returns {string}
     * @private
     */
    _getAutotext: function(value, options, selectedIdx) {
        const selectedOption = options[selectedIdx];

        if (selectedOption) {
            const selectedOptionStr = selectedOption.value || selectedOption;
            if (selectedOptionStr.indexOf(value) === 0) {
                return selectedOptionStr.substr(value.length);
            }
        }

        return '';
    },

    _onSelectFromList: function(newValue) {
        // This method will not called: ReactJS re-render component before event 'click' being handled.
        // re-render is scheduled after setState in 'onBlur'
        this._stopEdit(newValue);
    },

    _filterAutoComplete: function(options, value) {
        const result = Object.assign({}, options);

        if (value) {
            const maxPatternLength = 32;
            // Please note that Fuse 2.2 assumes that if the first item's tyoe is 'string'
            // than it will consider that all items are strings.
            // otherwise it will search using fuse_options.keys
            //
            // so, we maps all object to strings:
            const optionsStr = options.options.map((value) => {
                if (value instanceof AutoCompleteOption) {
                    return value.value;
                }
                return value;
            });

            const fuse = new Fuse(optionsStr, {
                maxPatternLength: maxPatternLength
            });

            const searchString = value.substring(options.start, options.end).substring(0, maxPatternLength);
            result.options = fuse.search(searchString).map(idx => options.options[idx]);
        }

        return result;
    },

    _getOptions: function(newValue) {
        const props = this.props;
        const caretPos = (this.refs && this.refs.value) ? this.refs.value._getCaretPosition() : 0; // Actually this is previous position
        let autoComplete = { start: -1, end: -1, options: [] };
        try {
            autoComplete = TheRender.getAutocomplete(props.path, newValue, props.definition, caretPos);
        } catch (e) {
            TheHelper.logError('Failed to autocomplete \'' + newValue + '\' ' + e.message, e);
        }
        return this._filterAutoComplete(autoComplete, newValue);
    },

    componentWillReceiveProps: function(nextProps) {
        if (nextProps.value !== this.props.value || nextProps.isEditing !== this.props.isEditing) {
            const newState = this._getState(nextProps.value, nextProps.isEditing);
            //collapse dropdown
            newState.options = [];
            this.setState(newState);
        }
    },

    render: function () {
        const state = this.state;
        const isEditing = this.props.isEditing;
        const hasFocus = isEditing || this.state.hasFocus;
        let optionsList = null;
        const options = state.options;
        const hasOptions = isEditing && options.length > 0;
        if (hasOptions) {

            let helpDiv = null;
            const hasHelp = hasOptions && state.selectedIdx != -1;
            if (hasHelp) {
                const selectedOption = options[state.selectedIdx];
                const helpContent = selectedOption.help || undefined;
                if (helpContent) {
                    helpDiv = (
                        <div className="exa-auto-help-ct" style={{
                                'position': 'relative',
                                'height': 0,
                                'overflow': 'visible'
                            }}>
                            <div className="exa-auto-help"
                                ref='help'
                                dangerouslySetInnerHTML={{ __html: helpContent }}
                                style={{
                                    'width': '100%',
                                    'maxHeight': MAX_DROPDOWN_HEIGHT_PX,
                                    'overflowY': 'auto',
                                    'overflowX': 'hidden',
                                    'textOverflow': 'ellipsis',
                                    'backgroundColor': 'khaki',
                                    'padding': '7px 5px 7px 5px'
                                }}>
                            </div>
                        </div>
                    );
                }
            }

            optionsList = (
                <div className="exa-auto-options-ct" style={{
                    'position': 'relative',
                    'height': 0,
                    'overflow': 'visible'
                }}>
                    <div className="exa-auto-options" ref='options' style={{
                        'width': '100%',
                        'maxHeight': MAX_DROPDOWN_HEIGHT_PX,
                        'overflowY': 'auto',
                        'overflowX': 'hidden',
                        'textOverflow': 'ellipsis'
                    }}>
                        {options.map((option, index) => {
                            const isItemFocused = index == state.selectedIdx;
                            const focusCls = isItemFocused ? 'exa-auto-option-selected' : '';
                            const templateCls = (option.isTemplate) ? 'exa-auto-option-template' : '';
                            const cls = `exa-auto-option ${focusCls} ${templateCls}`;
                            const optionTitle = option.value || option;
                            return (<div
                                className={cls}
                                onClick={this._onSelectFromList.bind(this, option.brace || optionTitle)}
                                onMouseEnter={this._selectByIndex.bind(this, index)}>
                                {optionTitle}
                            </div>);
                        })}
                    </div>
                    {helpDiv}
                </div>
            );
        } // if show OptionList

        return (
            <div
                onKeyDown={this._onKeyDown}
                onBlur={this.props.valueOnBlur ? this._confirmValue : this.handleBlur}
                className={`exa-prop-input-ct exa-prop-auto-input ${hasFocus ? 'exa-input-focus' : ''}`}>
                <ContentEditable
                    ref="value"
                    className="exa-prop-input"
                    contentEditable={isEditing}
                    placeholder="Enter value here"
                    data-autotext={state.autotext}
                    onInput={this.handleInput}
                    spellCheck="false"
                    hasFocus={hasFocus}
                    value={this.state.value} />
                {hasOptions ? optionsList : undefined}
            </div>
        );
    }

});


module.exports = AutoInput;
