package com.example.lending.loan.servicing.admin.reasons;

import com.example.lending.loan.servicing.common.ServicingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/** Maintenance of delinquency reason categories. */
@Service
public class CollectionReasonCategoryService {

    private final CollectionReasonCategoryRepository repository;

    public CollectionReasonCategoryService(CollectionReasonCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CollectionReasonCategory> children(Long parentId) {
        return repository.findByParentIdOrderBySort(parentId);
    }

    @Transactional
    public int update(Long id, ReasonCategoryParam param) {
        CollectionReasonCategory category = repository.findById(id).orElse(null);
        if (category == null) {
            return 0;
        }
        if (param.getParentId() != null) {
            if (Objects.equals(param.getParentId(), id)) {
                throw ServicingException.badRequest("A category cannot be its own parent");
            }
            if (!repository.existsById(param.getParentId())) {
                throw ServicingException.badRequest("Parent category does not exist");
            }
        }
        category.setParentId(param.getParentId());
        category.setName(param.getName());
        category.setSort(param.getSort());
        return 1;
    }
}
