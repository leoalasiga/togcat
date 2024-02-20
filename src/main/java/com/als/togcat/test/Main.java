package com.als.togcat.test;


import javax.sql.DataSource;

/**
 * @description: 查看java的类加载器
 * jdk8及jdk8之前是 BootstrapClassLoader（引导启动类加载器） <- ExtensionClassLoader（扩展类加载器）<- App ClassLoader（应用类加载器）
 * jdk8以后是BootstrapClassLoader（引导启动类加载器） <- PlatformClassLoader（扩展类加载器）<- App ClassLoader（应用类加载器）
 * @author: liujiajie
 * @date: 2024/2/19 9:06
 */
public class Main {
    public static void main(String[] args) {
        System.out.println(String.class.getClassLoader());
        System.out.println(DataSource.class.getClassLoader());
        System.out.println(Main.class.getClassLoader());
        System.out.println(Thread.currentThread().getContextClassLoader());
        System.out.println(ClassLoader.getSystemClassLoader());

        System.out.println("1、ClassLoaderTest类的加载器" +
                Main.class.getClassLoader());
        System.out.println("2、ClassLoaderTest类的父类的加载器" +
                Main.class.getClassLoader().getParent());
        System.out.println("3、ClassLoaderTest类的父类的父类的加载器" +
                Main.class.getClassLoader().getParent().getParent());
    }
}
