package org.example.user.dao;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class JUnitTest {
    static Set<JUnitTest> testObject = new HashSet<>();

    @Test
    public void test1() {
        assertThat(testObject, is(not(hasItem(this))));
        testObject.add(this);
    }

    @Test
    public void test2() {
        assertThat(testObject, is(not(hasItem(this))));
        testObject.add(this);
    }

    @Test
    public void test3() {
        assertThat(testObject, is(not(hasItem(this))));
        testObject.add(this);
    }
}
