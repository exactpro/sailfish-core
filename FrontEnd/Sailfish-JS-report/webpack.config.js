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

const path = require("path");
const HtmlWebPackPlugin = require("html-webpack-plugin");

const mode = process.env.NODE_ENV || 'production';

module.exports = {
  devServer: {
    watchOptions : {
      poll: true,
      ignored: [/node_modules/, 'src/__tests__/']
    },
    watchContentBase: true,
    contentBase: [path.join(__dirname, 'src'), path.join(__dirname, 'build', 'out')],
    compress: true,
    port: 9001,
    host: "0.0.0.0"
  },
  output: {
    path: path.resolve(__dirname, './build/out/'),
    filename: 'bundle.js'
  },
  entry: path.resolve("./src/index.tsx"),
  mode: mode,
  resolve: {
    extensions: ['.ts', '.tsx', '.scss', '.js', '.jsx']
  },
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        loader: "awesome-typescript-loader",
        exclude: /node_modules/
      },
      {
        test: /\.scss$/,
        exclude: /node_modules/,
        use: [
          'style-loader',
          'css-loader',
          mode == 'production' ? {
            loader: 'postcss-loader',
            options: {
              config: {
                path: path.resolve(__dirname, './postcss.config.js')
              }
            }
          } : null,
          'sass-loader'
        ].filter(loader => loader)
      },
      {
        test: /\.(woff(2)?|ttf|eot|svg|jpg)(\?v=\d+\.\d+\.\d+)?$/,
        use: [{
            loader: 'file-loader',
            options: {
                name: '[name].[ext]',
                outputPath: 'resources/'
            }
        }]
      }
    ]
  },
  plugins: [
    new HtmlWebPackPlugin({
      title: "Sailfish report",
      template: "src/index.html",
      favicon: "resources/icons/favicon.png"
    })
  ],
  optimization: {
    usedExports: mode == 'production'
  },
  devtool: mode == 'production' ? undefined : 'eval'
};
