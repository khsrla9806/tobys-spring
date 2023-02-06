package org.example.user.dao;

import org.example.user.domain.User;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

import java.sql.SQLException;

public class UserDaoTestXML {
    public static void main(String[] args) throws SQLException {
        ApplicationContext context = new GenericXmlApplicationContext("applicationContext.xml");
        UserDaoJdbc userDaoJdbc = context.getBean("userDao", UserDaoJdbc.class);

        User user = new User();
        user.setId("hoon8");
        user.setName("훈훈");
        user.setPassword("123456789");

        userDaoJdbc.add(user);

        System.out.println(user.getId() + " 등록 성공!");

        User user2 = userDaoJdbc.get(user.getId());
        System.out.println(user2.getName());
        System.out.println(user2.getPassword());

        System.out.println(user2.getId() + " 조회 성공!");
    }
}
