package com.example.lending.risk.part7.watchlist;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * The watchlist lives on db-primary next to loans, so it is bound explicitly to the primary
 * datasource rather than whichever DataSource the auto-configuration would pick.
 */
@Configuration
public class WatchlistJdbcConfig {

    @Bean(name = "watchlistJdbcTemplate")
    public NamedParameterJdbcTemplate watchlistJdbcTemplate(@Qualifier("primaryDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
