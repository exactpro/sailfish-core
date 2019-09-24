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

createDatabase()
{
    output=$(mysql --host=${HOST} --port=${PORT} --user=${SUPERUSER} --password=${SUPERPASSWORD} -e 'SELECT VERSION();')
    local IS_LOGIN=$?
    if [ "$IS_LOGIN" -ne "0" ]; then
        echo "Error determining mysql version"
        echo $output
        return 1
    fi

    echo "$output" | grep '^5.'
    local IS_FIFTH_VERSION=$?

    QUERY="drop database if exists $DATABASE;
    create database $DATABASE character set $CHAR;"

    if [ "$IS_FIFTH_VERSION" -eq "0" ]; then
        echo "Build query for MySQL 5"
        QUERY="$QUERY
            grant all on $DATABASE.* to '$USER'@'localhost' identified by '$PASSWORD' with grant option;
            grant all on $DATABASE.* to '$USER'@'localhost.localdomain' identified by '$PASSWORD' with grant option;"
    else
        echo "Build query for MySQL 8 or higher"
        QUERY="$QUERY
            create user if not exists '$USER'@'localhost' identified with mysql_native_password by '$PASSWORD';
            create user if not exists '$USER'@'localhost.localdomain' identified with mysql_native_password by '$PASSWORD';

            grant all on $DATABASE.* to '$USER'@'localhost';
            grant all on $DATABASE.* to '$USER'@'localhost.localdomain';"
    fi

    mysql --host=${HOST} --port=${PORT} --user=$1 --password=$2 -e "$QUERY"
}

restoreFromBackup()
{
    createDatabase $2 $3

    if [ "$?" != 0 ]; then
        echo "Can't recreate database $DATABASE"
        return 1
    fi
    mysql --host=${HOST} --port=${PORT} --user=$2 --password=$3 ${DATABASE} -e "source  $1"
}

# main script

HOST="localhost"
PORT="3306"

SUPERUSER="root"
SUPERPASSWORD=""

DATABASE=""
CHAR="utf8"
USER="sailfish"
PASSWORD="999"

BACKUP_FILE=""

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

if [ "$BACKUP_FILE" = "" ]; then
    echo "Backup file has not been set"
    exit 3
fi

restoreFromBackup ${BACKUP_FILE} ${SUPERUSER} ${SUPERPASSWORD}

if [ "$?" != 0 ]; then
    echo "Can't restore database $DATABASE from backup $BACKUP_FILE"
    exit 4
fi