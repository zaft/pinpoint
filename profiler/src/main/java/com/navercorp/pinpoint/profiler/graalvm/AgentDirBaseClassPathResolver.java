/*
 * Copyright 2018 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.profiler.graalvm;


import com.navercorp.pinpoint.bootstrap.BootLogger;
import com.navercorp.pinpoint.bootstrap.agentdir.BootDir;
import com.navercorp.pinpoint.bootstrap.agentdir.ClassPathResolver;
import com.navercorp.pinpoint.bootstrap.agentdir.JarDescription;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author emeroad
 */
public class AgentDirBaseClassPathResolver {

    private final BootLogger logger = BootLogger.getLogger(this.getClass());

    static final String VERSION_PATTERN = "(-[0-9]+\\.[0-9]+\\.[0-9]+((\\-SNAPSHOT)|(-RC[0-9]+))?)?";

    static final JarDescription bootstrap = new JarDescription("pinpoint-bootstrap", true);
    private static final String EXTENSIONS = "*.{jar,xml,properties}";

    // boot dir
    private final JarDescription commons = new JarDescription("pinpoint-commons", true);
    private final JarDescription commonsConfig = new JarDescription("pinpoint-commons-config", true);
    private final JarDescription bootstrapCore = new JarDescription("pinpoint-bootstrap-core", true);
    private final JarDescription annotations = new JarDescription("pinpoint-annotations", false);
    private final JarDescription bootstrapJava8 = new JarDescription("pinpoint-bootstrap-java8", false);
    private final JarDescription bootstrapJava9 = new JarDescription("pinpoint-bootstrap-java9", false);
    private final JarDescription bootstrapJava9internal = new JarDescription("pinpoint-bootstrap-java9-internal", false);
    private final JarDescription bootstrapJava15 = new JarDescription("pinpoint-bootstrap-java15", false);
    private final JarDescription bootstrapJava16 = new JarDescription("pinpoint-bootstrap-java16", false);
    private final List<JarDescription> bootJarDescriptions = Arrays.asList(commons, commonsConfig, bootstrapCore, annotations, bootstrapJava8, bootstrapJava9, bootstrapJava9internal, bootstrapJava15, bootstrapJava16);

    private final Path agentPath;


    public AgentDirBaseClassPathResolver(Path bootstrapJarPath) {
        this.agentPath = Objects.requireNonNull(bootstrapJarPath, "classPath");
    }

    public AgentDirectory resolve() {
//        if (!bootstrapJarPath.toFile().isFile()) {
//            throw new IllegalStateException(bootstrapJarPath + " not found");
//        }
//
//        // find bootstrap.jar
//        final Path bootstrapJarName = this.findBootstrapJar(this.bootstrapJarPath);
//        if (bootstrapJarName == null) {
//            throw new IllegalStateException(bootstrap.getSimplePattern() + " not found.");
//        }

        final Path agentDirPath = getAgentDirPath(this.agentPath);

        final AgentDirectory agentDirectory = new AgentDirectory(agentDirPath);

        return agentDirectory;
    }

    private Path getAgentDirPath(Path agentPath) {
        try {
            return agentPath.toRealPath().normalize();
        } catch (IOException e) {
            logger.warn(agentPath + " toRealPath() error. Error:" + e.getMessage(), e);
            return agentPath.toAbsolutePath();
        }
    }


}
