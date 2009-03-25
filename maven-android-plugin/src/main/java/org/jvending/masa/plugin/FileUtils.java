package org.jvending.masa.plugin;

import java.io.*;

/**
 * File utility methods.
 *
 * @author <a href="mailto:hugo.josefson@jayway.se">hugo.josefson@jayway.se</a>
 */
public class FileUtils {
    /**
     * Reads from <code>inputStream</code> and writes to a new temp file. The temp file will be deleted when the JVM exits.
     * @param inputStream {@link java.io.InputStream} to read from
     * @param tempFilenamePrefix prefix for the new temp file's filename
     * @param tempFilenameSuffix suffix for the new temp file's filename
     * @return the new temp file, containing all data from the <code>inputStream</code>
     * @throws java.io.IOException
     * @throws InterruptedException
     */
    public static File createTempFileFrom(InputStream inputStream, String tempFilenamePrefix, String tempFilenameSuffix) throws IOException, InterruptedException {
        File tempFile = File.createTempFile(tempFilenamePrefix, tempFilenameSuffix);
        tempFile.deleteOnExit();

        final FileOutputStream outputStream = new FileOutputStream(tempFile);
        try {
            final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            try {
                byte[] buf       = new byte[1024];
                int    bytesRead;
                do {
                    bytesRead = bufferedInputStream.read(buf);
                    if (bytesRead > 0) {
                        outputStream.write(buf, 0, bytesRead);
                    }
                    Thread.sleep(10);
                } while (bytesRead > -1);
            }finally {
                bufferedInputStream.close();
            }
        }finally {
            outputStream.close();
        }
        return tempFile;
    }
}
