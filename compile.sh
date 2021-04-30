#!/bin/bash

cd giraffe
gradle clean jar copyDependencies --warning-mode all
if [ "0" != "$?" ]; then
	exit 1
fi
cd ..
