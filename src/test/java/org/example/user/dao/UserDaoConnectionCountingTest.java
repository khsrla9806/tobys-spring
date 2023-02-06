package org.example.user.dao;

import org.example.user.domain.User;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.sql.SQLException;

public class UserDaoConnectionCountingTest {
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        ApplicationContext context = new AnnotationConfigApplicationContext(CountingDaoFactory.class);
        UserDaoJdbc userDaoJdbc = context.getBean("userDao", UserDaoJdbc.class);

        User user = new User();
        user.setId("hoon4");
        user.setName("훈훈");
        user.setPassword("123456789");

        userDaoJdbc.add(user);

        System.out.println(user.getId() + " 등록 성공!");

        User user2 = userDaoJdbc.get(user.getId());
        System.out.println(user2.getName());
        System.out.println(user2.getPassword());

        System.out.println(user2.getId() + " 조회 성공!");

        CountingConnectionMaker ccm = context.getBean("connectionMaker", CountingConnectionMaker.class);
        System.out.println("Connection Counter : " + ccm.getCounter());
    }
}
