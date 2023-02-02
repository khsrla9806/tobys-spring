package org.example.user.dao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;

@Configuration
public class CountingDaoFactory {
    @Bean
    public UserDao userDao() {
        UserDao userDao = new UserDao();
        userDao.setJdbcTemplate(dataSource());
        return userDao;
    }

    @Bean
    public DataSource dataSource() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();

        dataSource.setDriverClass(com.mysql.cj.jdbc.Driver.class);
        dataSource.setUrl("jdbc:mysql//localhost/tobyspring");
        dataSource.setUsername("root");
        dataSource.setPassword("");

        return dataSource;
    }

    @Bean
    public ConnectionMaker realConnectionMaker() {
        return new AConnectionMaker();
    }
}
