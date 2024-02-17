/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.asynchronous.asynctasks.movecopy

import android.app.ProgressDialog
import android.content.Intent
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.widget.AppCompatCheckBox
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.asynchronous.asynctasks.fromTask
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil
import com.amaze.filemanager.asynchronous.services.CopyService
import com.amaze.filemanager.databinding.CopyDialogBinding
import com.amaze.filemanager.fileoperations.filesystem.CAN_CREATE_FILES
import com.amaze.filemanager.fileoperations.filesystem.COPY
import com.amaze.filemanager.fileoperations.filesystem.FolderState
import com.amaze.filemanager.fileoperations.filesystem.MOVE
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.FilenameHelper
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.MakeDirectoryOperation
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.utils.OnFileFound
import com.amaze.filemanager.utils.Utils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.LinkedList

/**
 * This helper class works by checking the conflicts during paste operation. After checking
 * conflicts [MaterialDialog] is shown to user for each conflicting file. If the conflicting file
 * is a directory, the conflicts are resolved by inserting a node in [CopyNode] tree and then doing
 * BFS on this tree.
 */
class PreparePasteTask(strongRefMain: MainActivity) {

    private lateinit var targetPath: String
    private var isMove = false
    private var isRootMode = false
    private lateinit var openMode: OpenMode
    private lateinit var filesToCopy: MutableList<HybridFileParcelable>

    private val pathsList = ArrayList<String>()
    private val filesToCopyPerFolder = ArrayList<ArrayList<HybridFileParcelable>>()

    private val context = WeakReference(strongRefMain)

    @Suppress("DEPRECATION")
    private var progressDialog: ProgressDialog? = null
    private val coroutineScope = CoroutineScope(Job() + Dispatchers.Default)

    private lateinit var destination: HybridFile
    private val conflictingFiles: MutableList<HybridFileParcelable> = mutableListOf()
    private val conflictingDirActionMap = HashMap<HybridFileParcelable, String>()

    private var skipAll = false
    private var renameAll = false
    private var overwriteAll = false

    private fun startService(
        sourceFiles: ArrayList<HybridFileParcelable>,
        target: String,
        openMode: OpenMode,
        isMove: Boolean,
        isRootMode: Boolean
    ) {
        val intent = Intent(context.get(), CopyService::class.java)
        intent.putParcelableArrayListExtra(CopyService.TAG_COPY_SOURCES, sourceFiles)
        intent.putExtra(CopyService.TAG_COPY_TARGET, target)
        intent.putExtra(CopyService.TAG_COPY_OPEN_MODE, openMode.ordinal)
        intent.putExtra(CopyService.TAG_COPY_MOVE, isMove)
        intent.putExtra(CopyService.TAG_IS_ROOT_EXPLORER, isRootMode)
        ServiceWatcherUtil.runService(context.get(), intent)
    }

    /**
     * Starts execution of [PreparePasteTask] class.
     */
    fun execute(
        targetPath: String,
        isMove: Boolean,
        isRootMode: Boolean,
        openMode: OpenMode,
        filesToCopy: ArrayList<HybridFileParcelable>
    ) {
        this.targetPath = targetPath
        this.isMove = isMove
        this.isRootMode = isRootMode
        this.openMode = openMode
        this.filesToCopy = filesToCopy

        val isCloudOrRootMode = openMode == OpenMode.OTG ||
            openMode == OpenMode.GDRIVE ||
            openMode == OpenMode.DROPBOX ||
            openMode == OpenMode.BOX ||
            openMode == OpenMode.ONEDRIVE ||
            openMode == OpenMode.ROOT

        if (isCloudOrRootMode) {
            startService(filesToCopy, targetPath, openMode, isMove, isRootMode)
            return
        }

        val totalBytes = FileUtils.getTotalBytes(filesToCopy, context.get())
        destination = HybridFile(openMode, targetPath)
        destination.generateMode(context.get())

        if (filesToCopy.isNotEmpty() &&
            isMove &&
            filesToCopy[0].getParent(context.get()) == targetPath
        ) {
            Toast.makeText(context.get(), R.string.same_dir_move_error, Toast.LENGTH_SHORT).show()
            return
        }

        val isMoveSupported = isMove &&
            destination.mode == openMode &&
            MoveFiles.getOperationSupportedFileSystem().contains(openMode)

        if (destination.usableSpace < totalBytes &&
            !isMoveSupported
        ) {
            Toast.makeText(context.get(), R.string.in_safe, Toast.LENGTH_SHORT).show()
            return
        }
        @Suppress("DEPRECATION")
        progressDialog = ProgressDialog.show(
            context.get(),
            "",
            context.get()?.getString(R.string.checking_conflicts)
        )
        checkConflicts(
            isRootMode,
            filesToCopy,
            destination,
            conflictingFiles,
            conflictingDirActionMap
        )
    }

