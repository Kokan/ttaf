#!/bin/bash

java -Xmx16g -cp "giraffe/build/libs/*" dog.giraffe.gui.Viewer "$@"
