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

rem JVM arguments:
set "JAVA_OPTS=%JAVA_OPTS% -Xms1024m -Xmx1024m" rem Specifies the maximum size (in bytes) of the memory allocation pool in bytes.
set "JAVA_OPTS=%JAVA_OPTS% -XX:MaxPermSize=512m" rem Size of the Permanent Generation.
set "JAVA_OPTS=%JAVA_OPTS% -XX:+HeapDumpOnOutOfMemoryError" rem Option tells the HotSpot VM to generate a heap dump when an allocation from the Java heap or the permanent generation cannot be satisfied.
rem set "JAVA_OPTS=%JAVA_OPTS% -XX:HeapDumpPath=" rem For example -XX:HeapDumpPath=/disk2/dumps will cause the heap dump to be generated in the /disk2/dumps directory.
rem set "JAVA_OPTS=%JAVA_OPTS% -Duser.timezone=UTC" rem Use the user.timezone property value as the default time zone ID if it's available.
rem set "JAVA_OPTS=%JAVA_OPTS% -Djava.net.preferIPv4Stack=true" rem IPv4 addresses preferred over IPv6 addresses
rem set "JAVA_OPTS=%JAVA_OPTS% -XX:+UnlockCommercialFeatures -XX:+FlightRecorder" rem Use for configure jvm FlightRecorder
set "JAVA_OPTS=%JAVA_OPTS% -XX:+ExitOnOutOfMemoryError" rem Option tells what JVM should terminate process if OutOfMemoryError had been thrown

rem Deployer Jvm arguments:
set "DEPLOYER_JAVA_OPTS="
set "JAVA_OPTS=%JAVA_OPTS% %DEPLOYER_JAVA_OPTS%"

rem Get extends environment variables
set "ADDITIONAL_SCRIPTS_DIR=%CATALINA_HOME%\..\..\AdditionalScripts"

if exist "%ADDITIONAL_SCRIPTS_DIR%\set_env_ext.bat" call "%ADDITIONAL_SCRIPTS_DIR%\set_env_ext.bat"