    private fun checkConflicts(
        isRootMode: Boolean,
        filesToCopy: ArrayList<HybridFileParcelable>,
        destination: HybridFile,
        conflictingFiles: MutableList<HybridFileParcelable>,
        conflictingDirActionMap: HashMap<HybridFileParcelable, String>
    ) {
        coroutineScope.launch {
            destination.forEachChildrenFile(
                context.get(),
                isRootMode,
                object : OnFileFound {
                    override fun onFileFound(file: HybridFileParcelable) {
                        for (fileToCopy in filesToCopy) {
                            if (file.getName(context.get()) == fileToCopy.getName(context.get())) {
                                conflictingFiles.add(fileToCopy)
                            }
                        }
                    }
                }
            )
            withContext(Dispatchers.Main) {
                prepareDialog(conflictingFiles, conflictingDirActionMap)
                @Suppress("DEPRECATION")
                progressDialog?.setMessage(context.get()?.getString(R.string.copying))
            }
            resolveConflict(conflictingFiles, conflictingDirActionMap, filesToCopy)
        }
    }

    private suspend fun prepareDialog(
        conflictingFiles: MutableList<HybridFileParcelable>,
        conflictingDirActionMap: HashMap<HybridFileParcelable, String>
    ) {
        if (conflictingFiles.isEmpty()) return

        val contextRef = context.get() ?: return
        val accentColor = contextRef.accent
        val dialogBuilder = MaterialDialog.Builder(contextRef)
        val copyDialogBinding: CopyDialogBinding =
            CopyDialogBinding.inflate(LayoutInflater.from(contextRef))
        dialogBuilder.customView(copyDialogBinding.root, true)
        val checkBox: AppCompatCheckBox = copyDialogBinding.checkBox

        Utils.setTint(contextRef, checkBox, accentColor)
        dialogBuilder.theme(contextRef.appTheme.getMaterialDialogTheme())
        dialogBuilder.title(contextRef.resources.getString(R.string.paste))
        dialogBuilder.positiveText(R.string.rename)
        dialogBuilder.neutralText(R.string.skip)
        dialogBuilder.positiveColor(accentColor)
        dialogBuilder.negativeColor(accentColor)
        dialogBuilder.neutralColor(accentColor)
        dialogBuilder.negativeText(R.string.overwrite)
        dialogBuilder.cancelable(false)
        showDialog(
            conflictingFiles,
            conflictingDirActionMap,
            copyDialogBinding,
            dialogBuilder,
            checkBox
        )
    }

    private suspend fun showDialog(
        conflictingFiles: MutableList<HybridFileParcelable>,
        conflictingDirActionMap: HashMap<HybridFileParcelable, String>,
        copyDialogBinding: CopyDialogBinding,
        dialogBuilder: MaterialDialog.Builder,
        checkBox: AppCompatCheckBox
    ) {
        val iterator = conflictingFiles.iterator()
        while (iterator.hasNext()) {
            val hybridFileParcelable = iterator.next()
            copyDialogBinding.fileNameText.text = hybridFileParcelable.name
            val dialog = dialogBuilder.build()
            if (hybridFileParcelable.getParent(context.get()) == targetPath) {
                dialog.getActionButton(DialogAction.NEGATIVE)
                    .isEnabled = false
            }
            val resultDeferred = CompletableDeferred<DialogAction>()
            dialogBuilder.onPositive { _, _ ->
                resultDeferred.complete(DialogAction.POSITIVE)
            }
            dialogBuilder.onNegative { _, _ ->
                resultDeferred.complete(DialogAction.NEGATIVE)
            }
            dialogBuilder.onNeutral { _, _ ->
                resultDeferred.complete(DialogAction.NEUTRAL)
            }
            dialog.show()
            when (resultDeferred.await()) {
                DialogAction.POSITIVE -> {
                    if (checkBox.isChecked) {
                        renameAll = true
                        return
                    }
                    conflictingDirActionMap[hybridFileParcelable] = Action.RENAME
                }
                DialogAction.NEGATIVE -> {
                    if (checkBox.isChecked) {
                        overwriteAll = true
                        return
                    }
                    conflictingDirActionMap[hybridFileParcelable] = Action.OVERWRITE
                }
                DialogAction.NEUTRAL -> {
                    if (checkBox.isChecked) {
                        skipAll = true
                        return
                    }
                    conflictingDirActionMap[hybridFileParcelable] = Action.SKIP
                }
            }
            iterator.remove()
        }
    }

    private fun resolveConflict(
        conflictingFiles: MutableList<HybridFileParcelable>,
        conflictingDirActionMap: HashMap<HybridFileParcelable, String>,
        filesToCopy: ArrayList<HybridFileParcelable>
    ) = coroutineScope.launch {
        var index = conflictingFiles.size - 1
        if (renameAll) {
            while (conflictingFiles.isNotEmpty()) {
                conflictingDirActionMap[conflictingFiles[index]] = Action.RENAME
                conflictingFiles.removeAt(index)
                index--
            }
        } else if (overwriteAll) {
            while (conflictingFiles.isNotEmpty()) {
                conflictingDirActionMap[conflictingFiles[index]] = Action.OVERWRITE
                conflictingFiles.removeAt(index)
                index--
            }
        } else if (skipAll) {
            while (conflictingFiles.isNotEmpty()) {
                filesToCopy.remove(conflictingFiles.removeAt(index))
                index--
            }
        }

        val rootNode = CopyNode(targetPath, ArrayList(filesToCopy))
        var currentNode: CopyNode? = rootNode.startCopy()

        while (currentNode != null) {
            pathsList.add(currentNode.path)
            filesToCopyPerFolder.add(currentNode.filesToCopy)
            currentNode = rootNode.goToNextNode()
        }
        finishCopying()
    }

