ALTER TABLE stactions MODIFY `name` varchar(255) NOT NULL;
ALTER TABLE stenvironments MODIFY `name` varchar(255) NOT NULL;
ALTER TABLE stmatrices MODIFY `name` varchar(255) NOT NULL;

ALTER TABLE stmessagetypes MODIFY `name` varchar(255) NOT NULL;
ALTER TABLE stservices MODIFY `name` varchar(255) NOT NULL;
ALTER TABLE sttestcases MODIFY `testcaseid` varchar(255) NOT NULL;