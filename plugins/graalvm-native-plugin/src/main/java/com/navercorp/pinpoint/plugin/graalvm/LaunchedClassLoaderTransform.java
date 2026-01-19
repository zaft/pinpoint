package com.navercorp.pinpoint.plugin.graalvm;

import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.plugin.graalvm.DynamicClassLoader;
import com.navercorp.pinpoint.bootstrap.plugin.util.InstrumentUtils;

import java.security.ProtectionDomain;

public class LaunchedClassLoaderTransform implements TransformCallback {

    @Override
    public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
        InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

        if (!target.isInterceptable()) {
            return null;
        }
        target.addField(DynamicClassLoader.class);
        InstrumentUtils.findMethod(target, "loadClass",  "java.lang.String", "boolean")
                .addInterceptor(GraalVmInterceptor.class, new Object[]{loader});


        return target.toBytecode();
    }
};