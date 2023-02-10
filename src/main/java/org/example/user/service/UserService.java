package org.example.user.service;

import org.example.user.dao.UserDao;
import org.example.user.domain.Level;
import org.example.user.domain.User;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import javax.xml.crypto.Data;
import java.sql.Connection;
import java.util.List;

public class UserService {
    public static final int MIN_LOGIN_COUNT_FOR_SILVER = 50;
    public static final int MIN_RECOMMEND_COUNT_FOR_GOLD = 30;
    private UserDao userDao;
    private DataSource dataSource;

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void upgradeLevels() throws Exception {
        TransactionSynchronizationManager.initSynchronization(); // 트랜잭션 동기화 작업을 초기화
        Connection c = DataSourceUtils.getConnection(dataSource); // connection을 가져오는 동시에 트랜잭션 동기화 저장소에 바인딩 해줌
        c.setAutoCommit(false); // 트랜잭션 시작을 위해 자동 commit 기능은 false로 설정

        try {
            List<User> users = userDao.getAll();

            for (User user : users) {
                if (canUpgradeLevel(user)) {
                    upgradeLevel(user);
                }
            }
            c.commit();
        } catch (Exception e) {
            c.rollback(); // 예외가 발생되면 트랜잭션 롤백을 진행
            throw e;
        } finally {
            DataSourceUtils.releaseConnection(c, dataSource); // Connection을 안전하게 닫음
            TransactionSynchronizationManager.unbindResource(this.dataSource);
            TransactionSynchronizationManager.clearSynchronization(); // 동기화 작업 종료 및 정리
        }
    }

    private boolean canUpgradeLevel(User user) {
        Level currentLevel = user.getLevel();

        switch (currentLevel) {
            case BASIC: return (user.getLogin() >= MIN_LOGIN_COUNT_FOR_SILVER);
            case SILVER: return (user.getRecommend() >= MIN_RECOMMEND_COUNT_FOR_GOLD);
            case GOLD: return false;
            default: throw new IllegalArgumentException("Unknown Level : " + currentLevel);
        }
    }

    protected void upgradeLevel(User user) {
        user.upgradeLevel();
        userDao.update(user);
    }

    public void add(User user) {
        if (user.getLevel() == null) {
            user.setLevel(Level.BASIC);
        }

        userDao.add(user);
    }
}
