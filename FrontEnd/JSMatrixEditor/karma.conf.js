// Karma configuration
// Generated on Mon Apr 13 2015 13:46:55 GMT+0300 (MSK)

module.exports = function (config) {
    config.set({

        // base path that will be used to resolve all patterns (eg. files, exclude)
        basePath: './public/javascripts/',


        // frameworks to use
        // available frameworks: https://npmjs.org/browse/keyword/karma-adapter
        frameworks: ['jasmine'],


        // list of files / patterns to load in the browser
        files: [
            '__tests__/spec/**/*.js',
            '__tests__/spec/*.js'
        ],


        // list of files to exclude
        exclude:[ ],


        // preprocess matching files before serving them to the browser
        // available preprocessors: https://npmjs.org/browse/keyword/karma-preprocessor
        preprocessors: {
            '__tests__/spec/*.js': ['webpack', 'sourcemap'],
            '__tests__/spec/**/*.js': ['webpack', 'sourcemap']
        },

        webpack: function() {
            var config = require('./public/javascripts/__tests__/webpack.config.js');
            delete config.entry;
            delete config.output;
            config.devtool = '#inline-source-map';
            return config;
        }(),

        // test results reporter to use
        // possible values: 'dots', 'progress'
        // available reporters: https://npmjs.org/browse/keyword/karma-reporter
        reporters: ['spec'],


        // web server port
        port: 9876,


        // enable / disable colors in the output (reporters and logs)
        colors: false,


        // level of logging
        // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
        logLevel: config.LOG_INFO,


        // enable / disable watching file and executing tests whenever any file changes
        autoWatch: true,


        // start these browsers
        // available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
        browsers: ['ChromeHeadless', 'FirefoxHeadless'/*, 'PhantomJS' */],
        customLaunchers: {
            FirefoxHeadless: {
                base: 'Firefox',
                flags: [ '-headless' ],
            },
        },

        // Continuous Integration mode
        // if true, Karma captures browsers, runs the tests and exits
        singleRun: false,

        webpackMiddleware: {
            // webpack-dev-middleware configuration
            // i. e.
            noInfo: true
        },
        
        specReporter: {
            maxLogLines: 50,
            suppressErrorSummary: false,
            suppressFailed: false,
            suppressPassed: false,
            suppressSkipped: false,
            showSpecTiming: false
        },

        plugins: [
            require("karma-jasmine"),
            require("karma-chrome-launcher"),
            require("karma-firefox-launcher"),
            require("karma-webpack"),
            require("karma-sourcemap-loader"),
            require("karma-spec-reporter")

        ]
    });
};
