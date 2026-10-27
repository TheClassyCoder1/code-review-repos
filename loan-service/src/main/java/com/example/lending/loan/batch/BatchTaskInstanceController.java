package com.example.lending.loan.batch;

import com.example.lending.loan.support.web.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/ops/batch/projects/{projectCode}/task-instances")
public class BatchTaskInstanceController {

    private final TaskInstanceService taskInstanceService;

    public BatchTaskInstanceController(TaskInstanceService taskInstanceService) {
        this.taskInstanceService = taskInstanceService;
    }

    @PostMapping(value = "/{id}/force-success")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> forceTaskSuccess(Principal loginUser,
                                              @PathVariable long projectCode,
                                              @PathVariable(value = "id") Integer id) {
        taskInstanceService.forceTaskSuccess(loginUser.getName(), projectCode, id);
        return ApiResponse.ofSuccess();
    }
}
