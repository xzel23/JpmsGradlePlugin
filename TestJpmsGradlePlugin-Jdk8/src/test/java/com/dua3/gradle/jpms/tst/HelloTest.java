package com.dua3.gradle.jpms.tst;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HelloTest {

    private static Hello inst;

    @BeforeAll
    static void init() {
        inst = new Hello();
    }

    @AfterAll
    static void cleanup() {
        inst = null;
    }

    @Test
    static void sayHello() {
        inst.sayHello(); throw new RuntimeException("BANG!");
    }
}