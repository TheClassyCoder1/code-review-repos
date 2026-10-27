package com.example.lending.loan.ops.scheduler;

import com.example.lending.loan.support.crypto.Sha256Tool;
import com.example.lending.loan.support.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ops/scheduler")
public class SchedulerAccountController {

    private final SchedulerUserRepository schedulerUserRepository;

    public SchedulerAccountController(SchedulerUserRepository schedulerUserRepository) {
        this.schedulerUserRepository = schedulerUserRepository;
    }

    @PostMapping("/updatePwd")
    public ApiResponse<String> updatePwd(HttpServletRequest request, String oldPassword, String password) {

        // valid
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return ApiResponse.ofFail("Please enter the current password");
        }
        if (password == null || password.trim().isEmpty()) {
            return ApiResponse.ofFail("Please enter the new password");
        }
        password = password.trim();
        if (!(password.length() >= 4 && password.length() <= 20)) {
            return ApiResponse.ofFail("Password length must be within [4-20]");
        }

        String oldPasswordHash = Sha256Tool.sha256(oldPassword);
        String passwordHash = Sha256Tool.sha256(password);

        // valid old pwd
        SchedulerUser existUser = schedulerUserRepository.findByUserName(request.getUserPrincipal().getName());
        if (existUser == null || !oldPasswordHash.equals(existUser.getPassword())) {
            return ApiResponse.ofFail("Current password is invalid");
        }

        // write new
        existUser.setPassword(passwordHash);
        schedulerUserRepository.save(existUser);

        return ApiResponse.ofSuccess();
    }
}
