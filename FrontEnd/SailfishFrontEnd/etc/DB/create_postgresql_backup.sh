#!/bin/sh
#*******************************************************************************
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
#*******************************************************************************
#################################################################################
# This script is intended to (re)create Sailfish database on MySQL server.	#
# You can modify variables as you need.						#
# But don't modify QUERY variable please.					#
# Run only on Linux OS.								#
#################################################################################

readParameters()
{
    case "$2" in
        -*|"") return 2 ;;
    esac

    case "$1" in
        -host)
            HOST="$2" ;;
        -port)
            PORT="$2" ;;
        -database)
            DATABASE="$2" ;;
        -user)
            USER="$2" ;;
        -password)
            PASSWORD="$2" ;;
        -superuser)
            SUPERUSER="$2" ;;
        -superpassword)
            SUPERPASSWORD="$2" ;;
        -backup)
            BACKUP_FILE="$2" ;;
        *) return 1 ;;
    esac
}

createBackup()
{
    pg_dump "postgresql://$1:$2@$HOST:$PORT/$DATABASE" --file=${BACKUP_FILE}
}

# main script

HOST="localhost"
PORT="5432"

SUPERUSER="postgres"
SUPERPASSWORD="pgpassword"

DATABASE=""
CHAR="utf8"
USER="sailfish"
PASSWORD="999"

BACKUP_FILE="backup.sql"

while [ "$1" != "" ]; do
    readParameters $1 $2
    RC=$?
    if [ "$RC" -eq "2" ]; then
        echo "Wrong parameter $2 for $1"
        exit 2
    elif [ "$RC" -eq "1" ]; then
        echo "Wrong argument $1"
        exit 2
    fi
    shift 2
done

if [ "$DATABASE" = "" ]; then
    echo "Database has not been set"
    exit 3
fi

createBackup ${SUPERUSER} ${SUPERPASSWORD}

if [ "$?" != 0 ]; then
    echo "Can't create backup for database $DATABASE"
    exit 4
fi
