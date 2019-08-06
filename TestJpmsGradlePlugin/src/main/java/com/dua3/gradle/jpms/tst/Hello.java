package com.dua3.gradle.jpms.tst;

public class Hello {

    public static void main(String[] args) {
        new Hello().sayHello();
    }

    public Hello () {}

    public void sayHello() {
        System.out.println("Hello World!");
    }

}
