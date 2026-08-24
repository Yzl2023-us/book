package com.sping.user.controller;

import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.LoginRequest;
import com.sping.common.dto.RegisterRequest;
import com.sping.common.dto.UpdateUserRequest;
import com.sping.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @Value("${upload.path}")
    private String uploadPath;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @GetMapping("/info/{userId}")
    public ApiResponse<?> getUserInfo(@PathVariable("userId") Integer userId) {
        return userService.getUserInfo(userId);
    }

    @GetMapping("/admin/list")
    public ApiResponse<?> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return userService.listUsers(page, size);
    }

    @PostMapping("/admin/add")
    public ApiResponse<?> addUser(@RequestBody RegisterRequest request) {
        return userService.addUser(request);
    }

    @PutMapping("/admin/{userId}")
    public ApiResponse<?> updateUser(
            @PathVariable("userId") Integer userId,
            @RequestBody UpdateUserRequest request) {
        return userService.updateUser(userId, request);
    }

    @DeleteMapping("/admin/{userId}")
    public ApiResponse<?> deleteUser(@PathVariable("userId") Integer userId) {
        return userService.deleteUser(userId);
    }

    @PostMapping("/upload-avatar")
    public ApiResponse<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return userService.uploadAvatar(file);
    }

    @PutMapping("/avatar/{userId}")
    public ApiResponse<?> updateAvatar(
            @PathVariable("userId") Integer userId,
            @RequestBody Map<String, String> body) {
        String avatarUrl = body.get("avatar");
        return userService.updateAvatar(userId, avatarUrl);
    }

    @GetMapping("/avatar-img/{filename}")
    public ResponseEntity<Resource> getAvatarImage(@PathVariable("filename") String filename) {
        Path filePath = Paths.get(uploadPath, "avatars", filename);
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        String contentType = "image/jpeg";
        String name = filename.toLowerCase();
        if (name.endsWith(".png")) contentType = "image/png";
        else if (name.endsWith(".gif")) contentType = "image/gif";
        else if (name.endsWith(".webp")) contentType = "image/webp";
        else if (name.endsWith(".bmp")) contentType = "image/bmp";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(resource);
    }
}