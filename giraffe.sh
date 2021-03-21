#!/bin/bash
cd giraffe
gradle clean jar copyDependencies
if [ "0" != "$?" ]; then
	exit 1
fi
java -cp "build/libs/*" dog.giraffe.WebcamFrame "$@"
