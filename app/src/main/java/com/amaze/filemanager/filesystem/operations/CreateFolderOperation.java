package com.amaze.filemanager.filesystem.operations;

import android.content.Context;
import android.os.Build;

import com.amaze.filemanager.exceptions.ShellNotRunningException;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.Operations;
import com.amaze.filemanager.filesystem.operations.exceptions.ShellNotRunningIOException;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.RootUtils;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.cloudrail.si.interfaces.CloudStorage;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

public class CreateFolderOperation extends AbstractOperation {
	private final DataUtils dataUtils = DataUtils.getInstance();

	@NonNull
	private final Context context;
	@NonNull
	private final HybridFile file;
	private final boolean rootMode;
	@NonNull
	private final Operations.ErrorCallBack errorCallBack;

	public CreateFolderOperation(@NonNull Context context, @NonNull HybridFile file, boolean rootMode,
	                             @NonNull Operations.ErrorCallBack errorCallBack) {
		this.context = context;
		this.file = file;
		this.rootMode = rootMode;
		this.errorCallBack = errorCallBack;
	}

	@Override
	protected boolean check() {
		// checking whether filename is valid or a recursive call possible
		if (!Operations.isFileNameValid(file.getName(context))) {
			errorCallBack.invalidName(file);
			return false;
		}
		if (file.exists()) {
			errorCallBack.exists(file);
			return false;
		}
		return true;
	}

	@Override
	protected void operate() throws IOException {
		if (file.isSftp()) {
			file.mkdir(context);
		} else if (file.isSmb()) {
			file.getSmbFile(2000).mkdirs();

			if(!file.exists()) {
				errorCallBack.done(file, false);
				throw new IOException("Created folder doesn't actually exist!");
			}
		} else if (file.isOtgFile()) {
			// first check whether new directory already exists
			DocumentFile directoryToCreate = OTGUtil.getDocumentFile(file.getPath(), context, false);
			if (directoryToCreate != null) {
				errorCallBack.exists(file);
				throw new IOException("The file already exists!");
			}

			DocumentFile parentDirectory = OTGUtil.getDocumentFile(file.getParent(), context, false);
			if (!parentDirectory.isDirectory()) {
				errorCallBack.done(file, false);
				throw new IOException("Created folder doesn't actually exist!");
			}

			parentDirectory.createDirectory(file.getName(context));
		} else if (file.isDropBoxFile()) {
			CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
			cloudStorageDropbox.createFolder(CloudUtil.stripPath(OpenMode.DROPBOX, file.getPath()));
		} else if (file.isBoxFile()) {
			CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
			cloudStorageBox.createFolder(CloudUtil.stripPath(OpenMode.BOX, file.getPath()));
		} else if (file.isOneDriveFile()) {
			CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
			cloudStorageOneDrive.createFolder(CloudUtil.stripPath(OpenMode.ONEDRIVE, file.getPath()));
		} else if (file.isGoogleDriveFile()) {
			CloudStorage cloudStorageGdrive = dataUtils.getAccount(OpenMode.GDRIVE);
			cloudStorageGdrive.createFolder(CloudUtil.stripPath(OpenMode.GDRIVE, file.getPath()));
		} else {
			if (file.isLocal() || file.isRoot()) {
				int mode = checkFolder(new File(file.getParent()), context);

				if (mode == 2) {
					errorCallBack.launchSAF(file);
					throw new IOException("Access needed");
				}

				if (mode == 1 || mode == 0) {
					FileUtil.mkdir(file.getFile(), context);
				}

				if (!file.exists() && rootMode) {
					file.setMode(OpenMode.ROOT);
					if (file.exists()) {
						errorCallBack.exists(file);
						throw new IOException("Created folder doesn't actually exist!");
					}

					try {
						RootUtils.mkDir(file.getParent(context), file.getName(context));
					} catch (ShellNotRunningException e) {
						errorCallBack.done(file, false);
						throw new ShellNotRunningIOException(e);
					}
				}

			}

			if(!file.exists()) {
				errorCallBack.done(file, false);
				throw new IOException("Created folder doesn't actually exist!");
			}
		}

		errorCallBack.done(file, true);
	}

	@Override
	protected void undo() {
		//You either have the file or you don't,
		//in the first case it is already undone, in the second the operation succeeded,
		//there's nothing to undo
	}

	private static int checkFolder(final File folder, Context context) {
		boolean lol = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
		if (lol) {

			boolean ext = FileUtil.isOnExtSdCard(folder, context);
			if (ext) {

				if (!folder.exists() || !folder.isDirectory()) {
					return 0;
				}

				// On Android 5, trigger storage access framework.
				if (!FileUtil.isWritableNormalOrSaf(folder, context)) {
					return 2;
				}
				return 1;
			}
		} else if (Build.VERSION.SDK_INT == 19) {
			// Assume that Kitkat workaround works
			if (FileUtil.isOnExtSdCard(folder, context)) return 1;

		}

		// file not on external sd card
		if (FileUtil.isWritable(new File(folder, "DummyFile"))) {
			return 1;
		} else {
			return 0;
		}
	}

}
