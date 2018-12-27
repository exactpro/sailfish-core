create database sailfish character set utf8;
grant all on sailfish.* to 'sailfish'@'localhost' identified by '999' with grant option;
grant all on sailfish.* to 'sailfish'@'localhost.localdomain' identified by '999' with grant option;
