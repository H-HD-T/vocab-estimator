-- Drop existing database to allow clean re-run
DROP DATABASE IF EXISTS vocab_estimator;
-- =============================================================
-- English Vocabulary Estimation Tool - Database Initialization
-- Database: vocab_estimator
-- MySQL 8.0+
-- =============================================================

CREATE DATABASE IF NOT EXISTS vocab_estimator
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE vocab_estimator;

-- =============================================================
-- Table 1: voc_word - Standard vocabulary library
-- Stores words with difficulty levels and frequency information
-- =============================================================
CREATE TABLE IF NOT EXISTS voc_word (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    word VARCHAR(100) NOT NULL COMMENT 'Word',
    difficulty CHAR(1) NOT NULL COMMENT 'Difficulty: K(Primary), P(Junior), F(Senior), C(College+)',
    frequency DOUBLE DEFAULT 0.5 COMMENT 'Word frequency (0-1), higher = more common',
    definition VARCHAR(500) DEFAULT '' COMMENT 'Chinese definition',
    cet_label VARCHAR(10) DEFAULT 'NONE' COMMENT 'CET label: CET4, CET6, BOTH, NONE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    UNIQUE KEY uk_word (word),
    INDEX idx_difficulty (difficulty),
    INDEX idx_cet_label (cet_label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Standard vocabulary library';

-- =============================================================
-- Table 2: user_info - User test records
-- =============================================================
CREATE TABLE IF NOT EXISTS user_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    student_code VARCHAR(50) NOT NULL COMMENT 'Student ID / alias',
    name_alias VARCHAR(50) DEFAULT '' COMMENT 'Name alias',
    cet4_score INT DEFAULT NULL COMMENT 'CET-4 score',
    cet6_score INT DEFAULT NULL COMMENT 'CET-6 score',
    student_type VARCHAR(20) DEFAULT '' COMMENT 'Student type: CORPUS_C/F/P/K',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    UNIQUE KEY uk_student_code (student_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User test records';

-- =============================================================
-- Table 3: test_record - Individual test records
-- =============================================================
CREATE TABLE IF NOT EXISTS test_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    user_id BIGINT NOT NULL COMMENT 'User ID',
    test_words TEXT COMMENT 'Test word list (JSON format)',
    known_count INT DEFAULT 0 COMMENT 'Number of known words',
    unknown_count INT DEFAULT 0 COMMENT 'Number of unknown words',
    estimate_vocab INT DEFAULT 0 COMMENT 'Estimated vocabulary size',
    min_range INT DEFAULT 0 COMMENT 'Lower bound of estimation',
    max_range INT DEFAULT 0 COMMENT 'Upper bound of estimation',
    confidence DOUBLE DEFAULT 0 COMMENT 'Confidence level (0-100)',
    test_type VARCHAR(10) DEFAULT 'GUI' COMMENT 'Test type: GUI, BATCH',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Test time',
    INDEX idx_user_id (user_id),
    INDEX idx_test_type (test_type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Individual test records';

-- =============================================================
-- Table 4: batch_task - Batch processing tasks
-- =============================================================
CREATE TABLE IF NOT EXISTS batch_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    word_text TEXT COMMENT 'Uploaded word list text',
    batch_result TEXT COMMENT 'Batch estimation results (JSON)',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'Status: PENDING, PROCESSING, COMPLETED, FAILED',
    remark VARCHAR(500) DEFAULT '' COMMENT 'Remark',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Batch processing tasks';

-- =============================================================
-- Table 5: corpus_data - Learner corpus data (C/F/P/K)
-- =============================================================
CREATE TABLE IF NOT EXISTS corpus_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    corpus_type CHAR(1) NOT NULL COMMENT 'Corpus type: C(College), F(Senior), P(Junior), K(Primary)',
    raw_text LONGTEXT COMMENT 'Raw text content',
    extracted_words TEXT COMMENT 'Extracted word list (JSON array)',
    analysis_result TEXT COMMENT 'Analysis result (JSON)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    INDEX idx_corpus_type (corpus_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Learner corpus data';

-- =============================================================
-- Initial vocabulary data (sample words for all 4 levels)
-- =============================================================


-- =============================================================
-- Word data: see sql/word_data.sql for full word import (7533 words)
-- Run: SOURCE sql/word_data.sql  (after init.sql)
-- =============================================================

-- Sample test user
INSERT INTO user_info (student_code, name_alias, cet4_score, cet6_score, student_type) VALUES
('TEST_USER_001', 'TStudent', 480, 425, 'CORPUS_C');

-- Sample test record
INSERT INTO test_record (user_id, test_words, known_count, unknown_count, estimate_vocab, min_range, max_range, confidence, test_type) VALUES
(1, '[{"word":"apple","known":true},{"word":"abandon","known":true},{"word":"abolish","known":false}]', 2, 1, 8500, 5000, 12000, 65.0, 'GUI');


-- =============================================================
-- Table 6: validation_sample - TestYourVocab validation samples
-- Stores each collected sample Ri, Ui, Ci, Di
-- =============================================================
CREATE TABLE IF NOT EXISTS validation_sample (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    known_words TEXT COMMENT 'Known words list (JSON array)',
    unknown_words TEXT COMMENT 'Unknown words list (JSON array)',
    standard_estimate INT DEFAULT 0 COMMENT 'Ci: TestYourVocab estimate',
    algorithm_estimate INT DEFAULT 0 COMMENT 'Di: Our algorithm estimate',
    known_count INT DEFAULT 0 COMMENT 'Number of known words',
    unknown_count INT DEFAULT 0 COMMENT 'Number of unknown words',
    diff INT DEFAULT 0 COMMENT 'Di - Ci difference',
    absolute_error INT DEFAULT 0 COMMENT '|Di - Ci| absolute error',
    relative_error DOUBLE DEFAULT 0 COMMENT '|Di-Ci|/Ci relative error',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TestYourVocab validation samples';
