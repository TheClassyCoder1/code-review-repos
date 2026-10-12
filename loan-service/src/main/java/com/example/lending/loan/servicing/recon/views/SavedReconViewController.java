package com.example.lending.loan.servicing.recon.views;

import com.example.lending.loan.servicing.common.CurrentOperator;
import com.example.lending.loan.servicing.common.ServicingResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Saved filter sets of the reconciliation workbench. */
@RestController
@RequestMapping("/servicing/recon/views")
public class SavedReconViewController {

    private final SavedReconViewService savedViewService;
    private final CurrentOperator currentOperator;

    public SavedReconViewController(SavedReconViewService savedViewService, CurrentOperator currentOperator) {
        this.savedViewService = savedViewService;
        this.currentOperator = currentOperator;
    }

    @GetMapping
    public ServicingResult<List<SavedReconView>> list() {
        return ServicingResult.ok(savedViewService.listSavedViews(String.valueOf(currentOperator.id())));
    }

    @PostMapping
    public ServicingResult<String> save(@Valid @RequestBody SaveViewRequest request) {
        SavedReconView view = savedViewService.saveView(String.valueOf(currentOperator.id()),
                request.name(), request.filterJson());
        return ServicingResult.ok(view.getId());
    }

    @DeleteMapping("/deleteSavedView")
    public ServicingResult<String> deleteSavedView(@RequestParam String operatorId, @RequestParam String viewId) {
        boolean deleted = savedViewService.deleteSavedView(operatorId, viewId);
        return deleted ? ServicingResult.ok("Deleted") : ServicingResult.failed("View does not exist");
    }

    public record SaveViewRequest(@NotBlank @Size(max = 80) String name,
                                  @NotBlank @Size(max = 4000) String filterJson) {
    }
}
