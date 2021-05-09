#!/bin/bash

set -e

./compile.sh

cd giraffe
gradle test
cd ..

./giraffe-cmd-test.sh

