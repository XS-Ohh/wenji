package com.wenji.upload;

import com.wenji.common.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/uploads/images")
public class ImageUploadController {

    private final ImageUploadService service;

    public ImageUploadController(ImageUploadService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<ImageUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(service.upload(file));
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> image(@PathVariable String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(service.mediaType(filename)))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .body(service.load(filename));
    }
}
