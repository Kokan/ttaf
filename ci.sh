#!/bin/bash

set -e

cd giraffe
gradle clean jar copyDependencies
gradle test

