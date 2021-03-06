package mikaelhg.example;

import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSource;
import org.fusesource.scalate.spring.view.ScalateViewResolver;
import org.postgresql.ds.PGPoolingDataSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

/**
 * The application Spring Framework configuration class.
 */
@Configuration
@EnableWebMvc
@EnableTransactionManagement
@ComponentScan(basePackageClasses=ExampleConfiguration.class)
public class ExampleConfiguration extends WebMvcConfigurerAdapter {

    @Configuration
    @Profile("default")
    public static class DevelopmentProfileConfiguration {

        @Bean public DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .addScript("classpath:mikaelhg/example/dev-schema.sql")
                    .addScript("classpath:mikaelhg/example/dev-test-data.sql")
                    .build();
        }

        @Bean public JpaVendorAdapter jpaVendorAdapter() {
            final HibernateJpaVendorAdapter ret =
                    new HibernateJpaVendorAdapter();
            ret.setDatabase(Database.H2);
            return ret;
        }
    }

    @Configuration
    @Profile("postgresql")
    public static class PostgreSQLProfileConfiguration {

        @Resource private Environment env;

        @Bean public DataSource dataSource() {
            final PGPoolingDataSource ret = new PGPoolingDataSource();
            ret.setServerName(env.getProperty("db.servername"));
            ret.setDatabaseName(env.getProperty("db.databasename"));
            ret.setUser(env.getProperty("db.user"));
            ret.setPassword(env.getProperty("db.password"));
            return ret;
        }

        @Bean public JpaVendorAdapter jpaVendorAdapter() {
            final HibernateJpaVendorAdapter ret =
                    new HibernateJpaVendorAdapter();
            ret.setDatabase(Database.POSTGRESQL);
            return ret;
        }
    }

    @Configuration
    @Profile("mysql")
    public static class MySQLProfileConfiguration {

        @Resource private Environment env;

        @Bean public DataSource dataSource() {
            final BasicDataSource ret = new BasicDataSource();
            ret.setUrl(env.getProperty("db.url"));
            ret.setDriverClassName(
                    env.getProperty("db.driverClass", "com.mysql.jdbc.Driver"));
            ret.setUsername(env.getProperty("db.username"));
            ret.setPassword(env.getProperty("db.password"));
            ret.setValidationQuery("SELECT 1");
            return ret;
        }

        @Bean public JpaVendorAdapter jpaVendorAdapter() {
            final HibernateJpaVendorAdapter ret =
                    new HibernateJpaVendorAdapter();
            ret.setDatabase(Database.MYSQL);
            return ret;
        }
    }

    @Bean public FactoryBean<ExampleDao> exampleDao(
            final EntityManagerFactory emf, final BeanFactory beanFactory)
    {
        final JpaRepositoryFactoryBean<ExampleDao, Example, Long> ret =
                new JpaRepositoryFactoryBean<>();
        ret.setBeanFactory(beanFactory);
        ret.setEntityManager(emf.createEntityManager());
        ret.setRepositoryInterface(ExampleDao.class);
        ret.afterPropertiesSet();
        return ret;
    }

    @Bean public RequestToViewNameTranslator viewTranslator() {
        return new DefaultRequestToViewNameTranslator();
    }

    @Bean public ViewResolver scalateViewResolver() {
        final ScalateViewResolver ret = new ScalateViewResolver();
        ret.setOrder(1);
        ret.setPrefix("/WEB-INF/view/");
        ret.setSuffix(".scaml");
        return ret;
    }

    @Bean public ViewResolver internalResourceViewResolver() {
        final InternalResourceViewResolver ret =
                new InternalResourceViewResolver();
        ret.setOrder(2);
        ret.setViewClass(JstlView.class);
        ret.setPrefix("/WEB-INF/jsp/");
        ret.setSuffix(".jsp");
        return ret;
    }

    @Bean public FactoryBean<EntityManagerFactory> entityManagerFactory(
            final DataSource ds, final JpaVendorAdapter jva)
    {
        final LocalContainerEntityManagerFactoryBean ret =
                new LocalContainerEntityManagerFactoryBean();
        ret.setPackagesToScan("mikaelhg.example");
        ret.setDataSource(ds);
        ret.setJpaVendorAdapter(jva);
        ret.afterPropertiesSet();
        return ret;
    }

    @Bean public PlatformTransactionManager transactionManager(
            final EntityManagerFactory emf)
    {
        final JpaTransactionManager ret = new JpaTransactionManager();
        ret.setEntityManagerFactory(emf);
        return ret;
    }

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry reg) {
        reg.addResourceHandler("/resources/**")
                .addResourceLocations("/WEB-INF/resources/");
    }

}
