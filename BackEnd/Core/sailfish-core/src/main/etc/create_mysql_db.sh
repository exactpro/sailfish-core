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

HOST="localhost"
PORT="3306"

SUPERUSER="root"
SUPERPASSWORD=""

DATABASE="sailfish"
CHAR="utf8"
USER="sailfish"
PASSWORD="999"
DUMPFILE="dump_user.sql"

# Backup users and users roles. WARNING: You need to installed mysqldump.
# mysqldump --host=$HOST --port=$PORT --user=$SUPERUSER --password=$SUPERPASSWORD $DATABASE APP_USER APP_USERS_ROLES > $DUMPFILE


QUERY="drop database if exists $DATABASE;
create database $DATABASE character set $CHAR;
grant all on $DATABASE.* to '$USER'@'localhost' identified by '$PASSWORD' with grant option;
grant all on $DATABASE.* to '$USER'@'localhost.localdomain' identified by '$PASSWORD' with grant option;"

mysql --host=$HOST --port=$PORT --user=$SUPERUSER --password=$SUPERPASSWORD -e "$QUERY"

# Restore users and users roles
# mysql --host=$HOST --port=$PORT --user=$USER --password=$PASSWORD $DATABASE < $DUMPFILE

# Uncomment for autoremove dump file.
# rm $DUMPFILE

