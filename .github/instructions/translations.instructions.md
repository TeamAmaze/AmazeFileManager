---
applyTo: "./app/src/*/res/values*/strings.xml"
---
# Github Copilot guidelines for translation reviews

This file provides guidance for LLMs to review Amaze File Manager's pull
requests that contain translation files. 

The original files are in English, and translations should follow the original
intent, but translators have much leeway in how to express the original intent.

## Formatting

All translations should be formated in the same way as the original. One
particular case is translations enclosed in Character Data (starting with
"<![CDATA[" and ending with "]]>"), the translations should keep the
character data markers, as it is needed for Android to parse the string
correctly.