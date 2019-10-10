#!/bin/sh

USER='sailfish'
PASSWORD='999'
SUPERUSER='root'
SUPERPASSWORD='swordfish'

echo "Creating Sailfish DB.."
bash /var/scripts/create_mysql_db.sh \
    -user ${USER} \
    -password ${PASSWORD} \
    -superuser ${SUPERUSER} \
    -superpassword ${SUPERPASSWORD} \
    -database sailfish \
    -host localhost \
    -port 3306

echo "Creating Sailfish statistics DB.."
bash /var/scripts/create_mysql_db.sh \
    -user ${USER} \
    -password ${PASSWORD} \
    -superuser ${SUPERUSER} \
    -superpassword ${SUPERPASSWORD} \
    -database sfstatistics \
    -host localhost \
    -port 3306

QUERY="grant all on *.* to '$USER'@'%' identified by '$PASSWORD' with grant option"
mysql --host=${HOST} --port=${PORT} --user=${SUPERUSER} --password=${SUPERPASSWORD} -e "$QUERY"
