package com.yunshu.zhixue.modules.system.dto;

/**
 * A-G03 导出结果（同步生成下载流，Q10）。
 */
public record LogExport(byte[] content, String filename) {
}
