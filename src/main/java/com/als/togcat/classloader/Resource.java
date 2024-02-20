package com.als.togcat.classloader;

import java.nio.file.Path;
import java.util.Objects;

/**
 * @description:
 * @author: liujiajie
 * @date: 2024/2/19 10:13
 */
public class Resource {
    final public Path path;
    final public String name;

    public Resource(Path path, String name) {
        this.path = path;
        this.name = name;
    }

    public Path getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "path=" + path +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Resource resource = (Resource) o;
        return path.equals(resource.path) && name.equals(resource.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, name);
    }


    public String name() {
        return this.name;
    }
}

// Java Record 是一种不变类，类似于 String，Integer。让我们看一个简单的例子。
//    public record Resource(Path path, String name) {
//
//    }
//https://blog.csdn.net/zhxdick/article/details/122099159
