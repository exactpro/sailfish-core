@rem ***************************************************************************
@rem Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem     http://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem ***************************************************************************
@echo off
Rem #############################################################################
Rem # This script is intended to (re)create Sailfish database on MySQL server.	#
Rem # You can modify variables as you need.					#
Rem # But don't modify QUERY variable please.					#
Rem # Run only on Windows OS.							#
Rem #############################################################################

set MYSQL=C:\Documents and Settings\Andrey\Desktop\mysql-5.5.20-win32\bin

set HOST=localhost
set PORT=3306

set SUPERUSER=root
set SUPERPASSWORD=

set DATABASE=sailfish
set CHAR=utf8
set USER=sailfish
set PASSWORD=999
set DUMPFILE=dump_users.sql

Rem Backup users and users roles. WARNING: You need to mysqldump in you MySQL directory
Rem "%MYSQL%\mysqldump" --host=%HOST% --port=%PORT% --user=%SUPERUSER% --password=%SUPERPASSWORD% %DATABASE% app_user app_users_roles > %DUMPFILE%

set QUERY=drop database if exists %DATABASE%; create database %DATABASE% character set %CHAR%; grant all on %DATABASE%.* to '%USER%'@'localhost' identified by '%PASSWORD%' with grant option; grant all on %DATABASE%.* to '%USER%'@'localhost.localdomain' identified by '%PASSWORD%' with grant option;

"%MYSQL%\mysql" --host=%HOST% --port=%PORT% --user=%SUPERUSER% --password=%SUPERPASSWORD% -e "%QUERY%"

Rem Restore users and users roles
Rem "%MYSQL%\mysql" --host=%HOST% --port=%PORT% --user=%SUPERUSER% --password=%SUPERPASSWORD% %DATABASE% -e "SOURCE %DUMPFILE%"

Rem Uncomment for autoremove dump file.
Rem del %DUMPFILE%