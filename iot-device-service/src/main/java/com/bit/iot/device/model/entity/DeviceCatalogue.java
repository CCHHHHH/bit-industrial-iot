package com.bit.iot.device.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
/**
 * <p>
 * 设备目录
 * </p>
 *
 * @author chenhao
 * @since 2026-03-12 04:06:08
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@TableName("device_catalogue")
@Schema(description = "设备目录实体")
public class DeviceCatalogue implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    /**
     * 父级目录 id
     */
    @TableField("parent_id")
    @Schema(description = "父级目录 id")
    private String parentId;
    
    /**
     * 目录名称
     */
    @Schema(description = "目录名称")
    @TableField("catalogue_name")
    private String catalogueName;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    /**
     * 子节点列表（非数据库字段，用于树形结构返回）
     */
    @Schema(description = "子节点列表")
    @TableField(exist = false)
    private List<DeviceCatalogue> children;

    /**
     * 该目录下挂载的设备数量（非数据库字段，用于统计）
     */
    @Schema(description = "该目录下挂载的设备数量")
    @TableField(exist = false)
    private Long deviceCount;
}
