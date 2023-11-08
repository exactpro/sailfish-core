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
set USER=sailfish
set PASSWORD=999

set BACKUP_FILE=backup.sql

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

if not exist "%MYSQL%\mysqldump.exe" (
    echo Can't find '%MYSQL%\mysqldump.exe'
    exit /b 1
)

if "%DATABASE%" eq "" (
    echo Database name has not been set
    exit /b 1
)

call :createBackup %SUPERUSER% %SUPERPASSWORD%
if /I "!errorlevel!" NEQ "0" (
    echo Can't create backup for database %DATABASE%
    exit /b !errorlevel!
)
goto :EOF

:createBackup
    "%MYSQL%\mysqldump" --add-drop-table --result-file=%BACKUP_FILE% --host=%HOST% --port=%PORT% --user=%1 --password=%2 %DATABASE%
     exit /b !errorlevel!

:EOF
endlocal