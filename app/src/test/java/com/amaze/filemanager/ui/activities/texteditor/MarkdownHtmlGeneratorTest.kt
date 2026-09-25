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

package com.amaze.filemanager.ui.activities.texteditor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [MarkdownHtmlGenerator].
 */
@Suppress("StringLiteralDuplication")
class MarkdownHtmlGeneratorTest {
    /**
     * Tests isMarkdownFile
     */
    @Test
    fun testIsMarkdownFile_md() {
        assertTrue(MarkdownHtmlGenerator.isMarkdownFile("README.md"))
        assertTrue(MarkdownHtmlGenerator.isMarkdownFile("notes.markdown"))
        assertTrue(MarkdownHtmlGenerator.isMarkdownFile("CHANGELOG.MD"))
        assertTrue(MarkdownHtmlGenerator.isMarkdownFile("readme.Markdown"))
        assertFalse(MarkdownHtmlGenerator.isMarkdownFile("file.txt"))
        assertFalse(MarkdownHtmlGenerator.isMarkdownFile("Main.java"))
        assertFalse(MarkdownHtmlGenerator.isMarkdownFile("file.md.bak"))
        assertFalse(MarkdownHtmlGenerator.isMarkdownFile(null))
        assertFalse(MarkdownHtmlGenerator.isMarkdownFile(""))
        assertTrue(MarkdownHtmlGenerator.isMarkdownFile(".md"))
        assertFalse(MarkdownHtmlGenerator.isMarkdownFile("README"))
    }

    /**
     * Tests renderToHtml with styles and formatting
     */
    @Test
    fun testRenderToHtmlStyles() {
        var html = MarkdownHtmlGenerator.renderToHtml("# Hello")
        assertTrue("Should contain <h1>", html.contains("<h1>Hello</h1>"))
        html = MarkdownHtmlGenerator.renderToHtml("Some text")
        assertTrue("Should contain <p>", html.contains("<p>Some text</p>"))
        html = MarkdownHtmlGenerator.renderToHtml("**bold**")
        assertTrue("Should contain <strong>", html.contains("<strong>bold</strong>"))
        html = MarkdownHtmlGenerator.renderToHtml("*italic*")
        assertTrue("Should contain <em>", html.contains("<em>italic</em>"))
    }

    /**
     * Tests renderToHtml with lists
     */
    @Test
    fun testRenderUnorderedList() {
        var md = "- item1\n- item2\n- item3"
        var html = MarkdownHtmlGenerator.renderToHtml(md)
        assertTrue("Should contain <ul>", html.contains("<ul>"))
        assertTrue("Should contain <li>", html.contains("<li>item1</li>"))
        assertTrue("Should contain <li>", html.contains("<li>item2</li>"))

        md = "1. first\n2. second"
        html = MarkdownHtmlGenerator.renderToHtml(md)
        assertTrue("Should contain <ol>", html.contains("<ol>"))
        assertTrue("Should contain <li>", html.contains("<li>first</li>"))
    }

    /**
     * Tests renderToHtml with links
     */
    @Test
    fun testRenderLink() {
        val html = MarkdownHtmlGenerator.renderToHtml("[click](https://example.com)")
        assertTrue("Should contain <a>", html.contains("<a"))
        assertTrue("Should contain href", html.contains("href=\"https://example.com\""))
        assertTrue("Should contain link text", html.contains(">click</a>"))
    }

    /**
     * Test renderToHtml with inline code and code blocks
     */
    @Test
    fun testRenderInlineCode() {
        var html = MarkdownHtmlGenerator.renderToHtml("Use `println()`")
        assertTrue("Should contain <code>", html.contains("<code>println()</code>"))

        val md = "```\nval x = 1\n```"
        html = MarkdownHtmlGenerator.renderToHtml(md)
        assertTrue("Should contain <pre>", html.contains("<pre>"))
        assertTrue("Should contain <code>", html.contains("<code>"))
        assertTrue("Should contain code content", html.contains("val x = 1"))
    }

    /**
     * Test renderToHtml with blockquotes
     */
    @Test
    fun testRenderBlockquote() {
        val html = MarkdownHtmlGenerator.renderToHtml("> quote text")
        assertTrue("Should contain <blockquote>", html.contains("<blockquote>"))
        assertTrue("Should contain quote text", html.contains("quote text"))
    }

    /**
     * Test renderToHtml with horizontal rule
     */
    @Test
    fun testRenderHorizontalRule() {
        val html = MarkdownHtmlGenerator.renderToHtml("---")
        assertTrue("Should contain <hr>", html.contains("<hr"))
    }

    /**
     * Test renderToHtml with empty string
     */
    @Test
    fun testRenderEmptyString() {
        val html = MarkdownHtmlGenerator.renderToHtml("")
        // Empty input should produce empty or whitespace-only output
        assertTrue("Should be empty or whitespace", html.trim().isEmpty())
    }

    /**
     * Tests renderToHtml with multiple headings
     */
    @Test
    fun testRenderMultipleHeadings() {
        val md = "# H1\n## H2\n### H3"
        val html = MarkdownHtmlGenerator.renderToHtml(md)
        assertTrue(html.contains("<h1>H1</h1>"))
        assertTrue(html.contains("<h2>H2</h2>"))
        assertTrue(html.contains("<h3>H3</h3>"))
    }

