package com.vocab.estimator.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("batch_task")
public class BatchTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String wordText;
    private String batchResult;
    private String status;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public BatchTask() {}

    public BatchTask(Long id, String wordText, String batchResult, String status, String remark, LocalDateTime createTime, LocalDateTime updateTime) {
        this.id = id;
        this.wordText = wordText;
        this.batchResult = batchResult;
        this.status = status;
        this.remark = remark;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    public String getWordText() { return this.wordText; }
    public void setWordText(String wordText) { this.wordText = wordText; }

    public String getBatchResult() { return this.batchResult; }
    public void setBatchResult(String batchResult) { this.batchResult = batchResult; }

    public String getStatus() { return this.status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemark() { return this.remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreateTime() { return this.createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return this.updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
