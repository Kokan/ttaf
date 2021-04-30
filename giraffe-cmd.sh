#!/bin/bash

java -Xmx8g -cp "giraffe/build/libs/*" dog.giraffe.CmdLine "$@"
