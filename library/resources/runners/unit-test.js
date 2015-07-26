// reusable slimerjs/phantomjs script for running clojurescript.test tests
// see http://github.com/cemerick/clojurescript.test for more info

var p = require('webpage').create();
var fs = require('fs');
var sys = require('system');

function toAbsolutePath(path) {
    if (fs.isAbsolute(path)) {
        return path;
    } else {
        return fs.absolute(".") + "/" + path;
    };
};

var pagePath = sys.args[0] + ".html";

var scripts = "";

for (var i = 1; i < sys.args.length; i++) {
    scripts += '<script src="' + toAbsolutePath(sys.args[i]) + '"></script>';
}

var html = "<html><head><meta charset=\"UTF-8\">"
           + scripts
           + "</head><body></body></html>";

fs.write(pagePath, html, 'w');

function isSlimer() {
    return (typeof slimer !== 'undefined');
}

// Prepare the page with triggers

// Redirects the output of the page into the script
p.onConsoleMessage = function(msg) {
    console.log(msg); 
};

p.onError = function(msg) {
    console.error(msg);
    phantom.exit(1);
};

p.open("file://" + pagePath, function (status) {
    fs.remove(pagePath);
    if (status == "fail") {
        // TODO: improve error reporting
        console.log("Slimer or Phantom have failed to open the script. Try manually running it in a browser to see the errors");
        phantom.exit(1);
    } else {

        p.onCallback = function (x) {
	    var line = x.toString();
	    if (line !== "[NEWLINE]") {
	        console.log(line.replace(/\[NEWLINE\]/g, "\n"));
	    }
        };
        
        // if slimer & async add a shim to avoid insecure operations 
        if (isSlimer()) {
            p.evaluate(function () {
                // Helper functions
                function isAsync() {
                    return ((typeof goog !== 'undefined') &&
                            (typeof goog.async !== 'undefined'));
                }
                if (isAsync()) {
                    goog.async.nextTick.setImmediate_ = function(funcToCall) {
                        return window.setTimeout(funcToCall, 0);
                    };
                }
            });
        }
        
        p.evaluate(function() {
            if (typeof doo !== 'undefined') {
	        doo.runner.set_print_fn_BANG_(function(x) {
	            // using callPhantom to work around
                    // https://github.com/laurentj/slimerjs/issues/223
	            window.callPhantom(x.replace(/\n/g, "[NEWLINE]"));
                    // since console.log *itself* adds a newline
	        });
            } else {
                window.callPhantom("ERROR: doo was not loaded from the compiled script. Please make sure you are calling doo-tests or doo-all-tests");
            }
        });

        // p.evaluate is sandboxed, can't ship closures across;
        // so, a bit of a hack, better than polling :-P
        var exitCodePrefix = "phantom-exit-code:";
        p.onAlert = function (msg) {
	    var exit = msg.replace(exitCodePrefix, "");
	    if (msg != exit) {
                phantom.exit(parseInt(exit));
            }
        };
        
        p.evaluate(function (exitCodePrefix) {
	    doo.runner.set_exit_point_BANG_(function (isSuccess) {
	        window.alert(exitCodePrefix + (isSuccess ? 0 : 1));
	    });
            
            var results = doo.runner.run_BANG_();
            
        }, exitCodePrefix);
    }

});
