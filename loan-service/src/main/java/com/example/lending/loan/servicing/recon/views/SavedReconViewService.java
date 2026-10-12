package com.example.lending.loan.servicing.recon.views;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Saved workbench views. */
@Service
public class SavedReconViewService {

    private static final int MAX_VIEWS_PER_OPERATOR = 50;

    private final SavedReconViewRepository repository;

    public SavedReconViewService(SavedReconViewRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SavedReconView> listSavedViews(String operatorId) {
        return repository.findByOperatorIdOrderByCreatedAtDesc(operatorId);
    }

    @Transactional
    public SavedReconView saveView(String operatorId, String name, String filterJson) {
        if (repository.findByOperatorIdOrderByCreatedAtDesc(operatorId).size() >= MAX_VIEWS_PER_OPERATOR) {
            throw new IllegalStateException("Saved view limit reached");
        }
        SavedReconView view = new SavedReconView();
        view.setId(UUID.randomUUID().toString());
        view.setOperatorId(operatorId);
        view.setName(name);
        view.setFilterJson(filterJson);
        return repository.save(view);
    }

    @Transactional
    public boolean deleteSavedView(String operatorId, String viewId) {
        return repository.deleteByIdAndOperatorId(viewId, operatorId) > 0;
    }
}
