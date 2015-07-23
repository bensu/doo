var path = require("path"),
fs = require("fs"),
args = process.argv.slice(2);

// Support lein-npm, etc. in the simple case
module.paths.push(path.join(process.cwd(), 'node_modules'));

var haveCljsTest = function () {
    return (typeof cljs !== "undefined" &&
            typeof cljs.test !== "undefined" &&
            typeof doo !== "undefined" &&
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
            "",
            "Also remember that Node.js can be only used with simple/advanced ",
            "optimizations, not with none/whitespace.",
            ""
        ];
        console.error(messageLines.join("\n"));
        process.exit(1);
    }
};

args.forEach(function (arg) {
    var file = path.isAbsolute(arg) ? arg : path.join(process.cwd(), arg);
    global.require = require;
    global.process = process;
    if (fs.existsSync(file)) {
        try {
            // using eval instead of require here so that `this` is the "real"
            // top-level scope, not the module
           
            global.eval(  fs.readFileSync(file, {encoding: "UTF-8"}) ); 
            // check this before trying to call set_print_fn_BANG_
            failIfCljsTestUndefined(); 
        } catch (e) {
            console.log("Error in file: \"" + file + "\"");
            console.log(e);
            failIfCljsTestUndefined();
        }
    } else {
        try {
            global.eval("(function () {" + arg + "})()");
        } catch (e) {
            console.log("Could not evaluate expression: \"" + arg + "\"");
            console.log(e);
        }
    }
});

doo.runner.set_print_fn_BANG_(function(x) {
    // since console.log *itself* adds a newline
    var x = x.replace(/\n$/, "");
    if (x.length > 0) console.log(x);
});

doo.runner.set_exit_point_BANG_(function (isSuccess) {
    process.exit(isSuccess ? 0 : 1);
});

var results = doo.runner.run_BANG_();
