// Common code to all runners 
// Should be kept in sync between rhino.js, nashorn.js & headless.js

function hasDoo () {
    return (typeof doo !== "undefined" &&
            typeof doo.runner !== "undefined" &&
            typeof doo.runner.run_BANG_ === "function");
};

function noDooMsg() {
    var msgLines = [
            "",
            "ERROR: doo was not loaded from the compiled script.",
            "",
            "Make sure you start your tests using doo-tests or doo-all-tests",
            "and that you include that file in your build",
            ""
        ];
    return msgLines.join("\n");
};

function noScriptMsg(env) {
    var msgLines = [
        "",
        ["ERROR:", env, "couldn't find the script"].join(" "),
        "",
        "This is most likely doo's fault. Please file an issue at ",
        "http://github.com/bensu/doo and we'll sort it out together.",
        ""
    ];
    return msgLines.join("\n");
}
