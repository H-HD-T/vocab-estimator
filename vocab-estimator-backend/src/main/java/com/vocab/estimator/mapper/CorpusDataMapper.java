package com.vocab.estimator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocab.estimator.entity.CorpusData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * Corpus data mapper
 */
@Mapper
public interface CorpusDataMapper extends BaseMapper<CorpusData> {
    
    @Select("SELECT * FROM corpus_data WHERE corpus_type = #{corpusType} ORDER BY create_time DESC LIMIT 1")
    CorpusData findLatestByType(String corpusType);
}
