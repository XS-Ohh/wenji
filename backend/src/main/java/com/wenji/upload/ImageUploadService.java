package com.wenji.upload;

import com.wenji.common.BusinessException;
import com.wenji.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Service
public class ImageUploadService {

    private static final Logger log = LoggerFactory.getLogger(ImageUploadService.class);
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private final Path uploadRoot;

    public ImageUploadService(@Value("${wenji.upload-dir:../uploads}") String uploadDirectory) {
        this.uploadRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public ImageUploadResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片大小不能超过 5MB");
        }

        String extension = detectExtension(file);
        String filename = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(uploadRoot);
            Path target = uploadRoot.resolve(filename).normalize();
            if (!target.startsWith(uploadRoot)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "图片文件名无效");
            }
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target);
            }
            return new ImageUploadResponse("/api/uploads/images/" + filename);
        } catch (IOException exception) {
            log.error("Failed to store uploaded image", exception);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片保存失败");
        }
    }

    public Resource load(String filename) {
        if (filename == null || !filename.matches("[a-f0-9-]+\\.(jpg|png|webp)")) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        Path target = uploadRoot.resolve(filename).normalize();
        if (!target.startsWith(uploadRoot) || !Files.isRegularFile(target)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        return new FileSystemResource(target);
    }

    public String mediaType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private String detectExtension(MultipartFile file) {
        byte[] header = new byte[12];
        int length;
        try (InputStream input = file.getInputStream()) {
            length = input.read(header);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无法读取图片内容");
        }
        if (length >= 3 && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8
                && (header[2] & 0xff) == 0xff) {
            return "jpg";
        }
        if (length >= 8 && (header[0] & 0xff) == 0x89 && header[1] == 0x50 && header[2] == 0x4e
                && header[3] == 0x47 && header[4] == 0x0d && header[5] == 0x0a
                && header[6] == 0x1a && header[7] == 0x0a) {
            return "png";
        }
        if (length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return "webp";
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 JPG、PNG 或 WebP 图片");
    }
}
