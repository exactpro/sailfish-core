CREATE SEQUENCE sttcrstatus_sequence
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;

CREATE TABLE sttcrstatus (

  id bigint NOT NULL DEFAULT nextval('sttcrstatus_sequence'::regclass) PRIMARY KEY,
  name text NOT NULL UNIQUE

);

ALTER TABLE sttestcaseruns
  ADD COLUMN status_id bigint DEFAULT NULL,
  ADD COLUMN comment text DEFAULT NULL,
  ADD COLUMN fixrevision text DEFAULT NULL,
  ADD CONSTRAINT status_fk FOREIGN KEY (status_id) REFERENCES sttcrstatus (id)
    ON UPDATE CASCADE ON DELETE SET NULL;
    
INSERT INTO sttcrstatus(name) VALUES ('Real issue');
INSERT INTO sttcrstatus(name) VALUES ('Issue in test');
INSERT INTO sttcrstatus(name) VALUES ('Fake pass');
INSERT INTO sttcrstatus(name) VALUES ('Other');