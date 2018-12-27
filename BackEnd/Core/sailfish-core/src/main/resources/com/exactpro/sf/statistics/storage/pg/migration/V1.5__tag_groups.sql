CREATE SEQUENCE sttaggroups_sequence
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;

CREATE TABLE sttaggroups (
  id bigint NOT NULL DEFAULT nextval('sttaggroups_sequence'::regclass) PRIMARY KEY,
  name text NOT NULL UNIQUE
);

ALTER TABLE sttags 
  ADD COLUMN group_id bigint DEFAULT NULL,
  ADD CONSTRAINT group_id_fk FOREIGN KEY (group_id) REFERENCES sttaggroups (id)
    ON UPDATE CASCADE ON DELETE SET NULL;