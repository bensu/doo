#!/bin/bash
set -e
lein clean
lein doo phantom test once
lein doo phantom advanced once
lein doo phantom none-test once
lein doo node node-none once
lein doo node node-advanced once
