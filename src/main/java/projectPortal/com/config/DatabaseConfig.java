package projectPortal.com.config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class DatabaseConfig {

    @Value("${DATABASE_URL}")
    private String databaseUrl;

    @Bean
    public DataSource dataSource() throws URISyntaxException {
        HikariConfig config = new HikariConfig();

        // Parse the DATABASE_URL
        URI dbUri = new URI(databaseUrl);

        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];
        String host = dbUri.getHost();
        int port = dbUri.getPort();
        String path = dbUri.getPath();

        // Build JDBC URL
        String jdbcUrl = String.format(
                "jdbc:postgresql://%s:%d%s?sslmode=require",
                host, port, path
        );

        // Configure HikariCP
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        // Connection pool settings (from your application.properties)
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(20000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1200000);

        return new HikariDataSource(config);
    }
}
