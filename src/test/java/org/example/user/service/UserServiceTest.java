package org.example.user.service;

import org.example.user.dao.UserDao;
import org.example.user.domain.Level;
import org.example.user.domain.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

import static org.example.user.service.UserService.MIN_LOGIN_COUNT_FOR_SILVER;
import static org.example.user.service.UserService.MIN_RECOMMEND_COUNT_FOR_GOLD;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/test-applicationContext.xml")
public class UserServiceTest {
    @Autowired
    UserService userService;

    @Autowired
    UserDao userDao;

    @Autowired
    PlatformTransactionManager transactionManager;

    List<User> users;

    @Before
    public void setUp() {
        users = Arrays.asList(
                new User("hoon", "훈", "p1234", Level.BASIC, MIN_LOGIN_COUNT_FOR_SILVER - 1, 0, "kimhunsope@kakao.com"),
                new User("you", "유", "p1234", Level.BASIC, MIN_LOGIN_COUNT_FOR_SILVER, 0, "kimhunsope@kakao.com"),
                new User("min", "민", "p1234", Level.SILVER, 60, MIN_RECOMMEND_COUNT_FOR_GOLD - 1, "kimhunsope@kakao.com"),
                new User("young", "영", "p1234", Level.SILVER, 60, MIN_RECOMMEND_COUNT_FOR_GOLD, "kimhunsope@kakao.com"),
                new User("sun", "선", "p1234", Level.GOLD, 100, Integer.MAX_VALUE, "kimhunsope@kakao.com")
        );
    }

    @Test
    public void upgradeLevels() {
        userDao.deleteAll();
        for (User user : users) {
            userDao.add(user);
        }

        userService.upgradeLevels();

        checkLevelUpgraded(users.get(0), false);
        checkLevelUpgraded(users.get(1), true);
        checkLevelUpgraded(users.get(2), false);
        checkLevelUpgraded(users.get(3), true);
        checkLevelUpgraded(users.get(4), false);
    }

    @Test
    public void add() {
        userDao.deleteAll();

        User userWithLevel = users.get(4); // GOLD 레벨의 유저

        User userWithoutLevel = users.get(0);
        userWithoutLevel.setLevel(null); // 레벨이 비어있는 사용자를 하나 만듬

        userService.add(userWithLevel);
        userService.add(userWithoutLevel);

        User userWithLevelRead = userDao.get(userWithLevel.getId());
        User userWithoutLevelRead = userDao.get(userWithoutLevel.getId());

        assertThat(userWithLevelRead.getLevel(), is(userWithLevel.getLevel()));
        assertThat(userWithoutLevelRead.getLevel(), is(Level.BASIC));
    }

    @Test
    public void upgradeAllOrNothing() {
        UserService testUserService = new TestUserService(users.get(3).getId()); // 4번째 user에서 예외 발생
        testUserService.setUserDao(userDao); // 수동으로 DI를 진행
        testUserService.setTransactionManager(transactionManager); // 수동으로 DI 진행

        userDao.deleteAll();
        for (User user : users) {
            userDao.add(user);
        }

        try {
            testUserService.upgradeLevels(); // 여기서 예외가 발생되야 한다.
            fail("TestUserServiceException expected"); // 정상 종료 시에는 테스트 실패 메시지 출력
        } catch (TestUserServiceException e) {

        }

        checkLevelUpgraded(users.get(1), false); // 변경 전 데이터와 동일한지 확인
    }

    private void checkLevelUpgraded(User user, boolean upgraded) {
        User userUpdate = userDao.get(user.getId());

        if (upgraded) {
            assertThat(userUpdate.getLevel(), is(user.getLevel().nextLevel()));
        } else {
            assertThat(userUpdate.getLevel(), is(user.getLevel()));
        }
    }

    static class TestUserService extends UserService {
        private String id;

        private TestUserService(String id) {
            this.id = id;
        }

        @Override
        protected void upgradeLevel(User user) {
            if (user.getId().equals(this.id)) {
                throw new TestUserServiceException();
            }
            super.upgradeLevel(user);
        }
    }

    static class TestUserServiceException extends RuntimeException {

    }
}
