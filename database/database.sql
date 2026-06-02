-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: mysql-myproject-tranbichngoc855-32f5.l.aivencloud.com    Database: defaultdb
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '0f9b6a64-4722-11f1-b240-9edfecf74bc8:1-33,
988938ac-4cd3-11f1-a210-7e15d6f2ef8a:1-1513,
b1d8ab12-4bc0-11f1-9b8a-86d55d2cbfcd:1-19,
e9ee1cfd-485c-11f1-bba7-067e14d5b24e:1-35';

--
-- Table structure for table `arts`
--

DROP TABLE IF EXISTS `arts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `arts` (
                        `year_created` int DEFAULT NULL,
                        `id` bigint NOT NULL,
                        `artist` varchar(255) DEFAULT NULL,
                        PRIMARY KEY (`id`),
                        CONSTRAINT `FKi1t66ivq6whg67tmpli43018v` FOREIGN KEY (`id`) REFERENCES `item` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `arts`
--

LOCK TABLES `arts` WRITE;
/*!40000 ALTER TABLE `arts` DISABLE KEYS */;
INSERT INTO `arts` VALUES (0,5,'Unknown'),(0,6,'Unknown'),(0,7,'Unknown'),(0,8,'Unknown'),(0,10,'Unknown');
/*!40000 ALTER TABLE `arts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `auctions`
--

