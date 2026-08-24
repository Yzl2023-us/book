package com.sping.user.service;

import com.sping.common.dto.ApiResponse;
import com.sping.common.dto.LoginRequest;
import com.sping.common.dto.RegisterRequest;
import com.sping.common.dto.UpdateUserRequest;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    ApiResponse<?> register(RegisterRequest request);

    ApiResponse<?> login(LoginRequest request);

    ApiResponse<?> getUserInfo(Integer userId);

    ApiResponse<?> listUsers(int page, int size);

    ApiResponse<?> addUser(RegisterRequest request);

    ApiResponse<?> updateUser(Integer userId, UpdateUserRequest request);

    ApiResponse<?> deleteUser(Integer userId);

    ApiResponse<?> uploadAvatar(MultipartFile file);

    ApiResponse<?> updateAvatar(Integer userId, String avatarUrl);
}