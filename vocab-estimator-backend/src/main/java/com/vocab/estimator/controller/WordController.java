package com.vocab.estimator.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vocab.estimator.common.Result;
import com.vocab.estimator.entity.VocWord;
import com.vocab.estimator.service.VocWordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/words")
public class WordController {

    @Autowired
    private VocWordService vocWordService;

    @GetMapping("/page")
    public Result<Page<VocWord>> page(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String difficulty) {
        LambdaQueryWrapper<VocWord> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(VocWord::getWord, keyword).or().like(VocWord::getDefinition, keyword);
        }
        if (difficulty != null && !difficulty.isEmpty()) {
            wrapper.eq(VocWord::getDifficulty, difficulty);
        }
        return Result.success(vocWordService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/{id}")
    public Result<VocWord> getById(@PathVariable Long id) {
        return Result.success(vocWordService.getById(id));
    }

    @PostMapping
    public Result<VocWord> add(@RequestBody VocWord word) {
        vocWordService.save(word);
        return Result.success(word);
    }

    @PutMapping
    public Result<VocWord> update(@RequestBody VocWord word) {
        vocWordService.updateById(word);
        return Result.success(word);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        vocWordService.removeById(id);
        return Result.success(null);
    }

    @GetMapping("/search")
    public Result<List<VocWord>> search(@RequestParam String keyword) {
        return Result.success(vocWordService.searchWords(keyword));
    }

    @GetMapping("/count")
    public Result<Integer> count() {
        return Result.success(vocWordService.getWordCount());
    }
}
