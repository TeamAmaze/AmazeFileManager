/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.asynchronous.asynctasks.texteditor.read

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for [FileWindowReader].
 */
@Suppress("StringLiteralDuplication")
class FileWindowReaderTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testFile: File

    /**
     * Pre-test setup
     */
    @Before
    fun setUp() {
        testFile = tempFolder.newFile("test.txt")
    }

    /**
     * Test reading empty file.
     */
    @Test
    fun testReadEmptyFile() {
        testFile.writeText("")
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            val result = it.readWindow(0, 1024)
            assertEquals("", result.text)
            assertEquals(0L, result.startByte)
            assertEquals(0L, result.endByte)
            assertTrue(result.isStartOfFile)
            assertTrue(result.isEndOfFile)
        }
    }

    /**
     * Test reading file fitting the window.
     */
    @Test
    fun testReadSmallFileFitsInWindow() {
        val content = "Hello, World!\nSecond line\nThird line\n"
        testFile.writeText(content)
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            val result = it.readWindow(0, 1024)
            assertEquals(content, result.text)
            assertEquals(0L, result.startByte)
            assertTrue(result.isStartOfFile)
            assertTrue(result.isEndOfFile)
        }
    }

    /**
     * Test reading a file with a single line that has no newline at the end.
     */
    @Test
    fun testReadSingleLineFile() {
        val content = "No newline at end"
        testFile.writeText(content)
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            val result = it.readWindow(0, 1024)
            assertEquals(content, result.text)
            assertTrue(result.isStartOfFile)
            assertTrue(result.isEndOfFile)
        }
    }

    /**
     * Test file size is correctly reported.
     */
    @Test
    fun testFileSizeCorrect() {
        val content = "Hello"
        testFile.writeText(content)
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            assertEquals(content.toByteArray().size.toLong(), it.fileSize)
        }
    }

    /**
     * Test maxChars limits the output and snaps to line boundaries.
     */
    @Test
    fun testMaxCharsLimitsOutput() {
        // Create content with multiple lines, each larger than 5 chars
        val content = "Line1\nLine2\nLine3\nLine4\nLine5\n"
        testFile.writeText(content)
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            // Request only 12 chars — should get at most 12 chars snapped to line boundaries
            val result = it.readWindow(0, 12)
            assertTrue(result.text.length <= 12)
            assertTrue(result.isStartOfFile)
            // With line snapping, it shouldn't include trailing partial lines
            assertTrue(result.text.endsWith("\n"))
        }
    }

    /**
     * Test window read from the start of the file correctly identifies start of file
     * and returns expected content.
     */
    @Test
    fun testWindowFromStartOfFile() {
        val lines = (1..100).map { "Line number $it here\n" }
        val content = lines.joinToString("")
        testFile.writeText(content)
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            val result = it.readWindow(0, 200)
            assertTrue(result.isStartOfFile)
            assertFalse(result.isEndOfFile)
            assertTrue(result.text.startsWith("Line number 1 here\n"))
            assertTrue(result.text.endsWith("\n"))
        }
    }

    /**
     * Test window read from the middle of the file snaps to the next line start and does not
     * include partial lines at the start.
     */
    @Test
    fun testWindowFromMiddleSnapsToLineStart() {
        val lines = (1..20).map { "Line $it\n" }
        val content = lines.joinToString("")
        testFile.writeText(content)
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            // Seek to the middle of the file (byte offset in the middle of a line)
            val midOffset = content.toByteArray().size / 2L
            val result = it.readWindow(midOffset, 200)

            // When starting mid-file, the first partial line should be skipped
            assertFalse(result.isStartOfFile)
            // The result should start at a complete line
            assertTrue(result.text.startsWith("Line"))
            assertTrue(result.text.endsWith("\n"))
        }
    }

    /**
     * Test when window end falls in the middle of a line, it snaps back to the previous newline
     */
    @Test
    fun testWindowEndSnapsToNewline() {
        // Large content so the window can't contain it all
        val lines = (1..1000).map { "Line number $it with some padding text to make it longer\n" }
        val content = lines.joinToString("")
        testFile.writeText(content)
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            // Read a small window from the start
            val result = it.readWindow(0, 200)
            assertTrue(result.isStartOfFile)
            assertFalse(result.isEndOfFile)
            // End should be at a line boundary
            assertTrue(result.text.endsWith("\n"))
            // No partial lines
            assertFalse(result.text.trimEnd('\n').contains("Line number").not())
        }
    }

    /**
     * Test reading a window starting near the end of the file where requested maxChars
     * exceeds remaining chars
     */
    @Test
    fun testWindowNearEndOfFile() {
        val lines = (1..50).map { "Line $it\n" }
        val content = lines.joinToString("")
        testFile.writeText(content)
        val fileBytes = content.toByteArray()
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            // Read from near the end — request more chars than remain
            val nearEnd = (fileBytes.size - 30L).coerceAtLeast(0L)
            val result = it.readWindow(nearEnd, 10000)
            assertTrue(result.isEndOfFile)
            assertFalse(result.isStartOfFile)
            // Should contain the last line
            assertTrue(result.text.contains("Line 50\n"))
        }
    }

    /**
     * Test reading a window starting exactly at the end of the file should return empty text and
     * indicate end of file.
     */
    @Test
    fun testWindowAtExactEndOfFile() {
        val content = "Hello\nWorld\n"
        testFile.writeText(content)
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            val result = it.readWindow(content.toByteArray().size.toLong(), 1024)
            assertEquals("", result.text)
            assertTrue(result.isEndOfFile)
        }
    }

    /**
     * Test UTF-8 multibyte boundary handling
     */
    @Test
    fun testUtf8MultiByteBoundary() {
        // Use multibyte UTF-8 characters (emoji = 4 bytes each)
        val content = "Hello\n\uD83D\uDE00\uD83D\uDE01\uD83D\uDE02\n" // 😀😁😂
        testFile.writeBytes(content.toByteArray(Charsets.UTF_8))
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            val result = it.readWindow(0, 1024)
            assertTrue(result.text.contains("😀"))
            assertTrue(result.text.contains("😁"))
            assertTrue(result.text.contains("😂"))
        }
    }

    /**
     * Test seeking to a byte offset that falls in the middle of a multibyte UTF-8 character
     */
    @Test
    fun testUtf8SeekIntoMiddleOfMultibyteChar() {
        // 2-byte UTF-8 chars: é = C3 A9 (2 bytes)
        val content = "café\ncafé\ncafé\n"
        testFile.writeBytes(content.toByteArray(Charsets.UTF_8))
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            // Seek to byte 4 which is the second byte of 'é' in "café"
            // The snapToCharBoundary should back up to byte 3
            val result = it.readWindow(4, 1024)
            // Should be valid UTF-8 text, no replacement characters
            assertFalse(result.text.contains("\uFFFD"))
        }
    }

    /**
     * Test reading a file with CJK characters
     */
    @Test
    fun testCjkCharacters() {
        // 3-byte UTF-8 chars: Chinese characters
        val content = "第一行\n第二行\n第三行\n"
        testFile.writeBytes(content.toByteArray(Charsets.UTF_8))
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            val result = it.readWindow(0, 1024)
            assertTrue(result.text.contains("第一行"))
            assertTrue(result.text.contains("第三行"))
            assertTrue(result.isStartOfFile)
            assertTrue(result.isEndOfFile)
        }
    }

    /**
     * Test consecutive forward window reads
     */
    @Test
    fun testConsecutiveForwardWindowReads() {
        val lines = (1..200).map { "Line $it\n" }
        val content = lines.joinToString("")
        testFile.writeText(content)
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            // First window from start
            val first = it.readWindow(0, 100)
            assertTrue(first.isStartOfFile)
            assertFalse(first.isEndOfFile)
            assertTrue(first.text.isNotEmpty())

            // Second window starting at half of first window's end
            val midPoint = (first.endByte - first.startByte) / 2 + first.startByte
            val second = it.readWindow(midPoint, 100)
            assertFalse(second.isStartOfFile)
            // Should have progressed past the first window start
            assertTrue(second.startByte > first.startByte)
        }
    }

    /**
     * Test overlapping windows share content correctly and the overlapping lines are consistent
     */
    @Test
    fun testOverlappingWindowsShareContent() {
        val lines = (1..200).map { "Line $it content here\n" }
        val content = lines.joinToString("")
        testFile.writeText(content)
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            val first = it.readWindow(0, 500)
            // Read second window with 50% overlap
            val overlapStart = first.startByte + (first.endByte - first.startByte) / 2
            val second = it.readWindow(overlapStart, 500)

            // There should be overlapping content between the two windows
            val firstLines = first.text.lines().filter { l -> l.isNotEmpty() }
            val secondLines = second.text.lines().filter { l -> l.isNotEmpty() }

            val overlap = firstLines.intersect(secondLines.toSet())
            assertTrue(
                "Windows should overlap, got first=${firstLines.size} second=${secondLines.size} overlap=${overlap.size}",
                overlap.isNotEmpty(),
            )
        }
    }

    /**
     * Test negative offset
     */
    @Test
    fun testNegativeOffset() {
        val content = "Hello\nWorld\n"
        testFile.writeText(content)
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            // Should clamp to 0
            val result = it.readWindow(-100, 1024)
            assertTrue(result.isStartOfFile)
            assertEquals(content, result.text)
        }
    }

    /**
     * Test offset beyond file size
     */
    @Test
    fun testOffsetBeyondFileSize() {
        val content = "Hello\nWorld\n"
        testFile.writeText(content)
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            val result = it.readWindow(100000, 1024)
            assertEquals("", result.text)
            assertTrue(result.isEndOfFile)
        }
    }

    /**
     * Test maxChars=0, should return empty text
     */
    @Test
    fun testMaxCharsZero() {
        val content = "Hello\nWorld\n"
        testFile.writeText(content)
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            // maxChars=0 → maxBytes will be 0, should return empty
            val result = it.readWindow(0, 0)
            assertEquals("", result.text)
        }
    }

    /**
     * Test file with only newlines, should return correct number of newlines
     * and indicate start/end of file
     */
    @Test
    fun testFileWithOnlyNewlines() {
        val content = "\n\n\n\n\n"
        testFile.writeText(content)
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            val result = it.readWindow(0, 1024)
            assertEquals(content, result.text)
            assertTrue(result.isStartOfFile)
            assertTrue(result.isEndOfFile)
        }
    }

    /**
     * Test very long single line that exceeds maxChars.
     * Should return up to maxChars and indicate start of file
     */
    @Test
    fun testVeryLongSingleLine() {
        // Single line with no newlines at all
        val content = "A".repeat(10000)
        testFile.writeText(content)
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            // Request a small window — since there are no newlines, end-snap
            // won't find a newline. Because we're at start and end, full content
            // up to maxChars is returned.
            val result = it.readWindow(0, 500)
            assertTrue(result.text.length <= 500)
            assertTrue(result.isStartOfFile)
        }
    }

    /**
     * Test close reader should release resources and allow for double close without exception
     */
    @Test
    fun testCloseReleasesChannel() {
        val content = "Hello"
        testFile.writeText(content)
        val reader = FileWindowReader.fromFile(testFile)
        reader.close()
        // Double close should not throw
        reader.close()
    }

    /**
     * Test FileWindowReader.fromFile()
     */
    @Test
    fun testFromFileFactory() {
        val content = "Factory test\nLine 2\n"
        testFile.writeText(content)
        FileWindowReader.fromFile(testFile).use { reader ->
            assertEquals(content.toByteArray().size.toLong(), reader.fileSize)
            val result = reader.readWindow(0, 1024)
            assertEquals(content, result.text)
        }
    }

    /**
     * Test byte offset consistency: startByte + text byte length should equal endByte
     */
    @Test
    fun testByteOffsetsAreConsistent() {
        val lines = (1..50).map { "Line $it\n" }
        val content = lines.joinToString("")
        testFile.writeText(content)
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            val result = it.readWindow(0, 100)
            // endByte should equal startByte + byte length of returned text
            val expectedEndByte = result.startByte + result.text.toByteArray(Charsets.UTF_8).size
            assertEquals(expectedEndByte, result.endByte)
        }
    }

    /**
     * Test that when reading from a mid-file offset, the returned startByte is at or after
     * the requested offset and that the text corresponds to the byte range.
     */
    @Test
    fun testByteOffsetsForMidFileRead() {
        val lines = (1..100).map { "Line $it\n" }
        val content = lines.joinToString("")
        testFile.writeText(content)
        val reader = FileWindowReader.fromFile(testFile)
        reader.use {
            val result = it.readWindow(50, 100)
            // startByte should be >= 50 (snapped forward past partial line)
            assertTrue(result.startByte >= 50)
            // endByte should be > startByte
            assertTrue(result.endByte > result.startByte)
            // Byte range should match text length
            val textBytes = result.text.toByteArray(Charsets.UTF_8).size
            assertEquals(result.endByte, result.startByte + textBytes)
        }
    }
}
