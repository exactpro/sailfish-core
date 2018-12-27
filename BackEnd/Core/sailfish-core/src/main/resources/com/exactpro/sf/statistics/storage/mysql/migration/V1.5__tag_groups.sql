CREATE TABLE sttaggroups (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
);

ALTER TABLE sttags 
  ADD group_id bigint(20) DEFAULT NULL AFTER `name`,
  ADD CONSTRAINT `group_id_fk` FOREIGN KEY (`group_id`) REFERENCES `sttaggroups` (id)
    ON UPDATE CASCADE ON DELETE SET NULL;