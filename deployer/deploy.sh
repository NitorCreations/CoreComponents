#!/bin/bash

if ! [ -r $JAVA_HOME/lib/tools.jar ]; then
   echo "Could not find tools.jar - is \$JAVA_HOME set?"
   exit 1
fi
export INSTANCE_ID=test
exec $JAVA_HOME/bin/java -cp $JAVA_HOME/lib/tools.jar:target/deployer-1.0-SNAPSHOT-jar-with-dependencies.jar com.nitorcreations.deployer.Main com.nitorcreations:liipy-jersey:jar:uber:1.0-SNAPSHOT