    private suspend fun finishCopying() {
        var index = 0
        while (index < filesToCopyPerFolder.size) {
            if (filesToCopyPerFolder[index].size == 0) {
                filesToCopyPerFolder.removeAt(index)
                pathsList.removeAt(index)
                index--
            }
            index++
        }
        if (filesToCopyPerFolder.isNotEmpty()) {
            @FolderState
            val mode: Int = context.get()?.mainActivityHelper!!
                .checkFolder(targetPath, openMode, context.get())
            if (mode == CAN_CREATE_FILES && !targetPath.contains("otg:/")) {
                // This is used because in newer devices the user has to accept a permission,
                // see MainActivity.onActivityResult()
                context.get()?.oparrayListList = filesToCopyPerFolder
                context.get()?.oparrayList = null
                context.get()?.operation = if (isMove) MOVE else COPY
                context.get()?.oppatheList = pathsList
            } else {
                if (!isMove) {
                    for (foldersIndex in filesToCopyPerFolder.indices)
                        startService(
                            filesToCopyPerFolder[foldersIndex],
                            pathsList[foldersIndex],
                            openMode,
                            isMove,
                            isRootMode
                        )
                } else {
                    fromTask(
                        MoveFilesTask(
                            filesToCopyPerFolder,
                            isRootMode,
                            targetPath,
                            context.get()!!,
                            openMode,
                            pathsList
                        )
                    )
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context.get(),
                    context.get()!!.resources.getString(R.string.no_file_overwrite),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        withContext(Dispatchers.Main) {
            progressDialog?.dismiss()
        }
        coroutineScope.cancel()
    }

    private inner class CopyNode(
        val path: String,
        val filesToCopy: ArrayList<HybridFileParcelable>
    ) {
        private val nextNodes: MutableList<CopyNode> = mutableListOf()
        private var queue: LinkedList<CopyNode>? = null
        private var visited: HashSet<CopyNode>? = null

        init {
            val iterator = filesToCopy.iterator()
            while (iterator.hasNext()) {
                val hybridFileParcelable = iterator.next()
                if (conflictingDirActionMap.contains(hybridFileParcelable)) {
                    val fileAtTarget = HybridFile(
                        hybridFileParcelable.mode,
                        path,
                        hybridFileParcelable.name,
                        hybridFileParcelable.isDirectory
                    )
                    when (conflictingDirActionMap[hybridFileParcelable]) {
                        Action.RENAME -> {
                            if (hybridFileParcelable.isDirectory) {
                                val newName =
                                    FilenameHelper.increment(fileAtTarget).getName(context.get())
                                val newPath = "$path/$newName"
                                val newDir = HybridFile(hybridFileParcelable.mode, newPath)
                                MakeDirectoryOperation.mkdirs(context.get()!!, newDir)
                                @Suppress("DEPRECATION")
                                nextNodes.add(
                                    CopyNode(
                                        newPath,
                                        hybridFileParcelable.listFiles(context.get(), isRootMode)
                                    )
                                )
                                iterator.remove()
                            } else {
                                filesToCopy[filesToCopy.indexOf(hybridFileParcelable)].name =
                                    FilenameHelper.increment(
                                        fileAtTarget
                                    ).getName(context.get())
                            }
                        }

                        Action.SKIP -> iterator.remove()
                    }
                }
            }
        }

        /**
         * Starts BFS traversal of tree.
         *
         * @return Root node
         */
        fun startCopy(): CopyNode {
            queue = LinkedList()
            visited = HashSet()
            queue!!.add(this)
            visited!!.add(this)
            return this
        }

        /**
         * Moves to the next unvisited node in tree.
         *
         * @return The next unvisited node if available, otherwise returns null.
         */
        fun goToNextNode(): CopyNode? =
            if (queue.isNullOrEmpty()) null
            else {
                val node = queue!!.element()
                val child = getUnvisitedChildNode(visited!!, node)
                if (child != null) {
                    visited!!.add(child)
                    queue!!.add(child)
                    child
                } else {
                    queue!!.remove()
                    goToNextNode()
                }
            }

        private fun getUnvisitedChildNode(
            visited: Set<CopyNode>,
            node: CopyNode
        ): CopyNode? {
            for (currentNode in node.nextNodes) {
                if (!visited.contains(currentNode)) {
                    return currentNode
                }
            }
            return null
        }
    }

    private class Action {
        companion object {
            const val SKIP = "skip"
            const val RENAME = "rename"
            const val OVERWRITE = "overwrite"
        }
    }
}
