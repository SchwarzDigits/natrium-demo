#!/usr/bin/env bash

# 

# Pre-analyzer preparation for natrium-demo.

# 

# natrium-demo uses a Gradle composite build (`includeBuild "../natrium"`)

# that expects the Natrium SDK to sit as a sibling directory on disk:

# 

# parent/

# natrium/         <– cloned here by this script

# natrium-demo/    <– the workspace

# 

# This script clones natrium (with its Kalium submodule) into the parent

# of the workspace before ORT runs, so the Gradle Inspector can resolve

# the composite build.

# 

# Invoked by the central oss-compliance workflow via the

# `pre-analyzer-script` input.

set -euo pipefail

cd ..

echo "Cloning natrium as sibling of natrium-demo…"
git clone –recurse-submodules –depth 1   
https://github.com/SchwarzDigits/natrium.git

echo "Sibling clone ready at $(pwd)/natrium"
ls -la natrium/