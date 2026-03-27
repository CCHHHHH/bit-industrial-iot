package com.bit.iot.rule.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.iot.rule.model.entity.RuleAlgorithm;
import org.springframework.web.multipart.MultipartFile;

/**
 * 规则算法 Service
 *
 * @author chenhao
 * @since 2026-03-27
 */
public interface IRuleAlgorithmService extends IService<RuleAlgorithm> {

    /** 分页查询算法列表 */
    Page<RuleAlgorithm> getAlgorithmList(Page<RuleAlgorithm> page, String algorithmName, String algorithmType);

    /** 上传算法文件（JAR 或 Python），重名校验后入库 */
    RuleAlgorithm uploadAlgorithm(MultipartFile file, String algorithmName, String algorithmDesc,
                                   String algorithmType, String algorithmClass, String algorithmVersion);

    /** 新增算法（仅元数据，文件已在外部管理） */
    boolean addAlgorithm(RuleAlgorithm algorithm);

    /** 编辑算法元数据 */
    boolean editAlgorithm(RuleAlgorithm algorithm);

    /** 删除算法 */
    boolean deleteAlgorithm(String id);

    /** 启用算法 */
    boolean enableAlgorithm(String id);

    /** 禁用算法 */
    boolean disableAlgorithm(String id);
}