    /**
     * Test renderToHtml with image tags
     */
    @Test
    fun testRenderImage() {
        val html = MarkdownHtmlGenerator.renderToHtml("![alt](image.png)")
        assertTrue("Should contain <img>", html.contains("<img"))
        assertTrue("Should contain src", html.contains("src=\"image.png\""))
        assertTrue("Should contain alt", html.contains("alt=\"alt\""))
    }

    /**
     * Test wrapWithBaseHtml
     */
    @Test
    fun testWrapLightThemeContainsDoctype() {
        var result = MarkdownHtmlGenerator.wrapWithBaseHtml("<p>Hello</p>", isDarkTheme = false)
        assertTrue("Should start with DOCTYPE", result.contains("<!DOCTYPE html>"))
        result = MarkdownHtmlGenerator.wrapWithBaseHtml("<p>Hello</p>", isDarkTheme = false)
        assertTrue("Should contain body content", result.contains("<p>Hello</p>"))
    }

    /**
     * Test wrapWithBaseHtml applies correct colors for light and dark themes
     */
    @Test
    fun testWrapCorrectThemeColors() {
        var result = MarkdownHtmlGenerator.wrapWithBaseHtml("<p>Test</p>", isDarkTheme = false)
        assertTrue("Light bg should be #ffffff", result.contains("#ffffff"))
        assertTrue("Light text should be #212121", result.contains("#212121"))
        assertTrue("Light link should be #1565c0", result.contains("#1565c0"))
        assertTrue("Light code bg should be #f5f5f5", result.contains("#f5f5f5"))
        assertTrue("Light border should be #dddddd", result.contains("#dddddd"))

        result = MarkdownHtmlGenerator.wrapWithBaseHtml("<p>Test</p>", isDarkTheme = true)
        assertTrue("Dark bg should be #1a1a1a", result.contains("#1a1a1a"))
        assertTrue("Dark text should be #e0e0e0", result.contains("#e0e0e0"))
        assertTrue("Dark link should be #82b1ff", result.contains("#82b1ff"))
        assertTrue("Dark code bg should be #2d2d2d", result.contains("#2d2d2d"))
        assertTrue("Dark border should be #444444", result.contains("#444444"))
    }

    /**
     * Test wrapWithBaseHtml contains correct meta tags and CSS rules
     */
    @Test
    fun testWrapContainsMeta() {
        var result = MarkdownHtmlGenerator.wrapWithBaseHtml("<p>X</p>", isDarkTheme = false)
        assertTrue("Should contain viewport meta", result.contains("viewport"))
        assertTrue("Should contain width=device-width", result.contains("width=device-width"))

        result = MarkdownHtmlGenerator.wrapWithBaseHtml("<p>X</p>", isDarkTheme = false)
        assertTrue("Should contain charset UTF-8", result.contains("charset=\"UTF-8\""))

        result = MarkdownHtmlGenerator.wrapWithBaseHtml("<p>X</p>", isDarkTheme = false)
        assertTrue("Should contain style block", result.contains("<style>"))
        assertTrue("Should style body", result.contains("body {"))
        assertTrue("Should style pre", result.contains("pre {"))
        assertTrue("Should style code", result.contains("code {"))
        assertTrue("Should style blockquote", result.contains("blockquote {"))
        assertTrue("Should style table", result.contains("table {"))
        assertTrue("Should style headings", result.contains("h1, h2, h3"))
    }

    /**
     * Test wrapWithBaseHtml with empty body content
     */
    @Test
    fun testWrapEmptyBody() {
        val result = MarkdownHtmlGenerator.wrapWithBaseHtml("", isDarkTheme = false)
        assertTrue("Should still produce valid HTML", result.contains("<!DOCTYPE html>"))
        assertTrue("Should have body tags", result.contains("<body>"))
        assertTrue("Should close body", result.contains("</body>"))
    }

    /**
     * Test wrapWithBaseHtml preserves HTML entities in the body content
     */
    @Test
    fun testWrapPreservesHtmlEntities() {
        val body = "<p>5 &gt; 3 &amp; 2 &lt; 4</p>"
        val result = MarkdownHtmlGenerator.wrapWithBaseHtml(body, isDarkTheme = false)
        assertTrue("Should preserve HTML entities", result.contains("5 &gt; 3 &amp; 2 &lt; 4"))
    }

    /**
     * Test end to end rendering and wrapping
     */
    @Test
    fun testEndToEndRenderAndWrap() {
        val md = "# Title\n\nSome **bold** text."
        val bodyHtml = MarkdownHtmlGenerator.renderToHtml(md)
        val fullHtml = MarkdownHtmlGenerator.wrapWithBaseHtml(bodyHtml, isDarkTheme = false)

        assertTrue("Full doc should contain DOCTYPE", fullHtml.contains("<!DOCTYPE html>"))
        assertTrue("Full doc should contain h1", fullHtml.contains("<h1>Title</h1>"))
        assertTrue("Full doc should contain strong", fullHtml.contains("<strong>bold</strong>"))
        assertTrue("Full doc should close html", fullHtml.contains("</html>"))
    }

    /**
     * Test end to end rendering with dark theme
     */
    @Test
    fun testEndToEndDarkTheme() {
        val md = "Hello *world*"
        val bodyHtml = MarkdownHtmlGenerator.renderToHtml(md)
        val fullHtml = MarkdownHtmlGenerator.wrapWithBaseHtml(bodyHtml, isDarkTheme = true)

        assertTrue("Should use dark bg", fullHtml.contains("#1a1a1a"))
        assertTrue("Should contain em", fullHtml.contains("<em>world</em>"))
    }
}
