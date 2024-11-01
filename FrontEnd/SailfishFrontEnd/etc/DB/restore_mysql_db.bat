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
        set MYSQL=!PARAM_VALUE!
    ) else if "!PARAM_NAME!" equ "-backup" (
        set BACKUP_FILE=!PARAM_VALUE!
    ) else (
        exit /b 1
    )

    goto :readArgs
)

if not exist "%MYSQL%\mysql.exe" (
    echo Can't find '%MYSQL%\mysql.exe'
    exit /b 1
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
    set "version_query_file=%tmp%\bat~%RANDOM%.tmp"
    "%MYSQL%\mysql" --host=%HOST% --port=%PORT% --user=%SUPERUSER% --password=%SUPERPASSWORD% -e "select version();" > %version_query_file%

    if /I "!errorlevel!" NEQ "0" (
        echo Error determining mysql version
        exit /b 4
    )
    set "is_fifth_version="
    FINDSTR /B /C:"5." %version_query_file%>nul
    if /I "!errorlevel!" EQU "0" (
        set "is_fifth_version=1"
    )

    if "%is_fifth_version%"=="" (
        echo build query for MySQL 8 or higher
        set "CREATE_USER_QUERY=create user if not exists '%USER%'@'localhost' identified with mysql_native_password by '%PASSWORD%';"
        set "CREATE_USER_QUERY=!CREATE_USER_QUERY! create user if not exists '%USER%'@'localhost.localdomain' identified with mysql_native_password by '%PASSWORD%';"
        set "CREATE_USER_QUERY=!CREATE_USER_QUERY! grant all on %DATABASE%.* to '%USER%'@'localhost';"
        set "CREATE_USER_QUERY=!CREATE_USER_QUERY! grant all on %DATABASE%.* to '%USER%'@'localhost.localdomain';"
    ) else (
        echo build query for MySQL 5
        set "CREATE_USER_QUERY=grant all on %DATABASE%.* to '%USER%'@'localhost' identified by '%PASSWORD%' with grant option; grant all on %DATABASE%.* to '%USER%'@'localhost.localdomain' identified by '%PASSWORD%' with grant option;"
    )

    set "QUERY=drop database if exists %DATABASE%; create database %DATABASE% character set %CHAR%; %CREATE_USER_QUERY%"
    "%MYSQL%\mysql" --host=%HOST% --port=%PORT% --user=%1 --password=%2 -e "%QUERY%"
    exit /b !errorlevel!

:EOF
endlocal