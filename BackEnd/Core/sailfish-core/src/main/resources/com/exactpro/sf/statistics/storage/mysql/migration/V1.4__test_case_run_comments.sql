CREATE TABLE sttcrstatus (

  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)

);

ALTER TABLE sttestcaseruns
  ADD status_id bigint(20) DEFAULT NULL AFTER `tc_id` ,
  ADD comment varchar(1024) DEFAULT NULL AFTER `status_id`,
  ADD fixrevision varchar(255) DEFAULT NULL AFTER `comment`,
  ADD CONSTRAINT `status_fk` FOREIGN KEY (`status_id`) REFERENCES `sttcrstatus` (id)
    ON UPDATE CASCADE ON DELETE SET NULL;
    
INSERT INTO sttcrstatus(name) VALUES ('Real issue');
INSERT INTO sttcrstatus(name) VALUES ('Issue in test');
INSERT INTO sttcrstatus(name) VALUES ('Fake pass');
INSERT INTO sttcrstatus(name) VALUES ('Other');