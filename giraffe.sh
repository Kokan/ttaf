#!/bin/bash

JAVA=java
JAVA_ARG=""

if test -f "/usr/lib/jvm/java-8-openjdk-amd64/bin/java" ; then
  JAVA=/usr/lib/jvm/java-8-openjdk-amd64/bin/java
  JAVA_ARG="${JAVA_ARG} -agentlib:hprof=cpu=samples,file=../java.hprof.txt"
fi

cd giraffe
gradle clean jar copyDependencies
if [ "0" != "$?" ]; then
	exit 1
fi
${JAVA} ${JAVA_ARG} -cp "build/libs/*" dog.giraffe.WebcamFrame
