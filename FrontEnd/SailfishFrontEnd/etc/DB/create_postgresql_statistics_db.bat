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
Rem # This script is intended to (re)create Sailfish database on PostgreSQL server.	#
Rem # You can modify variables as you need.					#
Rem # But don't modify QUERY variable please.					#
Rem # Run only on Windows OS.							#
Rem #############################################################################
setlocal enabledelayedexpansion

set "POSTGRESQL=%PROGRAMFILES%\pgsql\bin"

set HOST=localhost
set PORT=5432

set SUPERUSER=postgres
set SUPERPASSWORD=pgpassword

set DATABASE=sfstatistics
set CHAR=utf8
set USER=sailfish
set PASSWORD=999

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
            set POSTGRESQL=%VAR%
        )
        set "CURRENT_PARAM="
    )
    shift
    goto :readArgs
)

set "QUERY_FILE=%tmp%\bat~%RANDOM%.tmp"
set "TEST_QUERY_FILE=%tmp%\bat~%RANDOM%.tmp"

echo select 1;> %TEST_QUERY_FILE%

call :checkDatabaseAvailability %USER% %PASSWORD%

if /I "!errorlevel!" NEQ "0" (
    echo Can't get access to database '%DATABASE%' for user '%USER%'. Checking the existence of the database

    call :checkDatabaseAvailability %SUPERUSER% %SUPERPASSWORD%

    if /I "!errorlevel!" NEQ "0" (
        echo Creating database '%DATABASE%'
        call :createDatabase

    ) else (
        echo Database '%DATABASE%' already exist for another user. The script will not be changing existing DB. Do all changes by yourself
        exit /b 3
    )
) else (
    echo Database '%DATABASE%' for user '%USER%' already exist
)
goto :EFO

:checkDatabaseAvailability
    "%POSTGRESQL%\psql" "postgresql://%1:%2@%HOST%:%PORT%/%DATABASE%" --file=%TEST_QUERY_FILE%
    exit /b !errorlevel!

:createDatabase
    echo Creating database '%DATABASE%'

    echo CREATE ROLE %USER% LOGIN ENCRYPTED PASSWORD '%PASSWORD%' NOINHERIT VALID UNTIL 'infinity'; > %QUERY_FILE%
    echo DROP DATABASE IF EXISTS %DATABASE%;>> %QUERY_FILE%
    echo CREATE DATABASE %DATABASE% WITH ENCODING='%CHAR%' OWNER=%USER%;>> %QUERY_FILE%
    echo GRANT ALL PRIVILEGES ON DATABASE %DATABASE% TO %USER% WITH GRANT OPTION;>> %QUERY_FILE%

    "%POSTGRESQL%\psql" "postgresql://%SUPERUSER%:%SUPERPASSWORD%@%HOST%:%PORT%" --file=%QUERY_FILE%
    exit /b !errorlevel!

:EFO
endlocal
