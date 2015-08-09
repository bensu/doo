// karma.conf.js
module.exports = function(config) {
  // same as :output-dir
  var root = 'target/';

  config.set({
    frameworks: ['cljs-test'],

    files: [
      root + '/cljsbuild-compiler-1/goog/base.js',
      root + '/cljsbuild-compiler-1/cljs_deps.js',
      root + '/testable.js',// same as :output-to
      {pattern: root + '/*.js', included: false},
      {pattern: root + '/**/*.js', included: false}
    ],
      
    autoWatchBatchDelay: 1000,
      
    logLevel: config.LOG_WARN,

    client: {
      // main function
      args: ['doo.runner.run_BANG_']
    },
    
  });
};
