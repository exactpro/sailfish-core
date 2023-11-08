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

:readArgs

set PARAM_NAME=%1
set PARAM_VALUE=%2

if "!PARAM_NAME!" neq "" (
    if "%PARAM_NAME:~0,1%" neq "-" exit /b 2

    if "%PARAM_VALUE:~0,1%" equ "-" (
        set "PARAM_VALUE="
        shift
    ) else (
        shift
        shift
    )

    if "!PARAM_NAME!" equ "-user" (
        set USER=!PARAM_VALUE!
    ) else if "!PARAM_NAME!" equ "-password" (
        set PASSWORD=!PARAM_VALUE!
    ) else if "!PARAM_NAME!" equ "-host" (
        set HOST=!PARAM_VALUE!
    ) else if "!PARAM_NAME!" equ "-port" (
        set PORT=!PARAM_VALUE!
    ) else if "!PARAM_NAME!" equ "-database" (
        set DATABASE=!PARAM_VALUE!
    ) else if "!PARAM_NAME!" equ "-superuser" (
        set SUPERUSER=!PARAM_VALUE!
    ) else if "!PARAM_NAME!" equ "-superpassword" (
        set SUPERPASSWORD=!PARAM_VALUE!
    ) else if "!PARAM_NAME!" equ "-path" (
        set POSTGRESQL=!PARAM_VALUE!
    ) else (
        exit /b 1
    )

    goto :readArgs
)

if not exist "%POSTGRESQL%\psql.exe" (
    echo Can't find '%POSTGRESQL%\psql.exe'
    exit /b 1
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
    "%POSTGRESQL%\psql" --file=%TEST_QUERY_FILE% "postgresql://%1:%2@%HOST%:%PORT%/%DATABASE%"
    exit /b !errorlevel!

:createDatabase
    echo Creating database '%DATABASE%'

    echo CREATE ROLE %USER% LOGIN ENCRYPTED PASSWORD '%PASSWORD%' NOINHERIT VALID UNTIL 'infinity'; > %QUERY_FILE%
    echo DROP DATABASE IF EXISTS %DATABASE%;>> %QUERY_FILE%
    echo CREATE DATABASE %DATABASE% WITH ENCODING='%CHAR%' OWNER=%USER%;>> %QUERY_FILE%
    echo GRANT ALL PRIVILEGES ON DATABASE %DATABASE% TO %USER% WITH GRANT OPTION;>> %QUERY_FILE%

    "%POSTGRESQL%\psql" --file=%QUERY_FILE% "postgresql://%SUPERUSER%:%SUPERPASSWORD%@%HOST%:%PORT%"
    exit /b !errorlevel!

:EFO
endlocal
