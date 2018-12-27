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

setlocal enabledelayedexpansion

set "MYSQL=%PROGRAMFILES%\MySQL\MySQL Server 8.0\bin"

set HOST=localhost
set PORT=3306

set SUPERUSER=root
set "SUPERPASSWORD="

set "DATABASE="
set CHAR=utf8
set USER=sailfish
set PASSWORD=999

set "BACKUP_FILE="

set "CURRENT_PARAM="

set "VAR="

:readArgs
set VAR=%~1

if "%VAR%" neq "" (
    if "%CURRENT_PARAM%" equ "" (
        if "%VAR:~0,1%" neq "-" exit /b 2

        set "CURRENT_PARAM=%VAR:~1%"
    ) else (
        if "%CURRENT_PARAM%" equ "user" (
            set USER=%VAR%
        ) else if "%CURRENT_PARAM%" equ "password" (
            set PASSWORD=%VAR%
        ) else if "%CURRENT_PARAM%" equ "host" (
            set HOST=%VAR%
        ) else if "%CURRENT_PARAM%" equ "port" (
            set PORT=%VAR%
        ) else if "%CURRENT_PARAM%" equ "database" (
            set DATABASE=%VAR%
        ) else if "%CURRENT_PARAM%" equ "superuser" (
            set SUPERUSER=%VAR%
        ) else if "%CURRENT_PARAM%" equ "superpassword" (
            set SUPERPASSWORD=%VAR%
        ) else if "%CURRENT_PARAM%" equ "path" (
            set MYSQL=%VAR%
        ) else if "%CURRENT_PARAM%" equ "backup" (
            set BACKUP_FILE=%VAR%
        )
        set "CURRENT_PARAM="
    )
    shift
    goto :readArgs
)

if "%DATABASE%" eq "" (
    echo Database name has not been set
    exit /b 1
)

if "%BACKUP_FILE%" eq "" (
    echo Backup file has not been set
    exit /b 1
)

call :restoreFromBackup %BACKUP_FILE% %SUPERUSER% %SUPERPASSWORD%

if /I "!errorlevel!" NEQ "0" (
    echo Can't restore database %DATABASE% from backup %BACKUP_FILE%
    exit /b !errorlevel!
)
goto :EOF

:restoreFromBackup
    call :createDatabase %2 %3
    
    if /I "!errorlevel!" NEQ "0" (
        echo Can't recreate database %DATABASE%
        exit /b !errorlevel!
    )
    "%MYSQL%\mysql" --host=%HOST% --port=%PORT% --user=%2 --password=%3 -e "source %1"
    exit /b !errorlevel!

:createDatabase
    set QUERY=drop database if exists %DATABASE%; create database %DATABASE% character set %CHAR%; grant all on %DATABASE%.* to '%USER%'@'localhost' identified by '%PASSWORD%' with grant option; grant all on %DATABASE%.* to '%USER%'@'localhost.localdomain' identified by '%PASSWORD%' with grant option;
    "%MYSQL%\mysql" --host=%HOST% --port=%PORT% --user=%1 --password=%2 -e "%QUERY%"
    exit /b !errorlevel!

:EOF
endlocal