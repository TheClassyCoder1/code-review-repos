package com.example.lending.ops.part2.admin;

import com.example.lending.ops.common.Result.CommonResult;
import com.example.lending.ops.part2.admin.AdminConsoleService.PmsProductCategoryParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/ops/admin/productCategory")
@RequiredArgsConstructor
public class ProductCategoryController {
    private final AdminConsoleService productCategoryService;

    @Operation(summary = "修改商品分类")
    @RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult update(@PathVariable Long id,
                         @Validated
                         @RequestBody PmsProductCategoryParam productCategoryParam) {
        int count = productCategoryService.update(id, productCategoryParam);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }
}
