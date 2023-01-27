package org.example.user.dao;

import java.sql.Connection;
import java.sql.SQLException;

public class DConnectionMaker implements ConnectionMaker {
    @Override
    public Connection makeConnection() throws ClassNotFoundException, SQLException {
        // D 사에서 사용하는 Connection을 생성하는 코드를 작성
        return null;
    }
}
