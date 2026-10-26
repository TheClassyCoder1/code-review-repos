package com.example.lending.ops.part2.admin;

import com.example.lending.ops.part2.admin.AdminConsoleService.R;
import com.example.lending.ops.part2.admin.Operation.SysLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ops/admin/dict")
@RequiredArgsConstructor
public class SysDictController {
	private final AdminConsoleService sysDictItemService;

	@SysLog("删除字典项")
	@DeleteMapping("/item/{id}")
	public R removeDictItemById(@PathVariable Long id) {
		return sysDictItemService.removeDictItem(id);
	}
}
