package com.example.lending.ops.forms;

import com.example.lending.ops.common.JSONObject;
import com.example.lending.ops.common.Result;
import com.example.lending.ops.common.StringUtils.oConvertUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/** Java-side hook called by the low-code form engine before it persists a borrower contact form. */
@Slf4j
@RestController
@RequestMapping("/online/cgform/enhance")
public class OnlineFormEnhanceController {
    @PostMapping("/enhanceJavaHttp")
    public Result<?> enhanceJavaHttp(@RequestBody JSONObject params) {
        log.info(" --- params：" + params.toJSONString());
        String tableName = params.getString("tableName");
        JSONObject record = params.getJSONObject("record");
        /*
         * 业务场景一： 获取提交表单数据，进行其他业务关联操作
         * （比如：根据入库单，同步更改库存）
         */
        log.info(" --- tableName：" + tableName);
        log.info(" --- 行数据：" + record.toJSONString());
        /*
         * 业务场景二： 保存数据之前进行数据的校验
         * 直接返回错误状态即可
         */
        String phone = record.getString("phone");
        if (oConvertUtils.isEmpty(phone)) {
            return Result.error("手机号不能为空！");
        }
        /*
         * 业务场景三： 保存数据之对数据的处理
         * 直接操作 record 即可
         */
        record.put("phone", "010-" + phone);

        /* 其他业务场景自行实现 */

        // 返回场景一： 不对 record 做任何修改的情况下，可以直接返回 code，
        // 返回 0 = 丢弃当前数据
        // 返回 1 = 新增当前数据
        // 返回 2 = 修改当前数据 TODO（？）存疑
//		 return Result.OK(1);

        // 返回场景二： 需要对 record 做修改的情况下，需要返回一个JSONObject对象（或者Map也行）
        JSONObject res = new JSONObject();
        res.put("code", 1);
        // 将 record 返回以进行修改
        res.put("record", record);
        // TODO 不要 code 的概念
        return Result.OK(res);
    }
}
