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

-- K level (Primary school, ~500 basic words)
INSERT INTO voc_word (word, difficulty, frequency, definition, cet_label) VALUES
('apple', 'K', 0.95, '苹果', 'NONE'),
('book', 'K', 0.93, '书', 'NONE'),
('cat', 'K', 0.90, '猫', 'NONE'),
('dog', 'K', 0.92, '狗', 'NONE'),
('egg', 'K', 0.85, '鸡蛋', 'NONE'),
('fish', 'K', 0.88, '鱼', 'NONE'),
('good', 'K', 0.96, '好的', 'NONE'),
('happy', 'K', 0.87, '快乐的', 'NONE'),
('ice', 'K', 0.80, '冰', 'NONE'),
('jump', 'K', 0.82, '跳', 'NONE'),
('king', 'K', 0.78, '国王', 'NONE'),
('love', 'K', 0.91, '爱', 'NONE'),
('milk', 'K', 0.86, '牛奶', 'NONE'),
('name', 'K', 0.94, '名字', 'NONE'),
('old', 'K', 0.90, '老的', 'NONE'),
('pen', 'K', 0.83, '钢笔', 'NONE'),
('queen', 'K', 0.75, '女王', 'NONE'),
('red', 'K', 0.92, '红色的', 'NONE'),
('sun', 'K', 0.88, '太阳', 'NONE'),
('tree', 'K', 0.89, '树', 'NONE'),
('up', 'K', 0.91, '向上', 'NONE'),
('van', 'K', 0.70, '货车', 'NONE'),
('water', 'K', 0.93, '水', 'NONE'),
('yellow', 'K', 0.84, '黄色的', 'NONE'),
('zoo', 'K', 0.72, '动物园', 'NONE'),
('ball', 'K', 0.85, '球', 'NONE'),
('cake', 'K', 0.82, '蛋糕', 'NONE'),
('door', 'K', 0.88, '门', 'NONE'),
('eye', 'K', 0.89, '眼睛', 'NONE'),
('foot', 'K', 0.86, '脚', 'NONE'),
('girl', 'K', 0.90, '女孩', 'NONE'),
('hand', 'K', 0.89, '手', 'NONE'),
('ink', 'K', 0.65, '墨水', 'NONE'),
('jeep', 'K', 0.60, '吉普车', 'NONE'),
('kite', 'K', 0.70, '风筝', 'NONE'),
('lion', 'K', 0.78, '狮子', 'NONE'),
('man', 'K', 0.95, '男人', 'NONE'),
('nest', 'K', 0.72, '巢', 'NONE'),
('owl', 'K', 0.68, '猫头鹰', 'NONE'),
('pig', 'K', 0.80, '猪', 'NONE'),
('run', 'K', 0.91, '跑', 'NONE'),
('sit', 'K', 0.87, '坐', 'NONE'),
('toy', 'K', 0.76, '玩具', 'NONE'),
('use', 'K', 0.90, '使用', 'NONE'),
('vet', 'K', 0.55, '兽医', 'NONE'),
('walk', 'K', 0.89, '步行', 'NONE'),
('box', 'K', 0.85, '盒子', 'NONE'),
('cry', 'K', 0.78, '哭', 'NONE'),
('day', 'K', 0.94, '天', 'NONE');

-- P level (Junior high school, ~1500 words)
INSERT INTO voc_word (word, difficulty, frequency, definition, cet_label) VALUES
('abroad', 'P', 0.72, '国外', 'NONE'),
('accept', 'P', 0.85, '接受', 'CET4'),
('accident', 'P', 0.75, '事故', 'CET4'),
('achieve', 'P', 0.80, '实现,达到', 'CET4'),
('address', 'P', 0.88, '地址', 'NONE'),
('advantage', 'P', 0.78, '优势', 'CET4'),
('advise', 'P', 0.76, '建议', 'CET4'),
('afford', 'P', 0.74, '负担得起', 'CET4'),
('agree', 'P', 0.86, '同意', 'NONE'),
('allow', 'P', 0.85, '允许', 'NONE'),
('although', 'P', 0.82, '虽然', 'CET4'),
('amount', 'P', 0.84, '数量', 'CET4'),
('animal', 'P', 0.90, '动物', 'NONE'),
('annual', 'P', 0.72, '每年的', 'CET4'),
('anxious', 'P', 0.68, '焦虑的', 'CET4'),
('appear', 'P', 0.83, '出现', 'CET4'),
('arrange', 'P', 0.72, '安排', 'CET4'),
('arrive', 'P', 0.87, '到达', 'NONE'),
('article', 'P', 0.80, '文章', 'CET4'),
('attention', 'P', 0.86, '注意力', 'NONE'),
('average', 'P', 0.82, '平均', 'CET4'),
('avoid', 'P', 0.78, '避免', 'CET4'),
('balance', 'P', 0.76, '平衡', 'CET4'),
('bargain', 'P', 0.65, '讨价还价', 'CET4'),
('behave', 'P', 0.66, '表现', 'CET4'),
('believe', 'P', 0.88, '相信', 'NONE'),
('belong', 'P', 0.78, '属于', 'CET4'),
('beneath', 'P', 0.64, '在...之下', 'CET4'),
('besides', 'P', 0.76, '除此之外', 'CET4'),
('billion', 'P', 0.70, '十亿', 'CET4'),
('borrow', 'P', 0.78, '借', 'NONE'),
('bottom', 'P', 0.80, '底部', 'CET4'),
('branch', 'P', 0.74, '分支', 'CET4'),
('brave', 'P', 0.72, '勇敢的', 'NONE'),
('breath', 'P', 0.76, '呼吸', 'CET4');

