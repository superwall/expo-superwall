#!/bin/bash

cd "$(dirname "$0")/.."


echo 'Checking for Ktlint...'
if which ktlint >/dev/null; then
    echo 'KtLint already installed ✅'
else
    if which brew >/dev/null; then
      echo 'Installing Ktlint...'
      brew install ktlint
    else
      echo "
      Error: Ktlint could not be installed! ❌
      Check installation instructions from https://pinterest.github.io/ktlint/latest/ for manual
      install, or brew install ktlint. Then run this script again."
      exit 1
    fi
fi

echo 'Installing githooks...'
ktlint installGitPreCommitHook
echo 'Done! ✅'

