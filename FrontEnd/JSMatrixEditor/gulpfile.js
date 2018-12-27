#!/usr/bin/env node

var gulp = require('gulp');
var fs = require('fs');
var path = require('path');
const WEB_CONTENT = 'WebContent';

gulp.task('webpack-copy',function() {
    var jsDest = path.join(__dirname, '../SailfishFrontEnd/' + WEB_CONTENT + '/resources/sf/js');
    var config = require('./public/javascripts/webpack.config.js');
    var bundleFile = path.join(config.output.path, config.output.filename);
    var destFile = path.join(jsDest, 'gui.editor.js');
    console.log('' + new Date() + ' copy from ' + bundleFile + ' to ' + destFile);
    fs.createReadStream(bundleFile).pipe(fs.createWriteStream(destFile));
    fs.createReadStream(bundleFile + '.map').pipe(fs.createWriteStream(destFile + '.map'));
});


gulp.task('watch', ['webpack-copy'], function() {
    gulp.watch('./build/javascripts/*', ['webpack-copy']);
});

gulp.task('default', ['watch']);
