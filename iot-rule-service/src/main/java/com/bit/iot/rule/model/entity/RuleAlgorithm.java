package com.bit.iot.rule.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 规则算法表
 * 管理用户上传的 JAR 包或 Python 脚本算法文件
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("rule_algorithm")
@Schema(description = "规则算法")
public class RuleAlgorithm implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    /** 算法名称（唯一） */
    @Schema(description = "算法名称")
    @TableField("algorithm_name")
    private String algorithmName;

    /** 算法描述 */
    @Schema(description = "算法描述")
    @TableField("algorithm_desc")
    private String algorithmDesc;

    /** 算法类型：jar / python */
    @Schema(description = "算法类型（jar/python）")
    @TableField("algorithm_type")
    private String algorithmType;

    /** 算法文件存储路径 */
    @Schema(description = "算法文件路径")
    @TableField("algorithm_path")
    private String algorithmPath;

    /**
     * 算法入口类全限定名（JAR 类型必填）
     * 该类须实现 IRuleAlgorithm 接口
     */
    @Schema(description = "算法入口类（JAR 类型填写全限定类名）")
    @TableField("algorithm_class")
    private String algorithmClass;

    /** 算法版本 */
    @Schema(description = "算法版本")
    @TableField("algorithm_version")
    private String algorithmVersion;

    /** 状态：0-禁用，1-启用 */
    @Schema(description = "状态（0-禁用，1-启用）")
    @TableField("algorithm_status")
    private Integer algorithmStatus;

    /** 文件大小（字节） */
    @Schema(description = "文件大小（字节）")
    @TableField("file_size")
    private Long fileSize;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
