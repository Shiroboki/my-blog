package com.youlai.boot.support.file.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.youlai.boot.common.exception.BusinessException;
import com.youlai.boot.support.file.model.FileInfo;
import com.youlai.boot.support.file.service.FileService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * 本地文件存储服务
 */
@Data
@Slf4j
@Component
@ConditionalOnProperty(value = "storage.type", havingValue = "local")
@ConfigurationProperties(prefix = "storage.local")
@RequiredArgsConstructor
public class LocalFileServiceImpl implements FileService {

    private static final String PUBLIC_PREFIX = "/uploads/";

    /** 本地存储根目录，来自 storage.local.path */
    private String path;

    /**
     * 上传文件到本地磁盘，按日期分目录，返回 /uploads/... 访问路径
     */
    @Override
    public FileInfo uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        String fileName = IdUtil.simpleUUID() + (suffix == null || suffix.isEmpty() ? "" : "." + suffix);
        String folder = DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATE_PATTERN);
        String filePrefix = path.endsWith(File.separator) ? path : path + File.separator;
        try (InputStream inputStream = file.getInputStream()) {
            FileUtil.writeFromStream(inputStream, filePrefix + folder + File.separator + fileName);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        }
        String fileUrl = PUBLIC_PREFIX + folder + "/" + fileName;
        FileInfo fileInfo = new FileInfo();
        fileInfo.setName(originalFilename);
        fileInfo.setUrl(fileUrl);
        return fileInfo;
    }

    /**
     * 删除本地文件。路径是目录、越界或不存在时返回 false
     *
     * @param filePath 相对于存储根目录或以 /uploads/ 开头的访问路径
     * @return 删除成功返回 true
     */
    @Override
    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        String normalized = filePath.replace('\\', '/');
        if (normalized.startsWith(PUBLIC_PREFIX)) {
            normalized = normalized.substring(PUBLIC_PREFIX.length());
        } else if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        Path root = Path.of(path).toAbsolutePath().normalize();
        Path target = root.resolve(normalized).normalize();
        if (!target.startsWith(root) || FileUtil.isDirectory(target.toFile())) {
            return false;
        }
        return FileUtil.del(target.toFile());
    }
}
