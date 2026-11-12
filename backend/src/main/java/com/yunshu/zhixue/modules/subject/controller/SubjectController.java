package com.yunshu.zhixue.modules.subject.controller;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.subject.dto.SortRequest;
import com.yunshu.zhixue.modules.subject.dto.StageRequest;
import com.yunshu.zhixue.modules.subject.dto.StageStatusRequest;
import com.yunshu.zhixue.modules.subject.dto.SubjectCard;
import com.yunshu.zhixue.modules.subject.dto.SubjectRequest;
import com.yunshu.zhixue.modules.subject.dto.SubjectStats;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import com.yunshu.zhixue.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学科管理（04 §5.3 A-S01 ~ A-S11）。
 *
 * <p>阶段随学科权限点（D4-5）：新增走 subject:add、编辑走 subject:edit、删除走 subject:delete。</p>
 */
@Tag(name = "学科管理", description = "A-S01 ~ A-S11（含阶段）")
@RestController
@RequestMapping("/api/admin/v1/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @Operation(summary = "A-S01 学科统计卡")
    @GetMapping("/stats")
    @RequirePermission("subject:view")
    public ApiResponse<SubjectStats> stats() {
        return ApiResponse.ok(subjectService.stats());
    }

    @Operation(summary = "A-S02 学科卡片列表")
    @GetMapping
    @RequirePermission("subject:view")
    public ApiResponse<List<SubjectCard>> list() {
        return ApiResponse.ok(subjectService.list());
    }

    @Operation(summary = "A-S03 新增学科")
    @PostMapping
    @RequirePermission("subject:add")
    public ApiResponse<Long> create(@Valid @RequestBody SubjectRequest request) {
        return ApiResponse.ok(subjectService.create(request));
    }

    @Operation(summary = "A-S04 编辑学科")
    @PutMapping("/{id}")
    @RequirePermission("subject:edit")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        subjectService.update(id, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-S05 学科上移 / 下移")
    @PutMapping("/sort")
    @RequirePermission("subject:edit")
    public ApiResponse<Void> sort(@Valid @RequestBody SortRequest request) {
        subjectService.move(request);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-S06 删除学科")
    @DeleteMapping("/{id}")
    @RequirePermission("subject:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        subjectService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-S07 新增阶段")
    @PostMapping("/{subjectId}/stages")
    @RequirePermission("subject:add")
    public ApiResponse<Long> createStage(@PathVariable Long subjectId,
                                         @Valid @RequestBody StageRequest request) {
        return ApiResponse.ok(subjectService.createStage(subjectId, request));
    }

    @Operation(summary = "A-S08 编辑阶段")
    @PutMapping("/{subjectId}/stages/{id}")
    @RequirePermission("subject:edit")
    public ApiResponse<Void> updateStage(@PathVariable Long subjectId,
                                         @PathVariable Long id,
                                         @Valid @RequestBody StageRequest request) {
        subjectService.updateStage(subjectId, id, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-S09 阶段上移 / 下移")
    @PutMapping("/{subjectId}/stages/sort")
    @RequirePermission("subject:edit")
    public ApiResponse<Void> sortStage(@PathVariable Long subjectId,
                                       @Valid @RequestBody SortRequest request) {
        subjectService.moveStage(subjectId, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "A-S10 启用 / 停用阶段")
    @PutMapping("/{subjectId}/stages/{id}/status")
    @RequirePermission("subject:edit")
    public ApiResponse<Void> updateStageStatus(@PathVariable Long subjectId,
                                               @PathVariable Long id,
                                               @Valid @RequestBody StageStatusRequest request) {
        subjectService.updateStageStatus(subjectId, id, request.status());
        return ApiResponse.ok();
    }

    @Operation(summary = "A-S11 删除阶段")
    @DeleteMapping("/{subjectId}/stages/{id}")
    @RequirePermission("subject:delete")
    public ApiResponse<Void> deleteStage(@PathVariable Long subjectId, @PathVariable Long id) {
        subjectService.deleteStage(subjectId, id);
        return ApiResponse.ok();
    }
}
