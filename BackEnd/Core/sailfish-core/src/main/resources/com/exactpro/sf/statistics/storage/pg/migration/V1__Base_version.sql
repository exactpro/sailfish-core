--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: stactionruns; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE stactionruns (
    id bigint NOT NULL,
    description character varying(255),
    failreason character varying(255),
    passed boolean NOT NULL,
    rank bigint NOT NULL,
    action_id bigint NOT NULL,
    msg_type_id bigint,
    service_id bigint,
    tc_run_id bigint NOT NULL
);


--
-- Name: stactionruns_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE stactionruns_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stactions; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE stactions (
    id bigint NOT NULL,
    name character varying(255)
);


--
-- Name: stactions_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE stactions_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stenvironments; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE stenvironments (
    id bigint NOT NULL,
    name character varying(255)
);


--
-- Name: stenvironments_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE stenvironments_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stmatrices; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE stmatrices (
    id bigint NOT NULL,
    name character varying(255)
);


--
-- Name: stmatrices_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE stmatrices_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stmatrixruns; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE stmatrixruns (
    id bigint NOT NULL,
    finishtime timestamp without time zone,
    sfrunid bigint NOT NULL,
    starttime timestamp without time zone,
    environment_id bigint NOT NULL,
    matrix_id bigint NOT NULL,
    sf_id bigint NOT NULL,
    user_id bigint NOT NULL
);


--
-- Name: stmatrixruns_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE stmatrixruns_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stmessagetypes; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE stmessagetypes (
    id bigint NOT NULL,
    name character varying(255)
);


--
-- Name: stmessagetypes_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE stmessagetypes_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stservices; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE stservices (
    id bigint NOT NULL,
    name character varying(255)
);


--
-- Name: stservices_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE stservices_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stsf_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE stsf_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stsfinstances; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE stsfinstances (
    id bigint NOT NULL,
    host character varying(255),
    name character varying(255),
    port integer NOT NULL
);


--
-- Name: sttestcaseruns; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sttestcaseruns (
    id bigint NOT NULL,
    description character varying(255),
    failreason character varying(255),
    finishtime timestamp without time zone,
    passed boolean,
    rank bigint NOT NULL,
    starttime timestamp without time zone,
    matrix_run_id bigint NOT NULL,
    tc_id bigint NOT NULL
);


--
-- Name: sttestcaseruns_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE sttestcaseruns_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sttestcases; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sttestcases (
    id bigint NOT NULL,
    testcaseid character varying(255)
);


--
-- Name: sttestcases_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE sttestcases_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stusers; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE stusers (
    id bigint NOT NULL,
    name character varying(255)
);


--
-- Name: stusers_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE stusers_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stactionruns_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY stactionruns
    ADD CONSTRAINT stactionruns_pkey PRIMARY KEY (id);


--
-- Name: stactions_name_key; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY stactions
    ADD CONSTRAINT stactions_name_key UNIQUE (name);


--
-- Name: stactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY stactions
    ADD CONSTRAINT stactions_pkey PRIMARY KEY (id);


--
-- Name: stenvironments_name_key; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY stenvironments
    ADD CONSTRAINT stenvironments_name_key UNIQUE (name);


--
-- Name: stenvironments_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY stenvironments
    ADD CONSTRAINT stenvironments_pkey PRIMARY KEY (id);


--
-- Name: stmatrices_name_key; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY stmatrices
    ADD CONSTRAINT stmatrices_name_key UNIQUE (name);


--
-- Name: stmatrices_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY stmatrices
    ADD CONSTRAINT stmatrices_pkey PRIMARY KEY (id);


--
-- Name: stmatrixruns_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY stmatrixruns
    ADD CONSTRAINT stmatrixruns_pkey PRIMARY KEY (id);


--
-- Name: stmessagetypes_name_key; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY stmessagetypes
    ADD CONSTRAINT stmessagetypes_name_key UNIQUE (name);


--
-- Name: stmessagetypes_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY stmessagetypes
    ADD CONSTRAINT stmessagetypes_pkey PRIMARY KEY (id);


--
-- Name: stservices_name_key; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY stservices
    ADD CONSTRAINT stservices_name_key UNIQUE (name);


--
-- Name: stservices_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY stservices
    ADD CONSTRAINT stservices_pkey PRIMARY KEY (id);


--
-- Name: stsfinstances_host_port_name_key; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY stsfinstances
    ADD CONSTRAINT stsfinstances_host_port_name_key UNIQUE (host, port, name);


--
-- Name: stsfinstances_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY stsfinstances
    ADD CONSTRAINT stsfinstances_pkey PRIMARY KEY (id);


--
-- Name: sttestcaseruns_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY sttestcaseruns
    ADD CONSTRAINT sttestcaseruns_pkey PRIMARY KEY (id);


--
-- Name: sttestcases_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY sttestcases
    ADD CONSTRAINT sttestcases_pkey PRIMARY KEY (id);


--
-- Name: sttestcases_testcaseid_key; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY sttestcases
    ADD CONSTRAINT sttestcases_testcaseid_key UNIQUE (testcaseid);


--
-- Name: stusers_name_key; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY stusers
    ADD CONSTRAINT stusers_name_key UNIQUE (name);


--
-- Name: stusers_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY stusers
    ADD CONSTRAINT stusers_pkey PRIMARY KEY (id);


--
-- Name: fk5add539f72ee26c6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY stactionruns
    ADD CONSTRAINT fk5add539f72ee26c6 FOREIGN KEY (action_id) REFERENCES stactions(id);


--
-- Name: fk5add539f7937c17; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY stactionruns
    ADD CONSTRAINT fk5add539f7937c17 FOREIGN KEY (msg_type_id) REFERENCES stmessagetypes(id);


--
-- Name: fk5add539fd1262a4e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY stactionruns
    ADD CONSTRAINT fk5add539fd1262a4e FOREIGN KEY (service_id) REFERENCES stservices(id);


--
-- Name: fk5add539fe760261c; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY stactionruns
    ADD CONSTRAINT fk5add539fe760261c FOREIGN KEY (tc_run_id) REFERENCES sttestcaseruns(id) ON DELETE CASCADE;


--
-- Name: fk782dafaa37258b8e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY stmatrixruns
    ADD CONSTRAINT fk782dafaa37258b8e FOREIGN KEY (environment_id) REFERENCES stenvironments(id);


--
-- Name: fk782dafaab22b055b; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY stmatrixruns
    ADD CONSTRAINT fk782dafaab22b055b FOREIGN KEY (sf_id) REFERENCES stsfinstances(id);


--
-- Name: fk782dafaab9cab6e6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY stmatrixruns
    ADD CONSTRAINT fk782dafaab9cab6e6 FOREIGN KEY (matrix_id) REFERENCES stmatrices(id);


--
-- Name: fk782dafaaeada85a6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY stmatrixruns
    ADD CONSTRAINT fk782dafaaeada85a6 FOREIGN KEY (user_id) REFERENCES stusers(id);


--
-- Name: fkb20d11eb36849999; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY sttestcaseruns
    ADD CONSTRAINT fkb20d11eb36849999 FOREIGN KEY (tc_id) REFERENCES sttestcases(id);


--
-- Name: fkb20d11ebf960dc4b; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY sttestcaseruns
    ADD CONSTRAINT fkb20d11ebf960dc4b FOREIGN KEY (matrix_run_id) REFERENCES stmatrixruns(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

