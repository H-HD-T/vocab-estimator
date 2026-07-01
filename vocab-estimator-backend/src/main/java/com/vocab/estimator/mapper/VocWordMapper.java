package com.vocab.estimator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocab.estimator.entity.VocWord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * Vocabulary word mapper
 */
@Mapper
public interface VocWordMapper extends BaseMapper<VocWord> {
    
    /** Get random words by difficulty level */
    @Select("SELECT * FROM voc_word WHERE difficulty = #{difficulty} ORDER BY RAND() LIMIT #{limit}")
    List<VocWord> getRandomWordsByLevel(@Param("difficulty") String difficulty, @Param("limit") int limit);
    
    /** Get total count by difficulty */
    @Select("SELECT COUNT(*) FROM voc_word WHERE difficulty = #{difficulty}")
    int countByDifficulty(@Param("difficulty") String difficulty);
    
    /** Search words by keyword */
    @Select("SELECT * FROM voc_word WHERE word LIKE CONCAT('%', #{keyword}, '%') OR definition LIKE CONCAT('%', #{keyword}, '%')")
    List<VocWord> searchWords(@Param("keyword") String keyword);
    
    /** Get words by CET label */
    @Select("SELECT * FROM voc_word WHERE cet_label = #{cetLabel}")
    List<VocWord> getWordsByCetLabel(@Param("cetLabel") String cetLabel);
}
