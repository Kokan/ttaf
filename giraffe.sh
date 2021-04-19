#!/bin/bash

JAVA=java
JAVA_ARG="${JAVA_ARG} -agentlib:hprof=cpu=samples,file=java.hprof.txt"

if test -f "/usr/lib/jvm/java-8-openjdk-amd64/bin/java" ; then
  JAVA=/usr/lib/jvm/java-8-openjdk-amd64/bin/java
fi

cd giraffe
gradle clean jar copyDependencies
if [ "0" != "$?" ]; then
	exit 1
fi
cd ..
${JAVA} ${JAVA_ARG} -cp "giraffe/build/libs/*" dog.giraffe.WebcamFrame "$@"
