/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.navercorp.pinpoint.profiler.transformer;

import com.navercorp.pinpoint.profiler.context.provider.plugin.PluginClassLoader;
import com.navercorp.pinpoint.profiler.instrument.transformer.LambdaClassFileResolver;
import com.navercorp.pinpoint.profiler.instrument.transformer.TransformerRegistry;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Objects;


/**
 * @author emeroad
 * @author netspider
 * @author jaehong.kim
 */
public class DefaultClassFileTransformerDispatcher implements ClassFileTransformerDispatcher {

    private final BaseClassFileTransformer baseClassFileTransformer;
    private final TransformerRegistry transformerRegistry;
    private final DynamicTransformerRegistry dynamicTransformerRegistry;

    private final ClassFileFilter classLoaderFilter;
    private final ClassFileFilter pinpointClassFilter;
    private final ClassFileFilter unmodifiableFilter;

    private final LambdaClassFileResolver lambdaClassFileResolver;
    private final ClassLoader pluginClassLoader;

    public DefaultClassFileTransformerDispatcher(ClassFileFilter pinpointClassFilter,
                                                 ClassFileFilter unmodifiableFilter,
                                                 TransformerRegistry transformerRegistry,
                                                 DynamicTransformerRegistry dynamicTransformerRegistry,
                                                 LambdaClassFileResolver lambdaClassFileResolver,
                                                 ClassLoader pluginClassLoader) {

        this.baseClassFileTransformer = new BaseClassFileTransformer(this.getClass().getClassLoader());

        this.classLoaderFilter = new PinpointClassLoaderFilter(this.getClass().getClassLoader());
        this.pinpointClassFilter = Objects.requireNonNull(pinpointClassFilter, "pinpointClassFilter");
        this.unmodifiableFilter = Objects.requireNonNull(unmodifiableFilter, "unmodifiableFilter");

        this.transformerRegistry = Objects.requireNonNull(transformerRegistry, "transformerRegistry");
        this.dynamicTransformerRegistry = Objects.requireNonNull(dynamicTransformerRegistry, "dynamicTransformerRegistry");
        this.lambdaClassFileResolver = Objects.requireNonNull(lambdaClassFileResolver, "lambdaClassFileResolver");
        this.pluginClassLoader = Objects.requireNonNull(pluginClassLoader, "pluginClassLoader");
    }

    @Override
    public byte[] transform(ClassLoader classLoader, String classInternalName, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) throws IllegalClassFormatException {
        if(classInternalName.equals("org/springframework/web/servlet/FrameworkServlet")) {
            System.out.println(classInternalName);
        }
        if (!classLoaderFilter.accept(classLoader, classInternalName, classBeingRedefined, protectionDomain, classFileBuffer)) {
            return null;
        }

        final String internalName = lambdaClassFileResolver.resolve(classLoader, classInternalName, protectionDomain, classFileBuffer);
        if (internalName == null) {
            return null;
        }
        if (!pinpointClassFilter.accept(classLoader, internalName, classBeingRedefined, protectionDomain, classFileBuffer)) {
            return null;
        }

        final ClassFileTransformer dynamicTransformer = dynamicTransformerRegistry.getTransformer(classLoader, internalName);
        if (dynamicTransformer != null) {

            if(Boolean.parseBoolean(System.getProperty("pinpoint.classLoader.useSystem"))) {
                if (pluginClassLoader instanceof PluginClassLoader) {
                    PluginClassLoader pcl = (PluginClassLoader) pluginClassLoader;
                    if (pcl.getDynamicParent() == null) {
                        pcl.setDynamicParent(classLoader);
                    }
                    if (pcl.getDynamicParent() == classLoader) {
                        return baseClassFileTransformer.transform(pcl, internalName, classBeingRedefined, protectionDomain, classFileBuffer, dynamicTransformer);
                    }
                }
            }
            return baseClassFileTransformer.transform(classLoader, internalName, classBeingRedefined, protectionDomain, classFileBuffer, dynamicTransformer);
        }

        if (!unmodifiableFilter.accept(classLoader, internalName, classBeingRedefined, protectionDomain, classFileBuffer)) {
            return null;
        }

        final ClassFileTransformer transformer = this.transformerRegistry.findTransformer(classLoader, internalName, classFileBuffer);
        if (transformer == null) {
            return null;
        }
        if(Boolean.parseBoolean(System.getProperty("pinpoint.classLoader.useSystem"))) {
            if (pluginClassLoader instanceof PluginClassLoader) {
                PluginClassLoader pcl = (PluginClassLoader) pluginClassLoader;
                if (pcl.getDynamicParent() == null) {
                    pcl.setDynamicParent(classLoader);
                }
                if (pcl.getDynamicParent() == classLoader) {
                    return baseClassFileTransformer.transform(pcl, internalName, classBeingRedefined, protectionDomain, classFileBuffer, transformer);
                }
            }
        }
        return baseClassFileTransformer.transform(classLoader, internalName, classBeingRedefined, protectionDomain, classFileBuffer, transformer);
    }

}