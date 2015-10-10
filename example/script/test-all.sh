#!/bin/bash
set -e
lein clean
lein doo all test once
lein doo all advanced once
lein doo all none-test once
lein doo node node-none once
lein doo node node-advanced once
