#!/bin/bash

set -euo pipefail
TESTS=$(circleci tests glob test/bats/*.bats | circleci tests split)
bats --tap $TESTS | tee ~/test-results/example/results.tap
