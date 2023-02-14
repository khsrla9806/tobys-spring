package org.example.user.service;

import org.example.user.dao.UserDao;
import org.example.user.domain.Level;
import org.example.user.domain.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.example.user.service.UserServiceImpl.MIN_LOGIN_COUNT_FOR_SILVER;
import static org.example.user.service.UserServiceImpl.MIN_RECOMMEND_COUNT_FOR_GOLD;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/test-applicationContext.xml")
public class UserServiceTest {
    @Autowired
    UserService userService;

    @Autowired
    UserServiceImpl userServiceImpl;

    @Autowired
    UserDao userDao;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    MailSender mailSender;

    List<User> users;

    @Before
    public void setUp() {
        users = Arrays.asList(
                new User("hoon", "훈", "p1234", Level.BASIC, MIN_LOGIN_COUNT_FOR_SILVER - 1, 0, "a@kakao.com"),
                new User("you", "유", "p1234", Level.BASIC, MIN_LOGIN_COUNT_FOR_SILVER, 0, "b@kakao.com"),
                new User("min", "민", "p1234", Level.SILVER, 60, MIN_RECOMMEND_COUNT_FOR_GOLD - 1, "c@kakao.com"),
                new User("young", "영", "p1234", Level.SILVER, 60, MIN_RECOMMEND_COUNT_FOR_GOLD, "d@kakao.com"),
                new User("sun", "선", "p1234", Level.GOLD, 100, Integer.MAX_VALUE, "e@kakao.com")
        );
    }

    @Test
    public void upgradeLevels() {
        UserServiceImpl userServiceImpl = new UserServiceImpl(); // 고립된 테스트에서는 직접 오브젝트를 생성

        // 다이나믹한 목 오브젝트 생성과 메서드의 리턴 값을 설정한 후 DI를 진행
        UserDao mockUserDao = mock(UserDao.class);
        when(mockUserDao.getAll()).thenReturn(this.users);
        userServiceImpl.setUserDao(mockUserDao);

        // 리턴 값이 없는 메서드를 가진 목 오브젝트는 더욱 간단하게 생성이 가능
        MailSender mockMailSender = mock(MailSender.class);
        userServiceImpl.setMailSender(mockMailSender);

        userServiceImpl.upgradeLevels(); // 테스트 대상 실행

        // 목 오브젝트가 제공하는 검증 기능을 통해서 어떤 메서드가 몇 번 호출되었는지 파라미터는 무엇인지 확인 가능
        verify(mockUserDao, times(2)).update(any(User.class));
        verify(mockUserDao).update(users.get(1));
        assertThat(users.get(1).getLevel(), is(Level.SILVER));
        verify(mockUserDao).update(users.get(3));
        assertThat(users.get(3).getLevel(), is(Level.GOLD));

        ArgumentCaptor<SimpleMailMessage> mailMessageArg =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        // 파라미터를 정밀하게 검사하기 위해서 캡처도 할 수 있다.
        verify(mockMailSender, times(2)).send(mailMessageArg.capture());
        List<SimpleMailMessage> mailMessages = mailMessageArg.getAllValues();
        assertThat(mailMessages.get(0).getTo()[0], is(users.get(1).getEmail()));
        assertThat(mailMessages.get(1).getTo()[0], is(users.get(3).getEmail()));
    }

    private void checkUserAndLevel(User updated, String expectedId, Level expectedLevel) {
        assertThat(updated.getId(), is(expectedId));
        assertThat(updated.getLevel(), is(expectedLevel));
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
        TestUserService testUserService = new TestUserService(users.get(3).getId()); // 4번째 user에서 예외 발생
        testUserService.setUserDao(userDao); // 수동으로 DI를 진행
        testUserService.setMailSender(mailSender);

        // 트랜잭션 처리 부분을 위한 수동 DI 설정
        UserServiceTx userServiceTx = new UserServiceTx();
        userServiceTx.setTransactionManager(transactionManager);
        userServiceTx.setUserService(testUserService);

        userDao.deleteAll();
        for (User user : users) {
            userDao.add(user);
        }

        try {
            userServiceTx.upgradeLevels(); // 여기서 예외가 발생되야 한다.
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

    static class TestUserService extends UserServiceImpl {
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

    static class MockMailSender implements MailSender {
        private List<String> requests = new ArrayList<String>();

        public List<String> getRequests() {
            return requests;
        }

        @Override
        public void send(SimpleMailMessage mailMessage) throws MailException {
            // 전송 요청을 받은 이메일 주소를 저장해둔다. 간단하게 첫 번째 수신자 메일 주소만 저장
            requests.add(mailMessage.getTo()[0]);
        }

        @Override
        public void send(SimpleMailMessage[] mailMessages) throws MailException {

        }
    }

    static class MockUserDao implements UserDao {
        private List<User> users; // 업그레이드 후보를 저장할 리스트
        private List<User> updated = new ArrayList<>(); // 업그레이드 대상 오브젝트를 저장해둘 목록

        private MockUserDao(List<User> users) {
            this.users = users;
        }

        public List<User> getUpdated() {
            return this.updated;
        }

        @Override
        public List<User> getAll() {
            return this.users; // 스텁 기능을 제공한다.
        }

        @Override
        public void update(User user) {
            updated.add(user); // 목 오브젝트 기능을 제공
        }

        // 테스트에 사용되지 않는 메서드들을 정리 (인터페이스 상속을 위해서 꼭 구현은 해놔야 함)
        @Override
        public void add(User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public User get(String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getCount() {
            throw new UnsupportedOperationException();
        }
    }
}
