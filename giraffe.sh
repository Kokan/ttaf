#!/bin/bash
cd giraffe
gradle clean jar copyDependencies
if [ "0" != "$?" ]; then
	exit 1
fi
/usr/lib/jvm/java-8-openjdk-amd64/bin/java -agentlib:hprof=cpu=samples,file=../java.hprof.txt -cp "build/libs/*" dog.giraffe.WebcamFrame
