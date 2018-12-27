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
import ReactDOM from 'react-dom';
import Editor from 'view/Editor.js';

ReactDOM.render(
    <Editor/>,
    document.getElementsByClassName('eps-editor-container')[0]
);


////////////////////////////
window.R = React;
window.RD = ReactDOM;

window.Perf = require('react-addons-perf');

// Handle toggleDetail in received from server ExecutionResult (html)
window.toggleDetails = function(block_id) {
    $('#' + block_id).toggle();
};
