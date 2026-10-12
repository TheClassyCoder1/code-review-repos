package com.example.lending.loan.servicing.statements.render;

import com.example.lending.loan.servicing.statements.render.RenderParameters.ResourceInfo;

import com.example.lending.loan.servicing.common.ServicingResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/** Manual statement render runs for the operations team. */
@RestController
@RequestMapping("/servicing/admin/render-jobs")
public class RenderJobController {

    private final StatementRenderRunner renderRunner;

    public RenderJobController(StatementRenderRunner renderRunner) {
        this.renderRunner = renderRunner;
    }

    @PostMapping("/runs")
    public ServicingResult<Integer> run(@Valid @RequestBody RenderJobRequest request)
            throws IOException, InterruptedException {
        RenderParameters parameters = new RenderParameters(new ResourceInfo(request.mainJar()),
                request.jvmArgs(), request.mainArgs(), List.of());
        return ServicingResult.ok(renderRunner.render(parameters, request.statementId(), request.period()));
    }

    public record RenderJobRequest(@NotBlank @Pattern(regexp = "^[A-Za-z0-9_.-]+\\.jar$") String mainJar,
                                   @Size(max = 512) String jvmArgs,
                                   @Size(max = 512) String mainArgs,
                                   @NotNull Long statementId,
                                   @Pattern(regexp = "^\\d{4}-\\d{2}$") String period) {
    }
}
