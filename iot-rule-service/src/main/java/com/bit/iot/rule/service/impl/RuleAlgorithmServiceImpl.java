package com.bit.iot.rule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.iot.rule.dao.RuleAlgorithmMapper;
import com.bit.iot.rule.engine.AlgorithmLoader;
import com.bit.iot.rule.model.entity.RuleAlgorithm;
import com.bit.iot.rule.service.IRuleAlgorithmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;

/**
 * 规则算法 ServiceImpl
 *
 * @author chenhao
 * @since 2026-03-27
 */
@Service
public class RuleAlgorithmServiceImpl extends ServiceImpl<RuleAlgorithmMapper, RuleAlgorithm>
        implements IRuleAlgorithmService {

    @Value("${rule.algorithm.upload-path:./algorithms}")
    private String uploadPath;

    @Autowired
    private AlgorithmLoader algorithmLoader;

    @Override
    public Page<RuleAlgorithm> getAlgorithmList(Page<RuleAlgorithm> page, String algorithmName, String algorithmType) {
        QueryWrapper<RuleAlgorithm> qw = new QueryWrapper<>();
        if (algorithmName != null && !algorithmName.isEmpty()) {
            qw.like("algorithm_name", algorithmName);
        }
        if (algorithmType != null && !algorithmType.isEmpty()) {
            qw.eq("algorithm_type", algorithmType);
        }
        qw.orderByDesc("create_time");
        return this.page(page, qw);
    }

    @Override
    public RuleAlgorithm uploadAlgorithm(MultipartFile file, String algorithmName, String algorithmDesc,
                                          String algorithmType, String algorithmClass, String algorithmVersion) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }

        // 推断算法类型
        if (algorithmType == null || algorithmType.isEmpty()) {
            algorithmType = originalFilename.endsWith(".py") ? "python" : "jar";
        }

        // 推断算法名称
        if (algorithmName == null || algorithmName.trim().isEmpty()) {
            int dot = originalFilename.lastIndexOf('.');
            algorithmName = dot > 0 ? originalFilename.substring(0, dot) : originalFilename;
        }

        // 重名校验
        long count = this.count(new QueryWrapper<RuleAlgorithm>().eq("algorithm_name", algorithmName));
        if (count > 0) {
            throw new RuntimeException("算法名称已存在：" + algorithmName);
        }

        // 存储文件
        int dot = originalFilename.lastIndexOf('.');
        String ext = dot > 0 ? originalFilename.substring(dot) : "";
        String uniqueFilename = UUID.randomUUID().toString() + ext;

        try {
            Path dir = Paths.get(uploadPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path dest = dir.resolve(uniqueFilename);
            file.transferTo(dest.toFile());

            RuleAlgorithm algorithm = new RuleAlgorithm();
            algorithm.setAlgorithmName(algorithmName);
            algorithm.setAlgorithmDesc(algorithmDesc);
            algorithm.setAlgorithmType(algorithmType);
            algorithm.setAlgorithmPath(dest.toAbsolutePath().toString());
            algorithm.setAlgorithmClass(algorithmClass);
            algorithm.setAlgorithmVersion(algorithmVersion != null ? algorithmVersion : "1.0.0");
            algorithm.setAlgorithmStatus(1);
            algorithm.setFileSize(file.getSize());
            Date now = new Date();
            algorithm.setCreateTime(now);
            algorithm.setUpdateTime(now);

            this.save(algorithm);
            return algorithm;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败：" + e.getMessage(), e);
        }
    }

    @Override
    public boolean addAlgorithm(RuleAlgorithm algorithm) {
        long count = this.count(new QueryWrapper<RuleAlgorithm>().eq("algorithm_name", algorithm.getAlgorithmName()));
        if (count > 0) {
            throw new RuntimeException("算法名称已存在：" + algorithm.getAlgorithmName());
        }
        Date now = new Date();
        algorithm.setCreateTime(now);
        algorithm.setUpdateTime(now);
        return this.save(algorithm);
    }

    @Override
    public boolean editAlgorithm(RuleAlgorithm algorithm) {
        algorithm.setUpdateTime(new Date());
        // 文件更新时卸载旧算法缓存，触发下次执行时重新加载
        algorithmLoader.unload(algorithm.getId());
        return this.updateById(algorithm);
    }

    @Override
    public boolean deleteAlgorithm(String id) {
        algorithmLoader.unload(id);
        RuleAlgorithm algorithm = this.getById(id);
        if (algorithm != null && algorithm.getAlgorithmPath() != null) {
            new File(algorithm.getAlgorithmPath()).delete();
        }
        return this.removeById(id);
    }

    @Override
    public boolean enableAlgorithm(String id) {
        RuleAlgorithm algorithm = this.getById(id);
        if (algorithm == null) throw new RuntimeException("算法不存在");
        algorithm.setAlgorithmStatus(1);
        algorithm.setUpdateTime(new Date());
        return this.updateById(algorithm);
    }

    @Override
    public boolean disableAlgorithm(String id) {
        RuleAlgorithm algorithm = this.getById(id);
        if (algorithm == null) throw new RuntimeException("算法不存在");
        algorithmLoader.unload(id);
        algorithm.setAlgorithmStatus(0);
        algorithm.setUpdateTime(new Date());
        return this.updateById(algorithm);
    }
}
