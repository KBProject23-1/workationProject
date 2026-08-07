package com.workit.config;

import com.workit.domain.reservation.scheduler.ReservationStatusScheduler;
import com.workit.domain.reservation.service.ReservationService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.ControllerAdvice;

import javax.sql.DataSource;

@Configuration
@PropertySource({
        "classpath:/application.properties",
        "classpath:/application-secret.properties"
})
@MapperScan(basePackages = {"com.workit.domain.**.mapper"})
// 컨트롤러/예외 어드바이스는 Servlet(자식) 컨텍스트 전용 — 여기서 스캔하면 서비스 빈이 두 컨텍스트에 중복 생성되어
// @Transactional 프록시가 안 걸린 자식 컨텍스트 인스턴스가 컨트롤러에 주입되는 문제가 있었음 (ServletConfig 참고)
@ComponentScan(
        basePackages = {"com.workit.domain", "com.workit.exception"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = {Controller.class, ControllerAdvice.class}
        )
)

@EnableTransactionManagement
@EnableScheduling
@Import({RedisConfig.class, SecurityConfig.class})
public class RootConfig {
    @Value("${jdbc.driver}")
    String driver;
    @Value("${jdbc.url}")
    String url;
    @Value("${jdbc.username}")
    String username;
    @Value("${jdbc.password}")
    String password;
    @Autowired
    ApplicationContext applicationContext;

    // 이용 종료 예약의 완료 상태 변경 스케줄러 등록
    @Bean
    public ReservationStatusScheduler reservationStatusScheduler(
            ReservationService reservationService) {

        return new ReservationStatusScheduler(reservationService);
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        config.setDriverClassName(driver);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setAutoCommit(false);  // Transactional 설정 전환용

        HikariDataSource dataSource = new HikariDataSource(config);
        return dataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
        sqlSessionFactory.setConfigLocation(applicationContext.getResource("classpath:/mybatis-config.xml"));
        sqlSessionFactory.setDataSource(dataSource());

        return sqlSessionFactory.getObject();
    }

    @Bean
    public DataSourceTransactionManager transactionManager() {
        DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource());

        return manager;
    }

}
