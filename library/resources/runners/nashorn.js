// Nashorn Runner

var global = this;

var console = {};
console.debug = print;
console.warn = print;
console.log = print;

var setTimeout, clearTimeout, setInterval, clearInterval;

(function () {
    var executor = Java.type("java.util.concurrent.Executors").newScheduledThreadPool(1);
    var counter = 1;
    var ids = {};

    var RunnableExtender = Java.extend(Java.type("java.lang.Runnable"));

    setTimeout = function (fn,delay) {
        var id = counter++;
	var runnable = new RunnableExtender() { run: fn }
        ids[id] = executor.schedule(runnable, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
        return id;
    };

    clearTimeout = function (id) {
        ids[id].cancel(false);
        executor.purge();
        delete ids[id];
    };

    setInterval = function (fn,delay) {
        var id = counter++;
	var runnable = new RunnableExtender() { run: fn }
        ids[id] = executor.scheduleAtFixedRate(runnable, delay, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
        return id;
    };

    clearInterval = clearTimeout;

})();

function assertDoo() {
    if (!hasDoo()) {
        print(noDooMsg());
        java.lang.System.exit(1);
    };
};

for each (arg in arguments) {
    if (new java.io.File(arg).exists()) {
        try {
            load(arg);
        } catch (e) {
            print("Error while loading file: \"" + arg + "\"");
            print(e);
            print("At line " + e.lineNumber);
        }
    } else {
        print(noScriptMsg("Nashorn"));
        java.lang.System.exit(1);
    }
};

// check this before trying to call set_print_fn_BANG_
assertDoo(); 

doo.runner.set_print_fn_BANG_(function(x) {
    // since console.log *itself* adds a newline
    var splitX = x.replace(/\n$/, "");
    if (splitX.length > 0) {
        print(splitX);
    }
});

doo.runner.set_exit_point_BANG_(function (isSuccess) {
    java.lang.System.exit(isSuccess ? 0 : 1);
});

try {
  var results = doo.runner.run_BANG_();
} catch (e) {
  e.printStackTrace();
  throw(e); // propagate exception further
}

/* wait for the exit to happen */
var Thread = Java.type("java.lang.Thread");
while (true) Thread.yield();

