/*
 * Copyright (C) 2009 Jayway AB
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
package org.jvending.masa.plugin;


import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.net.URL;

/**
 * @author hugo.josefson@jayway.com
 */
public class FileUtilsTest {
    @Test
    public void givenExampleFileContentsThenTempFileContainsSameData() throws IOException, InterruptedException {
        URL resource = this.getClass().getResource("exampleFile.txt");
        Assert.assertNotNull("resource should not be null.", resource);

        InputStream resourceStream = resource.openStream();
        Assert.assertNotNull("resourceStream should not be null.", resourceStream);

        final File   tempFile = FileUtils.createTempFileFrom(resourceStream, "myPrefix", ".mySuffix");
        Assert.assertNotNull("tempFile should not be null.", tempFile);

        final String readLine = new BufferedReader(new FileReader(tempFile)).readLine();
        Assert.assertEquals("This is example data.", readLine);
    }
}