DROP TABLE IF EXISTS `auctions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auctions` (
                            `bid_increment` double NOT NULL,
                            `current_price` double NOT NULL,
                            `start_price` double NOT NULL,
                            `end_time` datetime(6) NOT NULL,
                            `id` bigint NOT NULL AUTO_INCREMENT,
                            `item_id` bigint DEFAULT NULL,
                            `seller_id` bigint NOT NULL,
                            `start_time` datetime(6) DEFAULT NULL,
                            `version` bigint DEFAULT NULL,
                            `winner_id` bigint DEFAULT NULL,
                            `status` varchar(20) DEFAULT NULL,
                            `title` varchar(255) NOT NULL,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `UKj127fy4j3xa1ircf7vmx4lxqo` (`item_id`),
                            KEY `FK6wkb2nluea52laivh7wak1c0n` (`seller_id`),
                            KEY `FK8b9c66qobioih55ulgp3uxbxg` (`winner_id`),
                            CONSTRAINT `FK6wkb2nluea52laivh7wak1c0n` FOREIGN KEY (`seller_id`) REFERENCES `user` (`id`),
                            CONSTRAINT `FK8b9c66qobioih55ulgp3uxbxg` FOREIGN KEY (`winner_id`) REFERENCES `user` (`id`),
                            CONSTRAINT `FKh3q8j345egd7g78f2tbck62g` FOREIGN KEY (`item_id`) REFERENCES `item` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auctions`
--

LOCK TABLES `auctions` WRITE;
/*!40000 ALTER TABLE `auctions` DISABLE KEYS */;
INSERT INTO `auctions` VALUES (10,50,50,'2026-05-31 20:00:00.000000',5,5,2,'2026-05-31 15:00:00.000000',1,NULL,'CANCELED','Cortis'),(100,2000,1000,'2026-06-01 14:00:00.000000',6,6,3,'2026-05-31 14:00:00.000000',1,4,'RUNNING','Đêm đầy sao'),(100,1000,1000,'2026-06-01 14:00:00.000000',7,7,3,'2026-05-31 14:00:00.000000',0,NULL,'OPEN','Đêm đầy sao'),(10,60,60,'2026-05-31 22:00:00.000000',8,8,2,'2026-05-31 18:00:00.000000',0,NULL,'OPEN','flower shop'),(12,135,123,'2026-06-03 19:00:00.000000',9,9,2,'2026-05-31 00:00:00.000000',1,5,'RUNNING','même'),(123,123,123,'2026-05-31 22:00:00.000000',10,10,2,'2026-05-31 00:00:00.000000',0,NULL,'OPEN','Tranh nổi tiếng');
/*!40000 ALTER TABLE `auctions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `auto_bids`
--

DROP TABLE IF EXISTS `auto_bids`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auto_bids` (
                             `active` bit(1) NOT NULL,
                             `bid_increment` double DEFAULT '1',
                             `max_bid` double NOT NULL,
                             `auction_id` bigint NOT NULL,
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `registered_at` datetime(6) NOT NULL,
                             `user_id` bigint NOT NULL,
                             PRIMARY KEY (`id`),
                             KEY `FKgajarujcbb6cct3td6y2dcpsc` (`user_id`),
                             KEY `FK8vygi55uidmuhv6cp6m4eb5j8` (`auction_id`),
                             CONSTRAINT `FK8vygi55uidmuhv6cp6m4eb5j8` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`id`),
                             CONSTRAINT `FKgajarujcbb6cct3td6y2dcpsc` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auto_bids`
--

LOCK TABLES `auto_bids` WRITE;
/*!40000 ALTER TABLE `auto_bids` DISABLE KEYS */;
/*!40000 ALTER TABLE `auto_bids` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bid_history`
--

DROP TABLE IF EXISTS `bid_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bid_history` (
                               `bid_amount` double NOT NULL,
                               `auction_id` bigint NOT NULL,
                               `bid_time` datetime(6) NOT NULL,
                               `id` bigint NOT NULL AUTO_INCREMENT,
                               `user_id` bigint NOT NULL,
                               PRIMARY KEY (`id`),
                               KEY `idx_bid_history_user_time` (`user_id`,`bid_time`),
                               KEY `idx_bid_history_auction` (`auction_id`),
                               CONSTRAINT `FKbk0p9q0sxuoh7ab536xwo6yxx` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`id`),
                               CONSTRAINT `FKhfnu69bft4vsy0lf2d08jn8pi` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bid_history`
--

LOCK TABLES `bid_history` WRITE;
/*!40000 ALTER TABLE `bid_history` DISABLE KEYS */;
INSERT INTO `bid_history` VALUES (2000,6,'2026-05-31 15:26:05.693888',1,4),(135,9,'2026-05-31 17:17:15.694395',2,5);
/*!40000 ALTER TABLE `bid_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `electronics`
--

DROP TABLE IF EXISTS `electronics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `electronics` (
                               `id` bigint NOT NULL,
                               `brand` varchar(255) DEFAULT NULL,
                               `warranty_period` varchar(255) DEFAULT NULL,
                               PRIMARY KEY (`id`),
                               CONSTRAINT `FKp54gqhogr88h2917hw7dtik22` FOREIGN KEY (`id`) REFERENCES `item` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `electronics`
--

LOCK TABLES `electronics` WRITE;
/*!40000 ALTER TABLE `electronics` DISABLE KEYS */;
/*!40000 ALTER TABLE `electronics` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `item`
--

DROP TABLE IF EXISTS `item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `item` (
                        `bid_increment` double DEFAULT NULL,
                        `price` double DEFAULT NULL,
                        `end_time` datetime(6) DEFAULT NULL,
                        `id` bigint NOT NULL AUTO_INCREMENT,
                        `id_user` bigint NOT NULL,
                        `starting_time` datetime(6) DEFAULT NULL,
                        `categories` varchar(255) DEFAULT NULL,
                        `image_url` varchar(255) DEFAULT NULL,
                        `name` varchar(255) DEFAULT NULL,
                        `description` text,
                        `image_urls` longtext,
                        PRIMARY KEY (`id`),
                        KEY `idx_item_categories` (`categories`),
                        KEY `idx_item_seller` (`id_user`),
                        CONSTRAINT `FKihnm8gmsybv5ahjfcs80kfc11` FOREIGN KEY (`id_user`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `item`
--

LOCK TABLES `item` WRITE;
/*!40000 ALTER TABLE `item` DISABLE KEYS */;
INSERT INTO `item` VALUES (10,50,'2026-05-31 20:00:00.000000',5,2,'2026-05-31 15:00:00.000000','ART','/uploads/items/c07ec2e6-e0bd-4f32-93a2-a8ff93b1fd32.jpg','Cortis','Handsome boys','/uploads/items/c07ec2e6-e0bd-4f32-93a2-a8ff93b1fd32.jpg'),(100,1000,'2026-06-01 14:00:00.000000',6,3,'2026-05-31 14:00:00.000000','ART','/uploads/items/7a38e82d-1196-4154-9635-957e58b2a28d.jpg','Đêm đầy sao','Đêm đầy sao (tiếng Hà Lan: De sterrennacht) là một bức tranh của họa sĩ hậu ấn tượng người Hà Lan Vincent van Gogh. Được vẽ vào tháng 6 năm 1889, bức tranh miêu tả khung cảnh bên ngoài cửa sổ phòng bệnh của ông ở Saint-Rémy-de-Provence, miền Nam nước Pháp về đêm, mặc dù ông đã vẽ bức tranh vào ban ngày qua trí nhớ.','/uploads/items/7a38e82d-1196-4154-9635-957e58b2a28d.jpg'),(100,1000,'2026-06-01 14:00:00.000000',7,3,'2026-05-31 14:00:00.000000','ART','/uploads/items/7a00378e-763c-45af-bf91-c9c41d2c83e5.jpg','Đêm đầy sao','Đêm đầy sao (tiếng Hà Lan: De sterrennacht) là một bức tranh của họa sĩ hậu ấn tượng người Hà Lan Vincent van Gogh. Được vẽ vào tháng 6 năm 1889, bức tranh miêu tả khung cảnh bên ngoài cửa sổ phòng bệnh của ông ở Saint-Rémy-de-Provence, miền Nam nước Pháp về đêm, mặc dù ông đã vẽ bức tranh vào ban ngày qua trí nhớ.','/uploads/items/7a00378e-763c-45af-bf91-c9c41d2c83e5.jpg'),(10,60,'2026-05-31 22:00:00.000000',8,2,'2026-05-31 18:00:00.000000','ART','https://res.cloudinary.com/dt28irsgx/image/upload/v1780222411/auction/items/bahndpbq34e6zmwajjsh.jpg','flower shop','Rose Flower','https://res.cloudinary.com/dt28irsgx/image/upload/v1780222411/auction/items/bahndpbq34e6zmwajjsh.jpg'),(12,123,'2026-06-03 19:00:00.000000',9,2,'2026-05-31 00:00:00.000000','VEHICLE','https://res.cloudinary.com/dt28irsgx/image/upload/v1780222564/auction/items/zuxv2u0eprjerftwbypi.png','même','cortis','https://res.cloudinary.com/dt28irsgx/image/upload/v1780222564/auction/items/zuxv2u0eprjerftwbypi.png'),(123,123,'2026-05-31 22:00:00.000000',10,2,'2026-05-31 00:00:00.000000','ART','https://res.cloudinary.com/dt28irsgx/image/upload/v1780223953/auction/items/vfxw5btykvcxntwltt59.jpg','Tranh nổi tiếng','Tranh nổi tiếng','https://res.cloudinary.com/dt28irsgx/image/upload/v1780223953/auction/items/vfxw5btykvcxntwltt59.jpg,https://res.cloudinary.com/dt28irsgx/image/upload/v1780223955/auction/items/ybvc4lhc7lu8nouycuyw.jpg');
/*!40000 ALTER TABLE `item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification`
--

DROP TABLE IF EXISTS `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
                                `is_handled` bit(1) NOT NULL,
                                `auction_id` bigint DEFAULT NULL,
                                `created_at` datetime(6) NOT NULL,
                                `deadline` datetime(6) DEFAULT NULL,
                                `id` bigint NOT NULL AUTO_INCREMENT,
                                `user_id` bigint NOT NULL,
                                `message` varchar(500) DEFAULT NULL,
                                `type` varchar(255) NOT NULL,
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification`
--

LOCK TABLES `notification` WRITE;
/*!40000 ALTER TABLE `notification` DISABLE KEYS */;
INSERT INTO `notification` VALUES (_binary '',2,'2026-05-31 13:44:00.579385',NULL,1,2,'Phiên đấu giá sản phẩm \'The Kiss (Nụ hôn)\' của bạn đã kết thúc nhưng không có thành viên nào tham gia đặt giá.','AUCTION_CANCELED'),(_binary '',3,'2026-05-31 14:00:01.993636',NULL,3,2,'Phiên đấu giá sản phẩm \'Creation of Adam (Chúa Tạo Ra Adam)\' của bạn đã kết thúc nhưng không có thành viên nào tham gia đặt giá.','AUCTION_CANCELED'),(_binary '',5,'2026-05-31 20:00:01.456215',NULL,4,2,'Phiên đấu giá sản phẩm \'Cortis\' của bạn đã kết thúc nhưng không có thành viên nào tham gia đặt giá.','AUCTION_CANCELED');
/*!40000 ALTER TABLE `notification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
                        `id` bigint NOT NULL AUTO_INCREMENT,
                        `rolename` varchar(255) DEFAULT NULL,
                        PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role`
--

LOCK TABLES `role` WRITE;
/*!40000 ALTER TABLE `role` DISABLE KEYS */;
INSERT INTO `role` VALUES (1,'ADMIN'),(2,'SELLER'),(3,'BIDDER');
/*!40000 ALTER TABLE `role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `seller_registrations`
--

DROP TABLE IF EXISTS `seller_registrations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seller_registrations` (
                                        `created_at` datetime(6) NOT NULL,
                                        `id` bigint NOT NULL,
                                        `user_id` bigint DEFAULT NULL,
                                        `address` varchar(255) DEFAULT NULL,
                                        `email` varchar(255) DEFAULT NULL,
                                        `identity_number` varchar(255) DEFAULT NULL,
                                        `name` varchar(255) DEFAULT NULL,
                                        `phone_number` varchar(255) DEFAULT NULL,
                                        `status` varchar(255) DEFAULT NULL,
                                        `identified_image_behind` longtext,
                                        `identified_image_front` longtext,
                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `UKqpb4lk05n6genahusmrgtlpri` (`user_id`),
                                        CONSTRAINT `FKroxrsa1pf04o9ymwuahhgkbop` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `seller_registrations`
--

LOCK TABLES `seller_registrations` WRITE;
/*!40000 ALTER TABLE `seller_registrations` DISABLE KEYS */;
INSERT INTO `seller_registrations` VALUES ('2026-05-31 13:14:57.432380',2,2,'VN','quynhnhu@gmail.com','0123','LE THI QUYNH NHU ','09123','APPROVED','/uploads/cccd/05896e51-3c0f-4dee-9add-af9a084bf91b.jpg','/uploads/cccd/857e2d7a-e51f-409a-ba57-5b6431490427.jpg'),('2026-05-31 14:54:47.670121',3,3,'hung','hung1@gmail.com','123','hung','123','APPROVED','/uploads/cccd/d8b62f94-2045-4840-ba2e-83a2e702bbd5.jpg','/uploads/cccd/f7e70019-558f-442c-b774-1906299d36a1.jpg'),('2026-05-31 17:24:13.995652',5,5,'HN','nhi@gmail.com','0132665','Flower shop','0130554','REJECTED','https://res.cloudinary.com/dt28irsgx/image/upload/v1780223059/auction/cccd/ejm8tzv2p6jowxbt0ns4.png','https://res.cloudinary.com/dt28irsgx/image/upload/v1780223057/auction/cccd/uj8dtw1um66gwxqs25zf.png');
/*!40000 ALTER TABLE `seller_registrations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
                        `balance` double DEFAULT NULL,
                        `freeze_balance` double DEFAULT '0',
                        `id` bigint NOT NULL AUTO_INCREMENT,
                        `email` varchar(255) DEFAULT NULL,
                        `name` varchar(255) DEFAULT NULL,
                        `password` varchar(255) DEFAULT NULL,
                        PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (0,0,1,'admin@auction.local','admin','admin'),(0,0,2,'quynhnhu@gmail.com','LE THI QUYNH NHU','123'),(0,0,3,'hung1@gmail.com','hung1','hung'),(498000,2000,4,'hung2@gmail.com','hung2','hung'),(49865,135,5,'nhi@gmail.com','nguyen tran yen nhi','123');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
                              `id_role` bigint NOT NULL,
                              `id_user` bigint NOT NULL,
                              PRIMARY KEY (`id_role`,`id_user`),
                              KEY `FKok1v2uejpjcfqg8va888yvy0w` (`id_user`),
                              CONSTRAINT `FKok1v2uejpjcfqg8va888yvy0w` FOREIGN KEY (`id_user`) REFERENCES `user` (`id`),
                              CONSTRAINT `FKrcmv344t6l0beetcs8u4xhpd` FOREIGN KEY (`id_role`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` VALUES (1,1),(2,2),(3,2),(2,3),(3,3),(3,4),(3,5);
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vehicles`
--

DROP TABLE IF EXISTS `vehicles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vehicles` (
                            `id` bigint NOT NULL,
                            PRIMARY KEY (`id`),
                            CONSTRAINT `FKk6lwju50gya4qpbahomon30nb` FOREIGN KEY (`id`) REFERENCES `item` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vehicles`
--

LOCK TABLES `vehicles` WRITE;
/*!40000 ALTER TABLE `vehicles` DISABLE KEYS */;
INSERT INTO `vehicles` VALUES (9);
/*!40000 ALTER TABLE `vehicles` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-31 21:02:44
