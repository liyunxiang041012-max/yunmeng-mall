package com.liyun.item.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyun.common.utils.Result;
import com.liyun.item.domain.pojo.SpecTemplate;
import com.liyun.item.domain.pojo.SpecValue;
import com.liyun.item.mapper.SpecTemplateMapper;
import com.liyun.item.mapper.SpecValueMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 规格模板管理 - 商家维护规格方案
 */
@RestController
@RequestMapping("/spec-template")
@Tag(name = "规格模板管理", description = "商家管理规格模板（颜色/尺码等）")
@RequiredArgsConstructor
public class SpecTemplateController {

    private final SpecTemplateMapper templateMapper;
    private final SpecValueMapper valueMapper;

    // ==================== DTO ====================

    @lombok.Data
    public static class SpecTemplateSaveDTO {
        private Long categoryId;
        private String specName;
        private List<String> values;
    }

    @lombok.Data
    public static class SpecTemplateVO {
        private Long id;
        private Long categoryId;
        private String specName;
        private List<SpecValueVO> values;
    }

    @lombok.Data
    public static class SpecValueVO {
        private Long id;
        private String value;
    }

    // ==================== 接口 ====================

    @Operation(summary = "查询规格模板列表（含值）")
    @GetMapping("/list")
    public Result<List<SpecTemplateVO>> list(@RequestParam(required = false) Long categoryId) {
        LambdaQueryWrapper<SpecTemplate> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(SpecTemplate::getCategoryId, categoryId);
        }
        wrapper.orderByAsc(SpecTemplate::getId);
        List<SpecTemplate> templates = templateMapper.selectList(wrapper);

        if (templates.isEmpty()) return Result.success(Collections.emptyList());

        List<Long> specIds = templates.stream().map(SpecTemplate::getId).collect(Collectors.toList());
        Map<Long, List<SpecValue>> valueMap = valueMapper.selectList(
                new LambdaQueryWrapper<SpecValue>().in(SpecValue::getSpecId, specIds))
                .stream().collect(Collectors.groupingBy(SpecValue::getSpecId));

        List<SpecTemplateVO> vos = templates.stream().map(t -> {
            SpecTemplateVO vo = new SpecTemplateVO();
            vo.setId(t.getId());
            vo.setCategoryId(t.getCategoryId());
            vo.setSpecName(t.getSpecName());
            List<SpecValue> vals = valueMap.getOrDefault(t.getId(), Collections.emptyList());
            vo.setValues(vals.stream().map(v -> {
                SpecValueVO sv = new SpecValueVO();
                sv.setId(v.getId());
                sv.setValue(v.getSpecValue());
                return sv;
            }).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());

        return Result.success(vos);
    }

    @Operation(summary = "新增规格模板及值")
    @PostMapping
    @Transactional
    public Result<SpecTemplateVO> save(@RequestBody SpecTemplateSaveDTO dto) {
        if (dto.getSpecName() == null || dto.getSpecName().isBlank()) {
            return Result.fail("规格名不能为空");
        }
        if (dto.getValues() == null || dto.getValues().isEmpty()) {
            return Result.fail("规格值至少填一个");
        }

        SpecTemplate template = new SpecTemplate();
        template.setCategoryId(dto.getCategoryId());
        template.setSpecName(dto.getSpecName().trim());
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.insert(template);

        List<SpecValue> values = new ArrayList<>();
        for (String v : dto.getValues()) {
            SpecValue sv = new SpecValue();
            sv.setSpecId(template.getId());
            sv.setSpecValue(v.trim());
            sv.setCreateTime(LocalDateTime.now());
            sv.setUpdateTime(LocalDateTime.now());
            values.add(sv);
        }
        valueMapper.insert(values);

        SpecTemplateVO vo = new SpecTemplateVO();
        vo.setId(template.getId());
        vo.setCategoryId(template.getCategoryId());
        vo.setSpecName(template.getSpecName());
        vo.setValues(values.stream().map(v -> {
            SpecValueVO sv = new SpecValueVO();
            sv.setId(v.getId());
            sv.setValue(v.getSpecValue());
            return sv;
        }).collect(Collectors.toList()));

        return Result.success(vo);
    }

    @Operation(summary = "更新规格模板（全量替换规格值）")
    @PutMapping("/{id}")
    @Transactional
    public Result<Void> update(@PathVariable Long id, @RequestBody SpecTemplateSaveDTO dto) {
        SpecTemplate template = templateMapper.selectById(id);
        if (template == null) return Result.fail("规格模板不存在");

        if (dto.getSpecName() != null && !dto.getSpecName().isBlank()) {
            template.setSpecName(dto.getSpecName().trim());
        }
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);

        if (dto.getValues() != null && !dto.getValues().isEmpty()) {
            // 删旧值
            valueMapper.delete(new LambdaQueryWrapper<SpecValue>().eq(SpecValue::getSpecId, id));
            // 写新值
            List<SpecValue> newValues = new ArrayList<>();
            for (String v : dto.getValues()) {
                SpecValue sv = new SpecValue();
                sv.setSpecId(id);
                sv.setSpecValue(v.trim());
                sv.setCreateTime(LocalDateTime.now());
                sv.setUpdateTime(LocalDateTime.now());
                newValues.add(sv);
            }
            valueMapper.insert(newValues);
        }

        return Result.success();
    }

    @Operation(summary = "删除规格模板及关联值")
    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> delete(@PathVariable Long id) {
        valueMapper.delete(new LambdaQueryWrapper<SpecValue>().eq(SpecValue::getSpecId, id));
        templateMapper.deleteById(id);
        return Result.success();
    }
}
