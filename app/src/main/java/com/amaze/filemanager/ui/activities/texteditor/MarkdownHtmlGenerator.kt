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

import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

/**
 * Utility for Markdown preview in the text editor.
 * Pure functions with no Android dependencies — easy to unit-test.
 */
object MarkdownHtmlGenerator {
    /**
     * Returns `true` if [fileName] ends with `.md` or `.markdown` (case-insensitive).
     */
    @JvmStatic
    fun isMarkdownFile(fileName: String?): Boolean {
        if (fileName == null) return false
        val lower = fileName.lowercase()
        return lower.endsWith(".md") || lower.endsWith(".markdown")
    }

    /**
     * Parse Markdown source text and return the rendered HTML body fragment.
     */
    @JvmStatic
    fun renderToHtml(markdownSource: String): String {
        val extensions = listOf(TablesExtension.create(), StrikethroughExtension.create())
        val parser = Parser.builder().extensions(extensions).build()
        val document = parser.parse(markdownSource)
        val renderer = HtmlRenderer.builder().extensions(extensions).build()
        return renderer.render(document)
    }

    /**
     * Wrap an HTML body fragment with a full HTML document including theme-aware CSS.
     *
     * FIXME: Move template to strings.xml
     * FIXME: Use Android/Material native Color constants for easy maintenance
     *
     * @param bodyHtml the rendered Markdown HTML fragment
     * @param isDarkTheme true for dark/black theme, false for light theme
     * @return a complete HTML document string
     */
    @JvmStatic
    fun wrapWithBaseHtml(
        bodyHtml: String,
        isDarkTheme: Boolean,
    ): String {
        val bgColor = if (isDarkTheme) "#1a1a1a" else "#ffffff"
        val textColor = if (isDarkTheme) "#e0e0e0" else "#212121"
        val linkColor = if (isDarkTheme) "#82b1ff" else "#1565c0"
        val codeBg = if (isDarkTheme) "#2d2d2d" else "#f5f5f5"
        val borderColor = if (isDarkTheme) "#444444" else "#dddddd"
        val quoteColor = if (isDarkTheme) "#aaaaaa" else "#666666"

        return """<!DOCTYPE html>
<html><head><meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
body { font-family: sans-serif; padding: 16px; margin: 0; background-color: $bgColor; color: $textColor; line-height: 1.6; word-wrap: break-word; }
a { color: $linkColor; }
pre { background-color: $codeBg; padding: 12px; border-radius: 4px; overflow-x: auto; }
code { background-color: $codeBg; padding: 2px 4px; border-radius: 2px; font-size: 90%; }
pre code { padding: 0; background: none; }
blockquote { border-left: 4px solid $borderColor; margin: 0; padding: 0 16px; color: $quoteColor; }
table { border-collapse: collapse; width: 100%; }
th, td { border: 1px solid $borderColor; padding: 8px; text-align: left; }
th { background-color: $codeBg; }
img { max-width: 100%; height: auto; }
hr { border: none; border-top: 1px solid $borderColor; }
h1, h2, h3, h4, h5, h6 { margin-top: 24px; margin-bottom: 16px; }
</style></head><body>
$bodyHtml
</body></html>"""
    }
}
