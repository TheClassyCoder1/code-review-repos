package com.example.lending.loan.servicing.admin.reasons;

import com.example.lending.loan.servicing.common.ServicingResult;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/** Administration of delinquency reason categories. */
@Controller
@RequestMapping("/servicing/admin/reason-categories")
public class CollectionReasonCategoryController {

    private final CollectionReasonCategoryService reasonCategoryService;

    public CollectionReasonCategoryController(CollectionReasonCategoryService reasonCategoryService) {
        this.reasonCategoryService = reasonCategoryService;
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public ServicingResult<List<CollectionReasonCategory>> list(@RequestParam(required = false) Long parentId) {
        return ServicingResult.ok(reasonCategoryService.children(parentId));
    }

    @RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
    @ResponseBody
    public ServicingResult<Integer> update(@PathVariable Long id,
                                           @Validated
                                           @RequestBody ReasonCategoryParam reasonCategoryParam) {
        int count = reasonCategoryService.update(id, reasonCategoryParam);
        if (count > 0) {
            return ServicingResult.ok(count);
        } else {
            return ServicingResult.failed();
        }
    }
}
