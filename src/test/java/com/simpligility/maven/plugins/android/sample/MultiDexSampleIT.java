/*
 * Copyright (C) 2014 simpligility technologies inc.
 * Copyright (C) 2015 Piotr Soróbka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.simpligility.maven.plugins.android.sample;

import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Piotr Soróbka <psorobka@gmail.com>
 */
@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({"3.2.3"})
public class MultiDexSampleIT extends AbstractSampleIT {

    public MultiDexSampleIT(MavenRuntimeBuilder builder) throws Exception {
        super(builder);
    }

    @Test
    public void buildDeployAndRun() throws Exception {
        buildDeployAndRun("multidexsample", "com.simpligility.android.multidexsample",
                "com.simpligility.android.multidexsample.MultiDexSampleActivity");
    }

}
