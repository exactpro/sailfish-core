ALTER TABLE stmatrixruns 
ADD COLUMN reportfolder text DEFAULT NULL;

ALTER TABLE sttestcaseruns 
ADD COLUMN reportfile text DEFAULT NULL;