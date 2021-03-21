#!/bin/bash

JAVA=/usr/lib/jvm/java-8-openjdk-amd64/bin/java
JAVA_ARG="${JAVA_ARG} -agentlib:hprof=cpu=samples,file=../java.hprof.txt"

cd giraffe
gradle clean jar copyDependencies
if [ "0" != "$?" ]; then
	exit 1
fi
${JAVA} ${JAVA_ARG} -cp "build/libs/*" dog.giraffe.WebcamFrame
