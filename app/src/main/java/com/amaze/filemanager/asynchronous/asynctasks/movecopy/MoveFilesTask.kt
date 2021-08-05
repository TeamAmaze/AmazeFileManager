package com.amaze.filemanager.asynchronous.asynctasks.movecopy

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.Task
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil
import com.amaze.filemanager.asynchronous.services.CopyService
import com.amaze.filemanager.database.CryptHandler
import com.amaze.filemanager.database.models.explorer.EncryptedEntry
import com.amaze.filemanager.file_operations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.files.CryptUtil
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.ui.activities.MainActivity
import java.util.*

data class MoveFilesReturn(
  val movedCorrectly: Boolean,
  val invalidOperation: Boolean,
  val destinationSize: Long,
  val totalSize: Long
  )

class MoveFilesTask(
  val files: ArrayList<ArrayList<HybridFileParcelable>>,
  val isRootExplorer: Boolean,
  val currentPath: String,
  context: Context,
  val mode: OpenMode,
  val paths: ArrayList<String>
): Task<MoveFilesReturn, MoveFiles> {

  companion object {
    private val TAG = MoveFilesTask::class.java.simpleName
  }

  private val task: MoveFiles = MoveFiles(files, isRootExplorer, context, mode, paths)
  private val applicationContext: Context = context.applicationContext

  override fun getTask(): MoveFiles = task

  override fun onError(error: Throwable) {
    Log.e(TAG, "Unexpected error on file move: ", error)
  }

  override fun onFinish(value: MoveFilesReturn) {
    val (movedCorrectly, invalidOperation, destinationSize, totalBytes) = value

    if (movedCorrectly) {
      if (currentPath == paths[0]) {
        // mainFrag.updateList();
        val intent = Intent(MainActivity.KEY_INTENT_LOAD_LIST)
        intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, paths[0])
        applicationContext.sendBroadcast(intent)
      }

      if (invalidOperation) {
        Toast.makeText(
          applicationContext,
          R.string.some_files_failed_invalid_operation,
          Toast.LENGTH_LONG
        )
          .show()
      }

      for (i in paths.indices) {
        val targetFiles: MutableList<HybridFile> = ArrayList()
        val sourcesFiles: MutableList<HybridFileParcelable> = ArrayList()
        for (f in files[i]) {
          val file = HybridFile(
            OpenMode.FILE,
            paths[i] + "/" + f.getName(applicationContext)
          )
          targetFiles.add(file)
        }
        for (hybridFileParcelables in files) {
          sourcesFiles.addAll(hybridFileParcelables)
        }
        FileUtils.scanFile(applicationContext, sourcesFiles.toTypedArray())
        FileUtils.scanFile(applicationContext, targetFiles.toTypedArray())
      }

      // updating encrypted db entry if any encrypted file was moved
      AppConfig.getInstance()
        .runInBackground {
          for (i in paths.indices) {
            for (file in files[i]) {
              if (file.getName(applicationContext).endsWith(CryptUtil.CRYPT_EXTENSION)) {
                try {
                  val cryptHandler = CryptHandler.getInstance()
                  val oldEntry = cryptHandler.findEntry(file.path)
                  val newEntry = EncryptedEntry()
                  newEntry.id = oldEntry.id
                  newEntry.password = oldEntry.password
                  newEntry.path = paths[i] + "/" + file.getName(applicationContext)
                  cryptHandler.updateEntry(oldEntry, newEntry)
                } catch (e: Exception) {
                  e.printStackTrace()
                  // couldn't change the entry, leave it alone
                }
              }
            }
          }
        }
    } else {
      if (totalBytes > 0 && destinationSize < totalBytes) {
        // destination don't have enough space; return
        Toast.makeText(
          applicationContext,
          applicationContext.resources.getString(R.string.in_safe),
          Toast.LENGTH_LONG
        )
          .show()
        return
      }
      for (i in paths.indices) {
        val intent = Intent(applicationContext, CopyService::class.java)
        intent.putExtra(CopyService.TAG_COPY_SOURCES, files[i])
        intent.putExtra(CopyService.TAG_COPY_TARGET, paths[i])
        intent.putExtra(CopyService.TAG_COPY_MOVE, true)
        intent.putExtra(CopyService.TAG_COPY_OPEN_MODE, mode.ordinal)
        intent.putExtra(CopyService.TAG_IS_ROOT_EXPLORER, isRootExplorer)
        ServiceWatcherUtil.runService(applicationContext, intent)
      }
    }
  }
}