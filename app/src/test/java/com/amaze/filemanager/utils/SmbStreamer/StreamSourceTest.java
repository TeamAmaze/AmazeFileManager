package com.amaze.filemanager.utils.SmbStreamer;

import org.junit.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

import static org.junit.Assert.*;

/**
 * Created by Rustam Khadipash on 30/3/2018.
 */
public class StreamSourceTest {
    private SmbFile file;
    private StreamSource ssEmpty;
    private StreamSource ss;
    private byte[] text;

    // File Test.txt is 20 characters length
    // And contains "This is a test file." phrase
    @Before
    public void setUp() throws Exception {
        file = new SmbFile("smb://192.168.0.101/Test/Test.txt",
                new NtlmPasswordAuthentication(null, "usertest", "12345"));

        text = ((new BufferedReader(new InputStreamReader(new SmbFileInputStream(file))))
                .readLine()).getBytes();
        ssEmpty = new StreamSource();
        ss = new StreamSource(file, file.length());
    }

    @After
    public void tearDown() throws Exception {
        file = null;
        text = null;
        ss = null;
    }

    /**
     * Purpose: Open an empty stream
     * Input: no
     * Expected:
     *          IOException is thrown
     */
    @Test (expected = IOException.class)
    public void openEmpty() throws IOException {
        ssEmpty.open();
    }


    /*
      From now on ssEmpty will not be used since StreamSource()
      constructor does not initialize any internal variables
     */


    /**
     * Purpose: Open an existing file
     * Input: no
     * Expected:
     *          cs.read() = 1
     *          buff[0] = text[0]
     */
    @Test
    public void openExisting() throws IOException {
        ss.open();
        byte[] buff = new byte[1];

        assertEquals(buff.length, ss.read(buff));
        assertEquals(text[0], buff[0]);
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
        int n = ss.read(buff);
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
        int n = ss.read(buff);
        // erase dummy values in the end of buffer
        byte[] buffer = Arrays.copyOfRange(buff, 0, n);

        assertArrayEquals(text, buffer);
        assertEquals(text.length, n);
    }

    /**
     * Purpose: Throw an exception when reading happen on a closed file
     * Input: read(buffer)
     * Expected:
     *          IOException is thrown
     */
    @Test (expected = IOException.class)
    public void readClosedException() throws IOException {
        ss.close();
        byte[] buff = new byte[text.length];
        int n = ss.read(buff);
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

        int n = ss.read(buff, start, end);
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

        int n = ss.read(buff, start, end);
    }

    /**
     * Purpose: Throw an exception when reading happen on a closed file
     * Input: read(buffer, startPosition, endPosition)
     * Expected:
     *          IOException is thrown
     */
    @Test (expected = IOException.class)
    public void readStartEndClosedException() throws IOException {
        ss.close();
        byte[] buff = new byte[100];
        int start = 5;
        int end = 10;

        int n = ss.read(buff, start, end);
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
        int readPosition = text.length - 10;
        byte[] buff = new byte[1];

        ss.moveTo(readPosition);
        ss.open();

        int n = ss.read(buff);
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
        ss.moveTo(-1);
    }

    /**
     * Purpose: Close file after successful reading
     * Input: no
     * Expected:
     *          Stream is closed and reading from the file is unavailable
     */
    @Test
    public void close() {
        ss.close();

        int n = -1;
        try{
            byte[] buff = new byte[1];
            n = ss.read(buff);
        } catch (IOException ignored) {
        }

        assertEquals(-1, n);
    }

    /**
     * Purpose: Get MIME type
     * Input: no
     * Expected:
     *          return "txt"
     */
    @Test
    public void getMimeType() {
        assertEquals("txt", ss.getMimeType());
    }

    /**
     * Purpose: Get length of the text from a file
     * Input: no
     * Expected:
     *          return len
     */
    @Test
    public void length() {
        assertEquals(text.length, ss.length());
    }

    /**
     * Purpose: Get name of a file
     * Input: no
     * Expected:
     *          return "Test.txt"
     */
    @Test
    public void getName() {
        assertEquals(file.getName(), ss.getName());
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
        ss.moveTo(text.length - amount);
        assertEquals(amount, ss.available());
    }

    /**
     * Purpose: Move reading position to the beginning of a file
     * Input: no
     * Expected:
     *          return len
     */
    @Test
    public void reset() throws IOException {
        ss.moveTo(10);
        assertEquals(text.length - 10, ss.available());
        ss.reset();
        assertEquals(text.length, ss.available());
    }

    /**
     * Purpose: Get a file object
     * Input: no
     * Expected:
     *          return SmbFile
     */
    @Test
    public void getFile() {
        assertEquals(file, ss.getFile());
    }

    /**
     * Purpose: Get size of a buffer. The buffer size is predefined in StreamSource class
     * Input: no
     * Expected:
     *          return 1024*60
     */
    @Test
    public void getBufferSize() {
        assertEquals(1024*60, ss.getBufferSize());
    }
}
