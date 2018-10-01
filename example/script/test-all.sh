#!/usr/bin/env bash
set -e
npm install

export PATH=$PATH:node_modules/.bin/
export CHROME_BIN=$(which chromium || which chrome)

lein do clean, doo all once, doo nashorn once, doo all advanced once, doo all none-test once, doo node node-none once, doo node node-advanced once
