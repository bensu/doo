#!/bin/bash

set -euo pipefail

TEMPFILE=$(mktemp)
trap 'rm "$TEMPFILE"' EXIT

sed -e '/^#/d' -e '/^\s*$/d' "$1" \
    | circleci tests split > "$TEMPFILE"

echo "Will run the following tests: "
cat "$TEMPFILE"
echo

while read command
do
    echo "Running test: $command"
    sh -c "$command"
done < "$TEMPFILE"
