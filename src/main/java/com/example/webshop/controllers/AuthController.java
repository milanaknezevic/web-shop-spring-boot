package com.example.webshop.controllers;

import com.example.webshop.models.dto.LoginResponse;
import com.example.webshop.models.dto.User;
import com.example.webshop.models.requests.AccountActivationRequest;
import com.example.webshop.models.requests.LoginRequest;
import com.example.webshop.models.requests.SignUpRequest;
import com.example.webshop.services.AuthService;
import com.example.webshop.services.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@RestController
public class AuthController {
    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }
    @PostMapping("insertImage")
    public String insertImage(@RequestParam(value = "file", required = false) MultipartFile file) {
        return userService.insertImage(file);
    }
    @PostMapping("insertImages")
    public  List<String> insertImages(@RequestParam(value = "files", required = false) List<MultipartFile> files) {
        return userService.insertImages(files);
    }
    @PostMapping("login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("sign-up")
    public void signUp(@RequestBody @Valid SignUpRequest request) {
        userService.signUp(request);
    }

    @PostMapping("activeAccount")
    public LoginResponse activeAccount(@RequestBody @Valid AccountActivationRequest accountActivationRequest) {
        if (authService.activateAccount(accountActivationRequest)) {
            return userService.activateAccount(accountActivationRequest.getKorisnickoIme());
        }
        return null;
    }

}
