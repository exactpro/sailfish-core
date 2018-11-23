const path = require("path");
const HtmlWebPackPlugin = require("html-webpack-plugin");

module.exports = {
  resolve: {
    extensions: ['.ts', '.js', '.tsx', '.scss']
  },
  output: {
    path: path.resolve(__dirname, './build/'),
    filename: 'bundle.js'
  },
  entry: path.resolve("./src/index.tsx"),
  mode: process.env.NODE_ENV || 'development',
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
          'sass-loader'
        ]
      },
      {
        test: /\.(woff(2)?|ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/,
        use: [{
            loader: 'file-loader',
            options: {
                name: '[name].[ext]',
                outputPath: 'fonts/'
            }
        }]
      }
    ]
  },
  plugins: [
    new HtmlWebPackPlugin({
      title: "Sailfish reports",
      template: "src/index.html"
    })
  ]
};
