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

#JVM arguments:
export JAVA_OPTS="$JAVA_OPTS -Xms1024m -Xmx1024m" #Specifies the maximum size (in bytes) of the memory allocation pool in bytes.
export JAVA_OPTS="$JAVA_OPTS -XX:MaxPermSize=512m" #Size of the Permanent Generation.
export JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError" #Option tells the HotSpot VM to generate a heap dump when an allocation from the Java heap or the permanent generation cannot be satisfied.
#export JAVA_OPTS="$JAVA_OPTS -XX:HeapDumpPath=" #For example -XX:HeapDumpPath=/disk2/dumps will cause the heap dump to be generated in the /disk2/dumps directory.
#export JAVA_OPTS="$JAVA_OPTS -Duser.timezone=UTC" #Use the user.timezone property value as the default time zone ID if it's available.
#export JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true" #IPv4 addresses preferred over IPv6 addresses
#export JAVA_OPTS="$JAVA_OPTS -XX:+UnlockCommercialFeatures -XX:+FlightRecorder" #Use for configure jvm FlightRecorder
export JAVA_OPTS="$JAVA_OPTS -XX:+ExitOnOutOfMemoryError" # Option tells that JVM should terminate process if OutOfMemoryError had been thrown

#Deployer Jvm arguments:
export DEPLOYER_JAVA_OPTS=""
export JAVA_OPTS="$JAVA_OPTS $DEPLOYER_JAVA_OPTS"

#Get extends environment variables

export ADDITIONAL_SCRIPTS_DIR="$CATALINA_HOME/../../AdditionalScripts"

if [[ -r "$ADDITIONAL_SCRIPTS_DIR/set_env_ext.sh" ]]; then
  . "$ADDITIONAL_SCRIPTS_DIR/set_env_ext.sh"
fi