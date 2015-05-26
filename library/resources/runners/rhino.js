var setTimeout, clearTimeout, setInterval, clearInterval;

(function () {
    var executor = new java.util.concurrent.Executors.newScheduledThreadPool(1);
    var counter = 1;
    var ids = {};

    setTimeout = function (fn,delay) {
        var id = counter++;
        var runnable = new JavaAdapter(java.lang.Runnable, {run: fn});
        ids[id] = executor.schedule(runnable, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
        return id;
    }

    clearTimeout = function (id) {
        ids[id].cancel(false);
        executor.purge();
        delete ids[id];
    }

    setInterval = function (fn,delay) {
        var id = counter++;
        var runnable = new JavaAdapter(java.lang.Runnable, {run: fn});
        ids[id] = executor.scheduleAtFixedRate(runnable, delay, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
        return id;
    }

    clearInterval = clearTimeout;

})()

var haveCljsTest = function () {
    return (typeof doo !== "undefined" &&
            typeof cljs !== "undefined" &&
            typeof cljs.test !== "undefined" &&
            typeof doo.runner.run_BANG_ === "function");
};

var failIfCljsTestUndefined = function () {
    if (!haveCljsTest()) {
        var messageLines = [
            "",
            "ERROR: cljs.test was not required.",
            "",
            "You can resolve this issue by ensuring [cljs.test] appears",
            "in the :require clause of your test suite namespaces.",
            "Also make sure that your build has actually included any test files.",
            ""
        ];
        print(messageLines.join("\n"));
        java.lang.System.exit(1);
    }
}

arguments.forEach(function (arg) {
    if (new java.io.File(arg).exists()) {
        try {
            load(arg);
        } catch (e) {
            failIfCljsTestUndefined();
            print("Error in file: \"" + arg + "\"");
            print(e);
        }
    } else {
        try {
            eval("(function () {" + arg + "})()");
        } catch (e) {
            print("Could not evaluate expression: \"" + arg + "\"");
            print(e);
        }
    }
});

failIfCljsTestUndefined(); // check this before trying to call set_print_fn_BANG_

doo.runner.set_print_fn_BANG_(function(x) {
    // since console.log *itself* adds a newline
    var x = x.replace(/\n$/, "");
    if (x.length > 0) print(x);
});

doo.runner.set_exit_point_BANG_(function () {
    java.lang.System.exit(0);
});

var results = doo.runner.run_BANG_();
