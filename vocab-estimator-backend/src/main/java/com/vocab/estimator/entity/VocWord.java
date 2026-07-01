package com.vocab.estimator.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("voc_word")
public class VocWord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String word;
    private String difficulty;
    private Double frequency;
    private String definition;
    private String cetLabel;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public VocWord() {}

    public VocWord(Long id, String word, String difficulty, Double frequency, String definition, String cetLabel, LocalDateTime createTime) {
        this.id = id;
        this.word = word;
        this.difficulty = difficulty;
        this.frequency = frequency;
        this.definition = definition;
        this.cetLabel = cetLabel;
        this.createTime = createTime;
    }

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    public String getWord() { return this.word; }
    public void setWord(String word) { this.word = word; }

    public String getDifficulty() { return this.difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public Double getFrequency() { return this.frequency; }
    public void setFrequency(Double frequency) { this.frequency = frequency; }

    public String getDefinition() { return this.definition; }
    public void setDefinition(String definition) { this.definition = definition; }

    public String getCetLabel() { return this.cetLabel; }
    public void setCetLabel(String cetLabel) { this.cetLabel = cetLabel; }

    public LocalDateTime getCreateTime() { return this.createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
