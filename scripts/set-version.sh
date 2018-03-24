#!/bin/bash

set -euo pipefail

VERSION=${1:?No version given}

sed -i '' -e "s/\\(doo *\\)\"[^\"]*\"/\\1\"$VERSION\"/g" \
    library/project.clj \
    plugin/project.clj \
    plugin/src/leiningen/doo.clj \
    example/project.clj \
    example/build.boot

# The example project version number is not very important, but let's  keep it
# up to date as well.
sed -i '' -e "s/\\(lein-doo-example *\\)\"[^\"]*\"/\\1\"$VERSION\"/g" \
    example/project.clj
