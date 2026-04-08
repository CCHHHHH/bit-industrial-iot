package com.bit.iot.rule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import bit.iot.common.controller.BusinessException;
import com.bit.iot.rule.dao.RuleAlgorithmMapper;
import com.bit.iot.rule.engine.AlgorithmLoader;
import com.bit.iot.rule.model.entity.RuleAlgorithm;
import com.bit.iot.rule.service.IRuleAlgorithmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Set;
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

    @Value("${rule.algorithm.shared-path:./algorithms}")
    private String sharedPath;

    @Value("${rule.algorithm.max-file-size-bytes:52428800}")
    private long maxFileSizeBytes;

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".jar", ".py");

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
            throw new BusinessException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException("文件名不能为空");
        }

        String ext = resolveExtension(originalFilename);
        validateUpload(file, ext);
        algorithmType = resolveAlgorithmType(algorithmType, ext);

        if (algorithmName == null || algorithmName.trim().isEmpty()) {
            int dot = originalFilename.lastIndexOf('.');
            algorithmName = dot > 0 ? originalFilename.substring(0, dot) : originalFilename;
        }
        if ("jar".equalsIgnoreCase(algorithmType)
                && (algorithmClass == null || algorithmClass.trim().isEmpty())) {
            throw new BusinessException("JAR 算法必须提供 algorithmClass");
        }

        long count = this.count(new QueryWrapper<RuleAlgorithm>().eq("algorithm_name", algorithmName));
        if (count > 0) {
            throw new BusinessException("算法名称已存在：" + algorithmName);
        }

        String uniqueFilename = UUID.randomUUID().toString() + ext;

        try {
            Path dest = resolveTargetPath(uploadPath, uniqueFilename);
            file.transferTo(dest.toAbsolutePath().toFile());
            syncToSharedPath(dest);

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

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("文件上传失败：" + e.getMessage(), e);
        }
    }

    @Override
    public boolean addAlgorithm(RuleAlgorithm algorithm) {
        long count = this.count(new QueryWrapper<RuleAlgorithm>().eq("algorithm_name", algorithm.getAlgorithmName()));
        if (count > 0) {
            throw new BusinessException("算法名称已存在：" + algorithm.getAlgorithmName());
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
            deleteIfExists(Paths.get(algorithm.getAlgorithmPath()));
            Path sharedFile = Paths.get(sharedPath, Paths.get(algorithm.getAlgorithmPath()).getFileName().toString());
            deleteIfExists(sharedFile);
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

    private void validateUpload(MultipartFile file, String ext) {
        if (!SUPPORTED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("仅支持上传 .jar 或 .py 算法文件");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BusinessException("算法文件过大，当前上限为 " + maxFileSizeBytes + " 字节");
        }
    }

    private String resolveAlgorithmType(String algorithmType, String ext) {
        if (algorithmType == null || algorithmType.isBlank()) {
            return ".py".equalsIgnoreCase(ext) ? "python" : "jar";
        }
        String normalized = algorithmType.trim().toLowerCase();
        if (!"jar".equals(normalized) && !"python".equals(normalized)) {
            throw new BusinessException("不支持的算法类型: " + algorithmType);
        }
        if ("jar".equals(normalized) && !".jar".equalsIgnoreCase(ext)) {
            throw new BusinessException("JAR 算法文件必须是 .jar");
        }
        if ("python".equals(normalized) && !".py".equalsIgnoreCase(ext)) {
            throw new BusinessException("Python 算法文件必须是 .py");
        }
        return normalized;
    }

    private String resolveExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        String ext = dot > 0 ? filename.substring(dot).toLowerCase() : "";
        if (!SUPPORTED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("不支持的算法文件类型: " + filename);
        }
        return ext;
    }

    private Path resolveTargetPath(String baseDir, String filename) throws IOException {
        Path dir = Paths.get(baseDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) {
            throw new BusinessException("非法的算法文件路径");
        }
        return target;
    }

    private void syncToSharedPath(Path sourcePath) throws IOException {
        Path sharedFile = resolveTargetPath(sharedPath, sourcePath.getFileName().toString());
        Files.copy(sourcePath, sharedFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
