package com.example.lending.ops.part4.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Operator actions on the housekeeping scheduler, used during maintenance windows. */
@Slf4j
@Component
public class TaskUtil {

	public void pauseJobs(Scheduler scheduler) {
		try {
			if (scheduler != null) {
				scheduler.pauseAll();
			}
		}
		catch (Exception e) {
			log.error("暂停所有运行定时任务失败，失败信息：{}", e.getMessage());
		}
	}
}
