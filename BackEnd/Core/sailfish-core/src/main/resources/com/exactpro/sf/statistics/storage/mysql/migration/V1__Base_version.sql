-- MySQL dump 10.13  Distrib 5.5.43, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: sfstatistics
-- ------------------------------------------------------
-- Server version	5.5.43-0ubuntu0.12.04.1
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `stactionruns`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stactionruns` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `failReason` varchar(255) DEFAULT NULL,
  `passed` tinyint(1) NOT NULL,
  `rank` bigint(20) NOT NULL,
  `action_id` bigint(20) NOT NULL,
  `msg_type_id` bigint(20) DEFAULT NULL,
  `service_id` bigint(20) DEFAULT NULL,
  `tc_run_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK5ADD539F7937C17` (`msg_type_id`),
  KEY `FK5ADD539F72EE26C6` (`action_id`),
  KEY `FK5ADD539FE760261C` (`tc_run_id`),
  KEY `FK5ADD539FD1262A4E` (`service_id`),
  CONSTRAINT `FK5ADD539FD1262A4E` FOREIGN KEY (`service_id`) REFERENCES `stservices` (`id`),
  CONSTRAINT `FK5ADD539F72EE26C6` FOREIGN KEY (`action_id`) REFERENCES `stactions` (`id`),
  CONSTRAINT `FK5ADD539F7937C17` FOREIGN KEY (`msg_type_id`) REFERENCES `stmessagetypes` (`id`),
  CONSTRAINT `FK5ADD539FE760261C` FOREIGN KEY (`tc_run_id`) REFERENCES `sttestcaseruns` (`id`)
);
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stactions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stactions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
);
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stenvironments`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stenvironments` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
);
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stmatrices`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stmatrices` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
);
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stmatrixruns`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stmatrixruns` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `finishTime` datetime DEFAULT NULL,
  `sfRunId` bigint(20) NOT NULL,
  `startTime` datetime DEFAULT NULL,
  `environment_id` bigint(20) NOT NULL,
  `matrix_id` bigint(20) NOT NULL,
  `sf_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK782DAFAAB9CAB6E6` (`matrix_id`),
  KEY `FK782DAFAAEADA85A6` (`user_id`),
  KEY `FK782DAFAA37258B8E` (`environment_id`),
  KEY `FK782DAFAAB22B055B` (`sf_id`),
  CONSTRAINT `FK782DAFAAB22B055B` FOREIGN KEY (`sf_id`) REFERENCES `stsfinstances` (`id`),
  CONSTRAINT `FK782DAFAA37258B8E` FOREIGN KEY (`environment_id`) REFERENCES `stenvironments` (`id`),
  CONSTRAINT `FK782DAFAAB9CAB6E6` FOREIGN KEY (`matrix_id`) REFERENCES `stmatrices` (`id`),
  CONSTRAINT `FK782DAFAAEADA85A6` FOREIGN KEY (`user_id`) REFERENCES `stusers` (`id`)
);
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stmessagetypes`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stmessagetypes` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
);
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stservices`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stservices` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
);
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stsfinstances`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stsfinstances` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `host` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `port` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `host` (`host`,`port`,`name`)
);
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sttestcaseruns`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sttestcaseruns` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `failReason` varchar(255) DEFAULT NULL,
  `finishTime` datetime DEFAULT NULL,
  `passed` tinyint(1) DEFAULT NULL,
  `rank` bigint(20) NOT NULL,
  `startTime` datetime DEFAULT NULL,
  `matrix_run_id` bigint(20) NOT NULL,
  `tc_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKB20D11EBF960DC4B` (`matrix_run_id`),
  KEY `FKB20D11EB36849999` (`tc_id`),
  CONSTRAINT `FKB20D11EB36849999` FOREIGN KEY (`tc_id`) REFERENCES `sttestcases` (`id`),
  CONSTRAINT `FKB20D11EBF960DC4B` FOREIGN KEY (`matrix_run_id`) REFERENCES `stmatrixruns` (`id`)
);
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sttestcases`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sttestcases` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `testCaseId` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `testCaseId` (`testCaseId`)
);
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stusers`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stusers` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
);
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2015-07-14 19:27:01
