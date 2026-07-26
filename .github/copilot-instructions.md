# Github Copilot guidelines for PR reviews

This file provides guidance for LLMs to review Amaze File Manager's pull
requests.

## Specific instructions

In this section are the specific instructions for special cases.

### try-catch structure

When writing a try-catch, in most cases, the catch should contain a specific
`Exception` subclass, meant to be caught at that point. The `Exception` caught
should be thrown by one of the functions called in the try clause. In some
cases, the catch has to be generic, for example when the catch has to prevent
`Exception`s from crashing a thread.

When catching an `OutOfMemoryError` the catch clause should make careful use
of memory, and should not instantiate new objects. For more information see
https://stackoverflow.com/q/24510188.



