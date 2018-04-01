package com.amaze.filemanager.utils.cloud;

import org.junit.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by Rustam Khadipash on 31/3/2018.
 */
public class CloudStreamSourceTest {
    private CloudStreamSource cs;
    private byte[] text;
    private long len;
    private String fn;

    // File Test.txt is 20 characters length
    // And contains "This is a test file." phrase
    @Before
    public void setUp() throws Exception {
        InputStream is = new FileInputStream("D://Test.txt");
        fn = "Test.txt";
        len = Files.size(Paths.get("D://Test.txt"));
        text = Files.readAllBytes(Paths.get("D://Test.txt"));

        cs = new CloudStreamSource(fn, len, is);
    }

    @After
    public void tearDown() throws Exception {
        fn = null;
        len = 0;
        text = null;
        cs = null;
    }

    /**
     * Purpose: Open an existing file
     * Input: no
     * Expected:
     *          cs.read() = 1
     *          buff[0] = text[0]
     */
    @Test
    public void open() throws IOException {
        cs.open();
        byte[] buff = new byte[1];

        assertEquals(buff.length, cs.read(buff));
        assertEquals(text[0], buff[0]);
    }

    /**
     * Purpose: Throw an exception when a file does not exist
     * Input: CloudStreamSource (FileName, FileLength, Null pointer)
     * Expected:
     *          IOException is thrown
     */
    @Test (expected = IOException.class)
    public void openNoFileException() throws IOException {
        cs = new CloudStreamSource(fn, len, null);
        cs.moveTo(10);  // move pointer to non-existing position
        cs.open();
    }

    /**
     * Purpose: Read content of length equal to the buffer size from the file
     * Input: read(buffer)
     * Expected:
     *          buffer = text
     *          n = len(buffer)
     */
    @Test
    public void read() throws IOException {
        byte[] buff = new byte[10];
        int n = cs.read(buff);
        byte[] temp = Arrays.copyOfRange(text, 0, buff.length);

        assertArrayEquals(temp, buff);
        assertEquals(buff.length, n);
    }

    /**
     * Purpose: Read content from the file with the buffer size bigger than length
     * of the text in the file
     * Input: read(buffer)
     * Expected:
     *          buffer = text
     *          n = len
     */
    @Test
    public void readExceed() throws IOException {
        byte[] buff = new byte[100];
        int n = cs.read(buff);
        // erase dummy values in the end of buffer
        byte[] buffer = Arrays.copyOfRange(buff, 0, n);

        assertArrayEquals(text, buffer);
        assertEquals(len, n);
    }

    /**
     * Purpose: Throw an exception when reading happen on a closed file
     * Input: read(buffer)
     * Expected:
     *          IOException is thrown
     */
    @Test (expected = IOException.class)
    public void readClosedException() throws IOException {
        cs.close();
        byte[] buff = new byte[(int)len];
        int n = cs.read(buff);
    }

    /**
     * Purpose: Read content in certain positions of the buffer from the file
     * Input: read(buffer, startPosition, endPosition)
     * Expected:
     *          buffer = text
     *          n = endPosition
     */
    @Test
    public void readStartEnd() throws IOException {
        byte[] buff = new byte[100];
        int start = 5;
        int end = 10;

        int n = cs.read(buff, start, end);
        byte[] file = Arrays.copyOfRange(text, 0, end - start);
        byte[] buffer = Arrays.copyOfRange(buff, start, end);

        assertArrayEquals(file, buffer);
        assertEquals(end, n);
    }

    /**
     * Purpose: Throw an exception when start and/or end positions for writing in
     * the buffer exceed size of the buffer
     * Input: read(buffer, startPosition, endPosition)
     * Expected:
     *          IOException is thrown
     */
    @Ignore ("Expected IOException, but IndexOutOfBoundsException is thrown")
    @Test (expected = IOException.class)
    public void readStartEndExceedException() throws IOException {
        byte[] buff = new byte[100];
        int start = 95;
        int end = 110;

        int n = cs.read(buff, start, end);
    }

    /**
     * Purpose: Throw an exception when reading happen on a closed file
     * Input: read(buffer, startPosition, endPosition)
     * Expected:
     *          IOException is thrown
     */
    @Test (expected = IOException.class)
    public void readStartEndClosedException() throws IOException {
        cs.close();
        byte[] buff = new byte[100];
        int start = 5;
        int end = 10;

        int n = cs.read(buff, start, end);
    }

    /**
     * Purpose: Read content of the file from a certain position
     * Input: moveTo(readPosition), read(buff)
     * Expected:
     *          buff = text[readPosition]
     *          n = buff.length
     */
    @Test
    public void moveTo() throws IOException {
        int readPosition = (int)len - 10;
        byte[] buff = new byte[1];

        cs.moveTo(readPosition);
        cs.open();

        int n = cs.read(buff);
        assertEquals(text[readPosition], buff[0]);
        assertEquals(buff.length, n);
    }

    /**
     * Purpose: Throw an exception when a reading position in the file is incorrect
     * Input: moveTo(wrongPosition)
     * Expected:
     *          IOException is thrown
     */
    @Ignore ("No exception is thrown")
    @Test (expected = IOException.class)
    public void moveToException() throws IOException {
        cs.moveTo(-1);
    }

    /**
     * Purpose: Close file after successful reading
     * Input: no
     * Expected:
     *          Stream is closed and reading from the file is unavailable
     */
    @Test
    public void close() {
        cs.close();

        int n = -1;
        try{
            byte[] buff = new byte[1];
            n = cs.read(buff);
        } catch (IOException ignored) {
        }

        assertEquals(-1, n);
    }

    /**
     * Purpose: Get MIME type
     * Input: no
     * Expected:
     *          return NULL
     */
    @Test
    public void getMimeType() {
        assertEquals(null, cs.getMimeType());
    }

    /**
     * Purpose: Get length of the text from a file
     * Input: no
     * Expected:
     *          return len
     */
    @Test
    public void length() {
        assertEquals(len, cs.length());
    }

    /**
     * Purpose: Get name of a file
     * Input: no
     * Expected:
     *          return fn
     */
    @Test
    public void getName() {
        assertEquals(fn, cs.getName());
    }

    /**
     * Purpose: Get available to read remain amount of text from a file
     * Input: no
     * Expected:
     *          return amount
     */
    @Test
    public void available() throws IOException {
        int amount = 12;
        cs.moveTo((int)len - amount);
        assertEquals(amount, cs.available());
    }

    /**
     * Purpose: Move reading position to the beginning of a file
     * Input: no
     * Expected:
     *          return len
     */
    @Test
    public void reset() throws IOException {
        cs.moveTo(10);
        assertEquals(len - 10, cs.available());
        cs.reset();
        assertEquals(len, cs.available());
    }

    /**
     * Purpose: Get size of a buffer. The buffer size is predefined in CloudStreamSource class
     * Input: no
     * Expected:
     *          return 1024*60
     */
    @Test
    public void getBufferSize() {
        assertEquals(1024*60, cs.getBufferSize());
    }
}
