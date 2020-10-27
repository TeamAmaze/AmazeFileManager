package com.amaze.filemanager.filesystem.root.api

interface IFindFileCommand: IRootCommand {

    /**
     * find file at given path in root
     *
     * @return boolean whether file was deleted or not
     */
    fun findFile(path: String): Boolean
}