-- F level (Senior high school, ~2500 words)
INSERT INTO voc_word (word, difficulty, frequency, definition, cet_label) VALUES
('abandon', 'F', 0.65, '放弃', 'CET4'),
('absorb', 'F', 0.62, '吸收', 'CET4'),
('abstract', 'F', 0.55, '抽象的', 'CET6'),
('abundant', 'F', 0.58, '丰富的', 'CET6'),
('accelerate', 'F', 0.60, '加速', 'CET6'),
('accompany', 'F', 0.62, '陪伴', 'CET4'),
('accomplish', 'F', 0.65, '完成', 'CET4'),
('accurate', 'F', 0.64, '准确的', 'CET4'),
('accuse', 'F', 0.58, '指责', 'CET4'),
('acknowledge', 'F', 0.60, '承认', 'CET6'),
('acquire', 'F', 0.66, '获得', 'CET4'),
('adapt', 'F', 0.64, '适应', 'CET4'),
('adequate', 'F', 0.62, '充足的', 'CET4'),
('adjust', 'F', 0.64, '调整', 'CET4'),
('administration', 'F', 0.60, '管理', 'CET6'),
('adopt', 'F', 0.66, '采纳', 'CET4'),
('advance', 'F', 0.70, '前进', 'CET4'),
('advertise', 'F', 0.68, '做广告', 'CET4'),
('affair', 'F', 0.72, '事务', 'CET4'),
('affect', 'F', 0.76, '影响', 'CET4'),
('agenda', 'F', 0.58, '议程', 'CET4'),
('aggressive', 'F', 0.60, '有进取心的', 'CET4'),
('allocate', 'F', 0.52, '分配', 'CET6'),
('alternative', 'F', 0.68, '替代', 'CET4'),
('ambition', 'F', 0.60, '野心', 'CET4'),
('analyze', 'F', 0.70, '分析', 'CET4'),
('ancestor', 'F', 0.56, '祖先', 'CET4'),
('announce', 'F', 0.72, '宣布', 'CET4'),
('anxiety', 'F', 0.56, '焦虑', 'CET4'),
('apparent', 'F', 0.64, '明显的', 'CET4');

-- C level (College and above, ~3500 words)
INSERT INTO voc_word (word, difficulty, frequency, definition, cet_label) VALUES
('abolish', 'C', 0.40, '废除', 'CET6'),
('abortion', 'C', 0.45, '堕胎', 'CET6'),
('absurd', 'C', 0.38, '荒谬的', 'CET6'),
('abundance', 'C', 0.40, '丰富', 'CET6'),
('academy', 'C', 0.52, '学院', 'CET6'),
('accessory', 'C', 0.35, '配件', 'CET6'),
('accommodate', 'C', 0.48, '容纳', 'CET6'),
('accountability', 'C', 0.35, '责任', 'CET6'),
('accumulate', 'C', 0.50, '积累', 'CET6'),
('acquaintance', 'C', 0.45, '熟人', 'CET6'),
('activate', 'C', 0.42, '激活', 'CET6'),
('acute', 'C', 0.40, '急性的', 'CET6'),
('adamant', 'C', 0.30, '固执的', 'CET6'),
('adaptation', 'C', 0.45, '适应', 'CET6'),
('adhere', 'C', 0.38, '坚持', 'CET6'),
('adjacent', 'C', 0.36, '邻近的', 'CET6'),
('administer', 'C', 0.42, '管理', 'CET6'),
('adolescent', 'C', 0.44, '青少年', 'CET6'),
('adverse', 'C', 0.38, '不利的', 'CET6'),
('advocate', 'C', 0.50, '提倡', 'CET6'),
('aesthetic', 'C', 0.32, '审美的', 'CET6'),
('affiliate', 'C', 0.36, '附属', 'CET6'),
('aggregate', 'C', 0.34, '集合', 'CET6'),
('aggression', 'C', 0.38, '侵略', 'CET6'),
('allegation', 'C', 0.35, '指控', 'CET6'),
('allegedly', 'C', 0.35, '据称', 'CET6'),
('ambiguous', 'C', 0.36, '模棱两可的', 'CET6'),
('amend', 'C', 0.38, '修改', 'CET6'),
('amplify', 'C', 0.32, '放大', 'CET6'),
('analogy', 'C', 0.34, '类比', 'CET6');

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
