@echo off
rem *****************************************************************************
rem  Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
rem
rem  Licensed under the Apache License, Version 2.0 (the "License");
rem  you may not use this file except in compliance with the License.
rem  You may obtain a copy of the License at
rem
rem      http://www.apache.org/licenses/LICENSE-2.0
rem
rem  Unless required by applicable law or agreed to in writing, software
rem  distributed under the License is distributed on an "AS IS" BASIS,
rem  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem  See the License for the specific language governing permissions and
rem  limitations under the License.
rem *****************************************************************************
rem set "JAVA_HOME=<...>" rem Path to java_home for running tomcat

rem   CATALINA_OPTS   (Optional) Java runtime options used when the "start",
rem                   "run" or "debug" command is executed.
rem                   Include here and not in JAVA_OPTS all options, that should
rem                   only be used by Tomcat itself, not by the stop process,
rem                   the version command etc.
rem                   Examples are heap size, GC logging, JMX ports etc.
rem   JAVA_OPTS       (Optional) Java runtime options used when any command
rem                   is executed.
rem                   Include here and not in CATALINA_OPTS all options, that
rem                   should be used by Tomcat and also by the stop process,
rem                   the version command etc.
rem                   Most options should go into CATALINA_OPTS.

rem JVM arguments:
set "CATALINA_OPTS=%CATALINA_OPTS% -Xms1024m -Xmx1024m" rem Specifies the maximum size (in bytes) of the memory allocation pool in bytes.
set "CATALINA_OPTS=%CATALINA_OPTS% -XX:MaxPermSize=512m" rem Size of the Permanent Generation.
set "CATALINA_OPTS=%CATALINA_OPTS% -XX:+HeapDumpOnOutOfMemoryError" rem Option tells the HotSpot VM to generate a heap dump when an allocation from the Java heap or the permanent generation cannot be satisfied.
rem set "CATALINA_OPTS=%CATALINA_OPTS% -XX:HeapDumpPath=" rem For example -XX:HeapDumpPath=/disk2/dumps will cause the heap dump to be generated in the /disk2/dumps directory.
rem set "CATALINA_OPTS=%CATALINA_OPTS% -Duser.timezone=UTC" rem Use the user.timezone property value as the default time zone ID if it's available.
rem set "CATALINA_OPTS=%CATALINA_OPTS% -XX:+UnlockCommercialFeatures -XX:+FlightRecorder" rem Use for configure jvm FlightRecorder
set "CATALINA_OPTS=%CATALINA_OPTS% -Djava.net.preferIPv4Stack=true" rem IPv4 addresses preferred over IPv6 addresses
set "CATALINA_OPTS=%CATALINA_OPTS% -XX:+ExitOnOutOfMemoryError" rem Option tells what JVM should terminate process if OutOfMemoryError had been thrown
set "CATALINA_OPTS=%CATALINA_OPTS% -Duser.dir=%CATALINA_BASE%\temp" rem Uses to specify directory for relative paths
set "CATALINA_OPTS=%CATALINA_OPTS% -Djdk.tls.client.protocols=TLSv1,TLSv1.1,TLSv1.2" rem Use TLSv1.2 protocol by default

rem Deployer Jvm arguments:
set "DEPLOYER_JAVA_OPTS="
set "CATALINA_OPTS=%CATALINA_OPTS% %DEPLOYER_JAVA_OPTS%"

rem Get extends environment variables
set "ADDITIONAL_SCRIPTS_DIR=%CATALINA_HOME%\..\..\AdditionalScripts"

if exist "%ADDITIONAL_SCRIPTS_DIR%\set_env_ext.bat" call "%ADDITIONAL_SCRIPTS_DIR%\set_env_ext.bat"