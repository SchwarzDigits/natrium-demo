#!/usr/bin/env bash

set -euo pipefail

cd ..

echo "Cloning natrium as sibling of natrium-demo…"
git clone –recurse-submodules –depth 1 https://github.com/SchwarzDigits/natrium.git

echo "Sibling clone ready at $(pwd)/natrium"
ls -la natrium/