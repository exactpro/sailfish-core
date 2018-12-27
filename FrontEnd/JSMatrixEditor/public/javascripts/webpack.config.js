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

var path = require('path');

module.exports = {
    entry: path.join(__dirname, './entry.js'),
    output: {
        path: path.join(__dirname, '../../../SailfishFrontEnd/WebContent/resources/sf/js'),
        filename: 'gui.editor.js'
    },

    module: {
        loaders: [
            { test: /.*\.(jsx|js)$/, loader: 'babel-loader', exclude: /node_modules/, query: {
                presets: ['env', 'react', 'stage-1'],
                plugins: ['babel-plugin-default-import-checker']
            } },
            { test: /.*\.css$/, loader: 'style-loader!css-loader' }
        ]
    },

    devtool: '#eval-source-map',

    resolve: {
        root: __dirname,

        alias: {
            actions$: path.join(__dirname, 'actions'),
            consts: path.join(__dirname, 'consts.js'),
            state$: path.join(__dirname, 'state')
        }
    }
};
