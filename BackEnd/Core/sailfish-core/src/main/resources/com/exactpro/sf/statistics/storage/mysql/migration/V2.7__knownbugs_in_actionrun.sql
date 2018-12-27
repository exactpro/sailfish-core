CREATE TABLE `stknown_bugs` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `known_bug` varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `known_bug` (`known_bug`)
);

CREATE TABLE `stactionruns_known_bugs` (
    `stactionrun_id` bigint(20) NOT NULL,
    `known_bug_id` bigint(20) NOT NULL,
    `reproduced` boolean NOT NULL,
    PRIMARY KEY (`stactionrun_id`, `known_bug_id`),
    KEY `stactionrun_id_fk` (`stactionrun_id`),
    KEY `known_bug_id_fk` (`known_bug_id`),
    CONSTRAINT `stactionrun_id_fk` FOREIGN KEY (`stactionrun_id`) REFERENCES stactionruns(`id`),
    CONSTRAINT `known_bug_id_fk` FOREIGN KEY (`known_bug_id`) REFERENCES stknown_bugs(`id`)
);