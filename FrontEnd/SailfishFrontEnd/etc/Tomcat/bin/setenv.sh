#!/bin/sh
#*****************************************************************************
# Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#*****************************************************************************
#export JAVA_HOME=<...> #Path to java_home for running tomcat

#   CATALINA_OPTS   (Optional) Java runtime options used when the "start",
#                   "run" or "debug" command is executed.
#                   Include here and not in JAVA_OPTS all options, that should
#                   only be used by Tomcat itself, not by the stop process,
#                   the version command etc.
#                   Examples are heap size, GC logging, JMX ports etc.
#   JAVA_OPTS       (Optional) Java runtime options used when any command
#                   is executed.
#                   Include here and not in CATALINA_OPTS all options, that
#                   should be used by Tomcat and also by the stop process,
#                   the version command etc.
#                   Most options should go into CATALINA_OPTS.
#JVM arguments:
export CATALINA_OPTS="$CATALINA_OPTS -Xms1024m -Xmx1024m" #Specifies the maximum size (in bytes) of the memory allocation pool in bytes.
export CATALINA_OPTS="$CATALINA_OPTS -XX:MaxPermSize=512m" #Size of the Permanent Generation.
export CATALINA_OPTS="$CATALINA_OPTS -XX:+HeapDumpOnOutOfMemoryError" #Option tells the HotSpot VM to generate a heap dump when an allocation from the Java heap or the permanent generation cannot be satisfied.
#export CATALINA_OPTS="$CATALINA_OPTS -XX:HeapDumpPath=" #For example -XX:HeapDumpPath=/disk2/dumps will cause the heap dump to be generated in the /disk2/dumps directory.
#export CATALINA_OPTS="$CATALINA_OPTS -Duser.timezone=UTC" #Use the user.timezone property value as the default time zone ID if it's available.
#export CATALINA_OPTS="$CATALINA_OPTS -XX:+UnlockCommercialFeatures -XX:+FlightRecorder" #Use for configure jvm FlightRecorder
export CATALINA_OPTS="$CATALINA_OPTS -Djava.net.preferIPv4Stack=true" #IPv4 addresses preferred over IPv6 addresses
export CATALINA_OPTS="$CATALINA_OPTS -XX:+ExitOnOutOfMemoryError" # Option tells that JVM should terminate process if OutOfMemoryError had been thrown
export CATALINA_OPTS="$CATALINA_OPTS -Duser.dir=$CATALINA_BASE/temp" # Uses to specify directory for relative paths
export CATALINA_OPTS="$CATALINA_OPTS -Djdk.tls.client.protocols=TLSv1.2" # rem Use TLSv1.2 protocol by default

#Deployer Jvm arguments:
export DEPLOYER_JAVA_OPTS=""
export CATALINA_OPTS="$CATALINA_OPTS $DEPLOYER_JAVA_OPTS"

#Get extends environment variables

export ADDITIONAL_SCRIPTS_DIR="$CATALINA_HOME/../../AdditionalScripts"

if [ -r "$ADDITIONAL_SCRIPTS_DIR/set_env_ext.sh" ]; then
  . "$ADDITIONAL_SCRIPTS_DIR/set_env_ext.sh"
fi