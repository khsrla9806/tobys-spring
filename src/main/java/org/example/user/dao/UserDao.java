package org.example.user.dao;

import org.example.user.domain.User;

import java.sql.*;

public abstract class UserDao {
    public void add(User user) throws ClassNotFoundException, SQLException {
        Connection c = getConnection();

        PreparedStatement ps = c.prepareStatement(
                "insert into users(id, name, password) values(?,?,?)");
        ps.setString(1, user.getId());
        ps.setString(2, user.getName());
        ps.setString(3, user.getPassword());

        ps.executeUpdate();

        ps.close();
        c.close();
    }

    public User get(String id) throws ClassNotFoundException, SQLException {
        Connection c = getConnection();

        PreparedStatement ps = c.prepareStatement(
                "select * from users where id = ?");
        ps.setString(1, id);

        ResultSet rs = ps.executeQuery();
        rs.next();
        User user = new User();
        user.setId(rs.getString("id"));
        user.setName(rs.getString("name"));
        user.setPassword(rs.getString("password"));

        rs.close();
        ps.close();
        c.close();

        return user;
    }

    public abstract Connection getConnection() throws ClassNotFoundException, SQLException;

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
//        UserDao dao = new UserDao();
//
//        User user = new User();
//        user.setId("kimhunsope");
//        user.setName("김훈섭");
//        user.setPassword("!hoon1234");
//
//        dao.add(user);
//
//        System.out.println(user.getId() + " 등록 성공!");
//
//        User user2 = dao.get(user.getId());
//        System.out.println(user2.getName());
//        System.out.println(user2.getPassword());
//
//        System.out.println(user2.getId() + " 조회 성공!");
    }
}
