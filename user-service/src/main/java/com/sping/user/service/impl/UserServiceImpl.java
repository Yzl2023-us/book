package com.sping.user.service.impl;

import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.LoginRequest;
import com.sping.common.dto.RegisterRequest;
import com.sping.common.dto.UpdateUserRequest;
import com.sping.common.entity.User;
import com.sping.user.config.JwtUtil;
import com.sping.user.repository.UserRepository;
import com.sping.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${upload.path}")
    private String uploadPath;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public ApiResponse<?> register(RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ApiResponse.error(400, "用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ApiResponse.error(400, "密码不能为空");
        }
        if (userRepository.existsByUserName(request.getUsername())) {
            return ApiResponse.error(400, "用户名已存在");
        }

        User user = new User(request.getUsername(), passwordEncoder.encode(request.getPassword()));
        user.setUserId(userRepository.findMinAvailableUserId());  // 手动分配最小可用 ID
        userRepository.save(user);
        return ApiResponse.success("注册成功", null);
    }

    @Override
    public ApiResponse<?> login(LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ApiResponse.error(400, "用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ApiResponse.error(400, "密码不能为空");
        }

        Optional<User> userOpt = userRepository.findByUserName(request.getUsername());
        if (userOpt.isEmpty()) {
            return ApiResponse.error(400, "用户名或密码错误");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getUserPassword())) {
            return ApiResponse.error(400, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getUserName());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getUserId());
        data.put("userName", user.getUserName());
        data.put("isAdmin", user.getIsAdmin());
        data.put("avatar", user.getAvatar());

        return ApiResponse.success("登录成功", data);
    }

    @Override
    public ApiResponse<?> getUserInfo(Integer userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ApiResponse.error(404, "用户不存在");
        }
        User user = userOpt.get();
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("userName", user.getUserName());
        data.put("isAdmin", user.getIsAdmin());
        data.put("avatar", user.getAvatar());
        return ApiResponse.success(data);
    }

    @Override
    public ApiResponse<?> listUsers(int page, int size) {
        Page<User> userPage = userRepository.findAll(PageRequest.of(page, size));
        return ApiResponse.success(userPage);
    }

    @Override
    public ApiResponse<?> addUser(RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ApiResponse.error(400, "用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ApiResponse.error(400, "密码不能为空");
        }
        if (userRepository.existsByUserName(request.getUsername())) {
            return ApiResponse.error(400, "用户名已存在");
        }

        User user = new User(request.getUsername(), passwordEncoder.encode(request.getPassword()));
        user.setUserId(userRepository.findMinAvailableUserId());  // 手动分配最小可用 ID
        userRepository.save(user);
        return ApiResponse.success("添加成功", null);
    }

    @Override
    public ApiResponse<?> updateUser(Integer userId, UpdateUserRequest request) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ApiResponse.error(404, "用户不存在");
        }

        User user = userOpt.get();

        if (request.getUserName() != null && !request.getUserName().isBlank()) {
            // 检查用户名是否被其他用户占用
            if (!user.getUserName().equals(request.getUserName())
                    && userRepository.existsByUserName(request.getUserName())) {
                return ApiResponse.error(400, "用户名已存在");
            }
            user.setUserName(request.getUserName());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setUserPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getIsAdmin() != null) {
            user.setIsAdmin(request.getIsAdmin());
        }

        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        userRepository.save(user);
        return ApiResponse.success("更新成功", null);
    }

    @Override
    public ApiResponse<?> deleteUser(Integer userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ApiResponse.error(404, "用户不存在");
        }
        userRepository.deleteById(userId);
        return ApiResponse.success("删除成功", null);
    }

    @Override
    public ApiResponse<?> uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "请选择图片文件");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return ApiResponse.error(400, "文件名不能为空");
        }

        // 校验文件类型
        String ext = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            ext = originalFilename.substring(dotIndex).toLowerCase();
        }
        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp|bmp)$")) {
            return ApiResponse.error(400, "仅支持 jpg、jpeg、png、gif、webp、bmp 格式的图片");
        }

        try {
            // 生成唯一文件名
            String newFilename = UUID.randomUUID().toString() + ext;

            // 使用配置的绝对路径创建上传目录
            Path uploadDir = Paths.get(uploadPath, "avatars");
            Files.createDirectories(uploadDir);

            // 保存文件
            Path filePath = uploadDir.resolve(newFilename);
            file.transferTo(filePath.toFile());

            // 返回访问路径
            String url = "/api/user/avatar-img/" + newFilename;
            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            return ApiResponse.success("上传成功", data);
        } catch (IOException e) {
            return ApiResponse.error(500, "头像上传失败: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse<?> updateAvatar(Integer userId, String avatarUrl) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ApiResponse.error(404, "用户不存在");
        }
        User user = userOpt.get();
        user.setAvatar(avatarUrl);
        userRepository.save(user);
        return ApiResponse.success("头像更新成功", null);
    }
}