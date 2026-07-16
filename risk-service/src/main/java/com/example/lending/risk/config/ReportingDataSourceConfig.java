package com.example.lending.risk.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * REPORTING datasource -> db-secondary:5432, schema "lending" (SAME schema name, DIFFERENT host).
 * Scans entities in ...risk.reporting.entity and repositories in ...risk.repository.reporting.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.example.lending.risk.repository.reporting",
        entityManagerFactoryRef = "reportingEntityManagerFactory",
        transactionManagerRef = "reportingTransactionManager")
public class ReportingDataSourceConfig {

    @Bean(name = "reportingDataSource")
    @ConfigurationProperties("datasource.reporting")
    public DataSource reportingDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "reportingEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean reportingEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("reportingDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.example.lending.risk.reporting.entity")
                .persistenceUnit("reporting")
                .build();
    }

    @Bean(name = "reportingTransactionManager")
    public PlatformTransactionManager reportingTransactionManager(
            @Qualifier("reportingEntityManagerFactory")
            LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }
}
