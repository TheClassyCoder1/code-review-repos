package org.mercyhealth.lab;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.net.http.HttpClient;
import org.mercyhealth.lab.dao.OrderDao;
import org.mercyhealth.lab.dao.PhysicianDao;
import org.mercyhealth.lab.dao.ResultDao;
import org.mercyhealth.lab.service.AnalyzerBridge;
import org.mercyhealth.lab.service.AttachmentStore;
import org.mercyhealth.lab.service.PasswordHasher;
import org.mercyhealth.lab.service.ReferenceLabClient;
import org.mercyhealth.lab.service.SampleImport;
import org.mercyhealth.lab.service.TokenService;

@WebListener
public class LabBootstrap implements ServletContextListener {

    private HikariDataSource dataSource;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        LabConfig config = LabConfig.fromEnvironment();
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.jdbcUrl());
        hikari.setUsername(config.jdbcUser());
        hikari.setPassword(config.jdbcPassword());
        hikari.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(hikari);

        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(config.httpTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        ServletContext ctx = event.getServletContext();
        ctx.setAttribute(LabConfig.class.getName(), config);
        ctx.setAttribute(javax.sql.DataSource.class.getName(), dataSource);
        ctx.setAttribute(OrderDao.class.getName(), new OrderDao(dataSource));
        ctx.setAttribute(ResultDao.class.getName(), new ResultDao(dataSource));
        ctx.setAttribute(PhysicianDao.class.getName(), new PhysicianDao(dataSource));
        ctx.setAttribute(TokenService.class.getName(), new TokenService(dataSource));
        ctx.setAttribute(PasswordHasher.class.getName(), new PasswordHasher());
        ctx.setAttribute(AnalyzerBridge.class.getName(), new AnalyzerBridge(config.analyzerExportDir()));
        ctx.setAttribute(AttachmentStore.class.getName(), new AttachmentStore(config.attachmentRoot()));
        ctx.setAttribute(ReferenceLabClient.class.getName(), new ReferenceLabClient(http, config));
        ctx.setAttribute(SampleImport.class.getName(), new SampleImport());
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T lookup(ServletContext ctx, Class<T> type) {
        return (T) ctx.getAttribute(type.getName());
    }
}
