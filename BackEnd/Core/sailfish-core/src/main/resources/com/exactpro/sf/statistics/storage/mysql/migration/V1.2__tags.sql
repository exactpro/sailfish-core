CREATE TABLE `sttags` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
);

CREATE TABLE `stmrtags` (
  `mr_id` bigint(20) NOT NULL,
  `tag_id` bigint(20) NOT NULL,
  PRIMARY KEY (`mr_id`,`tag_id`),
  KEY `FK62F6E8DF5AEC20B3` (`mr_id`),
  KEY `FK62F6E8DF1DA7BE2E` (`tag_id`),
  CONSTRAINT `FK62F6E8DF1DA7BE2E` FOREIGN KEY (`tag_id`) REFERENCES `sttags` (`id`),
  CONSTRAINT `FK62F6E8DF5AEC20B3` FOREIGN KEY (`mr_id`) REFERENCES `stmatrixruns` (`id`)
);