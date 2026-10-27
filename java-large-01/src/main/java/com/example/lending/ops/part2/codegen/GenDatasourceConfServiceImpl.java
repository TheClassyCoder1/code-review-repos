package com.example.lending.ops.part2.codegen;

import lombok.RequiredArgsConstructor;
import org.springframework.context.*;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GenDatasourceConfServiceImpl implements GenDatasourceConfService {

	private final GenDatasourceConfMapper baseMapper;

	private final StringEncryptor stringEncryptor;

	@Override
	public Boolean updateDsByEnc(GenDatasourceConf conf) {
		// 密码为空时，从数据库查询原始密码并解密，用于连接校验
		if (StrUtil.isBlank(conf.getPassword())) {
			GenDatasourceConf dbConf = this.baseMapper.selectById(conf.getId());
			conf.setPassword(stringEncryptor.decrypt(dbConf.getPassword()));
		}

		if (!checkDataSource(conf)) {
			return Boolean.FALSE;
		}
		// 先移除
		DynamicRoutingDataSource dynamicRoutingDataSource = SpringContextHolder.getBean(DynamicRoutingDataSource.class);
		dynamicRoutingDataSource.removeDataSource(baseMapper.selectById(conf.getId()).getName());

		// 再添加
		addDynamicDataSource(conf);

		// 更新数据库配置
		conf.setPassword(stringEncryptor.encrypt(conf.getPassword()));
		this.baseMapper.updateById(conf);
		return Boolean.TRUE;
	}

	private static DataSource dataSource(GenDatasourceConf conf) {
		return new DriverManagerDataSource(conf.getUrl(), conf.getUsername(), conf.getPassword());
	}

	private boolean checkDataSource(GenDatasourceConf conf) {
		try (Connection ignored = dataSource(conf).getConnection()) {
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	private void addDynamicDataSource(GenDatasourceConf conf) {
		SpringContextHolder.getBean(DynamicRoutingDataSource.class).dataSources.put(conf.getName(), dataSource(conf));
	}

	interface StrUtil {
		static boolean isBlank(CharSequence cs) { return cs == null || cs.toString().isBlank(); }
	}

	/** Named reporting datasources the report builder queries, swapped at runtime when configs change. */
	@Component
	public static class DynamicRoutingDataSource {
		final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

		public void removeDataSource(String name) { dataSources.remove(name); }
	}

	@Component
	public static class SpringContextHolder implements ApplicationContextAware {
		private static ApplicationContext context;

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) { context = applicationContext; }

		public static <T> T getBean(Class<T> type) { return context.getBean(type); }
	}

	@Repository
	@RequiredArgsConstructor
	public static class GenDatasourceConfMapper {
		private final JdbcTemplate jdbc;

		/** Returns the live configuration with this id, or {@code null} when there is none. */
		public GenDatasourceConf selectById(Long id) {
			return jdbc.query("SELECT id, name, url, username, password FROM ops_gen_datasource_conf WHERE id = ? AND del_flag = false",
					new BeanPropertyRowMapper<>(GenDatasourceConf.class), id).stream().findFirst().orElse(null);
		}

		public int updateById(GenDatasourceConf c) {
			return jdbc.update("UPDATE ops_gen_datasource_conf SET name = ?, url = ?, username = ?, password = ? WHERE id = ?",
					c.getName(), c.getUrl(), c.getUsername(), c.getPassword(), c.getId());
		}
	}
}
