package org.jvending.masa.plugin;


import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.net.URL;

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
