CREATE TABLE sttags
(
  id bigint NOT NULL,
  name character varying(255),
  CONSTRAINT sttags_pkey PRIMARY KEY (id ),
  CONSTRAINT sttags_name_key UNIQUE (name )
);

CREATE TABLE stmrtags
(
  mr_id bigint NOT NULL,
  tag_id bigint NOT NULL,
  CONSTRAINT stmrtags_pkey PRIMARY KEY (mr_id , tag_id ),
  CONSTRAINT fk62f6e8df1da7be2e FOREIGN KEY (tag_id)
      REFERENCES sttags (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk62f6e8df5aec20b3 FOREIGN KEY (mr_id)
      REFERENCES stmatrixruns (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE SEQUENCE sttags_sequence
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;