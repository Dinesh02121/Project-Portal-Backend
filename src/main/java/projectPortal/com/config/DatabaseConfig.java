package com.example.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

   
    @Value("${DATABASE_URL:}")
    private String databaseUrl;
    
    // Standard Spring datasource properties
    @Value("${spring.datasource.url:}")
    private String datasourceUrl;
    
    @Value("${spring.datasource.username:}")
    private String datasourceUsername;
    
    @Value("${spring.datasource.password:}")
    private String datasourcePassword;
    
    // Hikari connection pool settings from properties
    @Value("${spring.datasource.hikari.maximum-pool-size:3}")
    private int maximumPoolSize;
    
    @Value("${spring.datasource.hikari.minimum-idle:1}")
    private int minimumIdle;
    
    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;
    
    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;
    
    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;
    
    @Value("${spring.datasource.hikari.keepalive-time:300000}")
    private long keepaliveTime;

    /**
     * Primary DataSource Bean - Production Configuration
     * 
     * Supports two configuration methods:
     * 1. DATABASE_URL environment variable (Render/Railway/Heroku style)
     * 2. Spring datasource properties (Traditional Spring Boot style)
     * 
     * @return Configured HikariDataSource for production use
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        logger.info("========================================");
        logger.info("Initializing Production Database Configuration");
        logger.info("========================================");
        
        HikariConfig config = new HikariConfig();
        
        try {
            // Determine which configuration method to use
            if (isNotEmpty(databaseUrl)) {
                logger.info("📊 Configuring database from DATABASE_URL environment variable");
                configureDatabaseFromUrl(config, databaseUrl);
            } else if (isNotEmpty(datasourceUrl)) {
                logger.info("📊 Configuring database from spring.datasource properties");
                configureDatabaseFromProperties(config);
            } else {
                String errorMsg = "❌ No database configuration found! " +
                    "Please set either DATABASE_URL or spring.datasource.url";
                logger.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            
            // Set PostgreSQL driver
            config.setDriverClassName("org.postgresql.Driver");
            
            // Configure connection pool settings
            configureConnectionPool(config);
            
            // Configure connection validation
            configureConnectionValidation(config);
            
            // Configure SSL/TLS for Supabase
            configureSSL(config);
            
            // Additional performance and monitoring settings
            configureAdvancedSettings(config);
            
            // Create the datasource
            HikariDataSource dataSource = new HikariDataSource(config);
            
            // Test the connection
            testConnection(dataSource);
            
            logger.info("========================================");
            logger.info("✅ Database Configuration Successful!");
            logger.info("Pool Name: {}", config.getPoolName());
            logger.info("JDBC URL: {}", maskUrl(config.getJdbcUrl()));
            logger.info("Max Pool Size: {}", config.getMaximumPoolSize());
            logger.info("Min Idle: {}", config.getMinimumIdle());
            logger.info("========================================");
            
            return dataSource;
            
        } catch (URISyntaxException e) {
            logger.error("❌ Invalid DATABASE_URL format", e);
            throw new IllegalStateException("Failed to parse DATABASE_URL: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("❌ Database configuration failed", e);
            throw new IllegalStateException("Database configuration failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Configure database from DATABASE_URL format
     * Format: postgresql://username:password@host:port/database?params
     */
    private void configureDatabaseFromUrl(HikariConfig config, String url) throws URISyntaxException {
        logger.debug("Parsing DATABASE_URL...");
        
        URI dbUri = new URI(url);
        
        // Validate URI components
        if (dbUri.getUserInfo() == null || !dbUri.getUserInfo().contains(":")) {
            throw new IllegalArgumentException(
                "Invalid DATABASE_URL format - missing or invalid credentials. " +
                "Expected format: postgresql://user:password@host:port/database"
            );
        }
        
        // Extract credentials
        String[] credentials = dbUri.getUserInfo().split(":", 2);
        String username = credentials[0];
        String password = credentials[1];
        
        // Extract connection details
        String host = dbUri.getHost();
        int port = dbUri.getPort() > 0 ? dbUri.getPort() : 5432;
        String database = dbUri.getPath();
        
        // Build JDBC URL with required parameters
        String jdbcUrl = buildJdbcUrl(host, port, database);
        
        // Set connection parameters
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        
        logger.info("✓ Database URL parsed - Host: {}:{}, Database: {}", host, port, database);
    }
    
    /**
     * Configure database from Spring datasource properties
     */
    private void configureDatabaseFromProperties(HikariConfig config) {
        config.setJdbcUrl(datasourceUrl);
        config.setUsername(datasourceUsername);
        config.setPassword(datasourcePassword);
        
        logger.info("✓ Database properties configured");
    }
    
    /**
     * Build JDBC URL with required parameters for Supabase
     */
    private String buildJdbcUrl(String host, int port, String database) {
        // Ensure database path starts with /
        if (!database.startsWith("/")) {
            database = "/" + database;
        }
        
        // Build JDBC URL with SSL and schema
        return String.format(
            "jdbc:postgresql://%s:%d%s?sslmode=require&currentSchema=public",
            host, port, database
        );
    }
    
    /**
     * Configure HikariCP connection pool settings
     * Optimized for Supabase free tier (60 max connections)
     */
    private void configureConnectionPool(HikariConfig config) {
        config.setPoolName("ProjectPortalHikariPool");
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setKeepaliveTime(keepaliveTime);
        
        logger.debug("✓ Connection pool configured - Max: {}, Min: {}", 
                    maximumPoolSize, minimumIdle);
    }
    
    /**
     * Configure connection validation settings
     */
    private void configureConnectionValidation(HikariConfig config) {
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        config.setLeakDetectionThreshold(60000);
        
        logger.debug("✓ Connection validation configured");
    }
    
    /**
     * Configure SSL/TLS settings for secure Supabase connection
     */
    private void configureSSL(HikariConfig config) {
        config.addDataSourceProperty("ssl", "true");
        config.addDataSourceProperty("sslmode", "require");
        config.addDataSourceProperty("sslrootcert", "system");
        
        logger.debug("✓ SSL/TLS configured");
    }
    
    /**
     * Configure advanced HikariCP settings for production
     */
    private void configureAdvancedSettings(HikariConfig config) {
        // Auto-commit (important for data persistence)
        config.setAutoCommit(true);
        
        // Application name for connection tracking
        config.addDataSourceProperty("ApplicationName", "ProjectPortal");
        
        // Network timeout settings
        config.addDataSourceProperty("socketTimeout", "30");
        config.addDataSourceProperty("tcpKeepAlive", "true");
        config.addDataSourceProperty("loginTimeout", "10");
        
        // PostgreSQL-specific optimizations
        config.addDataSourceProperty("stringtype", "unspecified");
        config.addDataSourceProperty("prepareThreshold", "3");
        config.addDataSourceProperty("preparedStatementCacheQueries", "256");
        config.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");
        
        logger.debug("✓ Advanced settings configured");
    }
    
    /**
     * Test database connection on startup
     */
    private void testConnection(HikariDataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                logger.info("✓ Database connection test successful");
                
                // Log database metadata
                var metadata = connection.getMetaData();
                logger.info("Database: {} {}", 
                    metadata.getDatabaseProductName(),
                    metadata.getDatabaseProductVersion());
                logger.info("Driver: {} {}", 
                    metadata.getDriverName(),
                    metadata.getDriverVersion());
            } else {
                logger.warn("⚠️ Database connection test failed - connection not valid");
            }
        } catch (SQLException e) {
            logger.error("❌ Database connection test failed", e);
            throw new IllegalStateException("Cannot connect to database: " + e.getMessage(), e);
        }
    }
    
    private String maskUrl(String url) {
        if (url == null) return "null";
        // Mask password in URL if present
        return url.replaceAll("password=[^&]*", "password=***");
    }
    
    private boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }
}
