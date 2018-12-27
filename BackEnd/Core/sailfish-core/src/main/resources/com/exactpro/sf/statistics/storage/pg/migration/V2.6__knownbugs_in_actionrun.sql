CREATE SEQUENCE stknown_bugs_sequence
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;

CREATE TABLE stknown_bugs (
    id bigint NOT NULL DEFAULT nextval('stknown_bugs_sequence'::regclass) PRIMARY KEY,
    known_bug character varying(255) NOT NULL UNIQUE
);

CREATE TABLE stactionruns_known_bugs (
    stactionrun_id bigint NOT NULL,
    known_bug_id bigint NOT NULL,
    reproduced boolean NOT NULL
);

ALTER TABLE ONLY stactionruns_known_bugs
    ADD CONSTRAINT stactionruns_known_bugs_pkey PRIMARY KEY (stactionrun_id, known_bug_id),
    ADD CONSTRAINT stactionrun_id_fkey FOREIGN KEY (stactionrun_id) REFERENCES stactionruns(id),
    ADD CONSTRAINT known_bug_id_fkey FOREIGN KEY (known_bug_id) REFERENCES stknown_bugs(id)