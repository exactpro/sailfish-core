ALTER TABLE stmatrixruns 
ADD `reportfolder` varchar(255) DEFAULT NULL AFTER `startTime`;

ALTER TABLE sttestcaseruns
ADD `reportfile` varchar(255) DEFAULT NULL AFTER `startTime`;