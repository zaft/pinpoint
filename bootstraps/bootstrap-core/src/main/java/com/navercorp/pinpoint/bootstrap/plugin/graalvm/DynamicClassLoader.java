package com.navercorp.pinpoint.bootstrap.plugin.graalvm;

public interface DynamicClassLoader {

    void setDynamicClassLoader(ClassLoader dynamicClassLoader);

    ClassLoader getDynamicClassLoader();

}
