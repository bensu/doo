// karma.conf.js
module.exports = function(config) {
  // same as :output-dir
  var root =  'doooutputdir';

  config.set({
    frameworks: ['cljs-test'],
      
    plugins: ['karma-firefox-launcher',
              'karma-chrome-launcher',
              'karma-cljs-test'],

    browsers: ['Chrome', 'Firefox'],

    files: [
      root + '/goog/base.js',
      root + '/cljs_deps.js',
      'doooutputto',
      {pattern: root + '/*.js', included: false},
      {pattern: root + '/**/*.js', included: false}
    ],
      
    autoWatchBatchDelay: 1000,
      
    logLevel: config.LOG_WARN,

    client: {
      // main function
      args: ['doo.runner.run_BANG_']
    },
    
    singleRun: true
  });
};
