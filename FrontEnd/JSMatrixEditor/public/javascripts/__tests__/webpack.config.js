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
var rootDir = path.join(__dirname, '..');
var RewirePlugin = require("rewire-webpack");

module.exports = {
    entry: "./entry__.js",
    output: {
        path: __dirname,
        filename: "test-page.bundle.js"
    },

    module: {
        loaders: [
            { test: /.*\.(jsx|js)$/, loader: 'babel-loader', exclude: /node_modules/,
                query: {
                presets: ['env', 'react', 'stage-1']
                }
            },
            { test: /.*\.css$/, loader: 'style-loader!css-loader' }
        ]
    },

    devtool: "#source-map",

    resolve: {
        root: rootDir,


        alias: {
            "actions/TheChannel.js": path.join(rootDir, '__tests__', 'mock', 'FakeChannels.js'),
            "state/TheMatrixList.js": path.join(rootDir, '__tests__', 'mock', 'FakeMatrixList.js'),
            "actions/TheServer.js": path.join(rootDir, '__tests__', 'mock', 'FakeServer.js'),
            "actions$": path.join(rootDir, 'actions'),
            "consts": path.join(rootDir, 'consts.js'),
            "state$": path.join(rootDir, 'state')
        }
    },

    plugins: [
        new RewirePlugin()
    ]
};
