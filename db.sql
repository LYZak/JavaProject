-- BigComp Access Control System Database
-- Exported from SQLite to MySQL format
-- Generated: 2025-12-30 09:43:37

SET FOREIGN_KEY_CHECKS = 0;

DROP DATABASE IF EXISTS access_control;
CREATE DATABASE access_control CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE access_control;

-- ============================================
-- Table Structure
-- ============================================

-- Table: users
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` VARCHAR(50) PRIMARY KEY,
  `first_name` VARCHAR(100) NOT NULL,
  `last_name` VARCHAR(100) NOT NULL,
  `gender` VARCHAR(20) NOT NULL,
  `user_type` VARCHAR(50) NOT NULL,
  `badge_id` VARCHAR(50),
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: badges
DROP TABLE IF EXISTS `badges`;
CREATE TABLE `badges` (
  `id` VARCHAR(50) PRIMARY KEY,
  `code` VARCHAR(50) UNIQUE NOT NULL,
  `user_id` VARCHAR(50) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: resources
DROP TABLE IF EXISTS `resources`;
CREATE TABLE `resources` (
  `id` VARCHAR(50) PRIMARY KEY,
  `name` VARCHAR(200) NOT NULL,
  `type` VARCHAR(50) NOT NULL,
  `location` VARCHAR(200),
  `building` VARCHAR(100),
  `floor` VARCHAR(50),
  `state` VARCHAR(20) DEFAULT 'CONTROLLED',
  `badge_reader_id` VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: badge_readers
DROP TABLE IF EXISTS `badge_readers`;
CREATE TABLE `badge_readers` (
  `id` VARCHAR(50) PRIMARY KEY,
  `resource_id` VARCHAR(50) NOT NULL,
  `is_active` BOOLEAN DEFAULT TRUE,
  FOREIGN KEY (`resource_id`) REFERENCES `resources`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: badge_profiles
DROP TABLE IF EXISTS `badge_profiles`;
CREATE TABLE `badge_profiles` (
  `badge_id` VARCHAR(50) NOT NULL,
  `profile_name` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`badge_id`, `profile_name`),
  FOREIGN KEY (`badge_id`) REFERENCES `badges`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: resource_group_members
DROP TABLE IF EXISTS `resource_group_members`;
CREATE TABLE `resource_group_members` (
  `resource_id` VARCHAR(50) NOT NULL,
  `group_name` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`resource_id`, `group_name`),
  FOREIGN KEY (`resource_id`) REFERENCES `resources`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table Data
-- ============================================

