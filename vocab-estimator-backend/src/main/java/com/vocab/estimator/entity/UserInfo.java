package com.vocab.estimator.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("user_info")
public class UserInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String studentCode;
    private String nameAlias;
    private Integer cet4Score;
    private Integer cet6Score;
    private String studentType;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public UserInfo() {}

    public UserInfo(Long id, String studentCode, String nameAlias, Integer cet4Score, Integer cet6Score, String studentType, LocalDateTime createTime, LocalDateTime updateTime) {
        this.id = id;
        this.studentCode = studentCode;
        this.nameAlias = nameAlias;
        this.cet4Score = cet4Score;
        this.cet6Score = cet6Score;
        this.studentType = studentType;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentCode() { return this.studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getNameAlias() { return this.nameAlias; }
    public void setNameAlias(String nameAlias) { this.nameAlias = nameAlias; }

    public Integer getCet4Score() { return this.cet4Score; }
    public void setCet4Score(Integer cet4Score) { this.cet4Score = cet4Score; }

    public Integer getCet6Score() { return this.cet6Score; }
    public void setCet6Score(Integer cet6Score) { this.cet6Score = cet6Score; }

    public String getStudentType() { return this.studentType; }
    public void setStudentType(String studentType) { this.studentType = studentType; }

    public LocalDateTime getCreateTime() { return this.createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return this.updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
