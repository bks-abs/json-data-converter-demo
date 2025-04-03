package org.bks.po;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 转换的配置文件
 */
@Data
@NoArgsConstructor
public class ConvConfig {

    // 源数据字段ID,只需要保存要映射的叶子节点字段的ID
    String srcId;

    // 目标数据字段ID,只需要保存要映射的叶子节点字段的ID
    String dstId;

    public ConvConfig(String srcId, String dstId) {
        this.srcId = srcId;
        this.dstId = dstId;
    }

}
