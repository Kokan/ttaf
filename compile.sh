#!/bin/bash

set -e

cd giraffe
gradle clean jar copyDependencies --warning-mode all
cd ..
