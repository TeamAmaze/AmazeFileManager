package com.amaze.filemanager.filesystem.operations.singlefile;

import android.content.Context;

import com.amaze.filemanager.exceptions.ShellNotRunningException;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.Operations;
import com.amaze.filemanager.filesystem.operations.AbstractOperation;
import com.amaze.filemanager.filesystem.operations.exceptions.ShellNotRunningIOException;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.RootUtils;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.cloudrail.si.interfaces.CloudStorage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import jcifs.smb.SmbException;

public class CreateFileOperation extends AbstractOperation {
	private final DataUtils dataUtils = DataUtils.getInstance();

	@NonNull
	private final Context context;
	@NonNull
	private final HybridFile file;
	private final boolean rootMode;
	@NonNull
	private final Operations.ErrorCallBack errorCallBack;

	public CreateFileOperation(@NonNull Context context, @NonNull HybridFile file,  boolean rootMode,
	                           @NonNull Operations.ErrorCallBack errorCallBack) {

		this.context = context;
		this.file = file;
		this.rootMode = rootMode;
		this.errorCallBack = errorCallBack;
	}

	@Override
	protected boolean check() {
		// check whether filename is valid or not
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
			OutputStream out = file.getOutputStream(context);
			if(out == null) {
				errorCallBack.done(file, false);
				throw new IOException();
			}

			try {
				out.close();
			} catch(IOException e) {
				errorCallBack.done(file, false);
				throw e;
			}
		} else if (file.isSmb()) {
			try {
				file.getSmbFile(2000).createNewFile();
			} catch (SmbException e) {
				errorCallBack.done(file, false);
				throw e;
			}

			if(!file.exists()) {
				throw new IOException("Created file doesn't exist!");
			}
		} else if (file.isDropBoxFile()) {
			CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
			byte[] tempBytes = new byte[0];
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(tempBytes);
			cloudStorageDropbox.upload(CloudUtil.stripPath(OpenMode.DROPBOX, file.getPath()),
						byteArrayInputStream, 0l, true);
		} else if (file.isBoxFile()) {
			CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
				byte[] tempBytes = new byte[0];
				ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(tempBytes);
				cloudStorageBox.upload(CloudUtil.stripPath(OpenMode.BOX, file.getPath()),
						byteArrayInputStream, 0l, true);
		} else if (file.isOneDriveFile()) {
			CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
				byte[] tempBytes = new byte[0];
				ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(tempBytes);
				cloudStorageOneDrive.upload(CloudUtil.stripPath(OpenMode.ONEDRIVE, file.getPath()),
						byteArrayInputStream, 0l, true);
		} else if (file.isGoogleDriveFile()) {
			CloudStorage cloudStorageGdrive = dataUtils.getAccount(OpenMode.GDRIVE);
				byte[] tempBytes = new byte[0];
				ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(tempBytes);
				cloudStorageGdrive.upload(CloudUtil.stripPath(OpenMode.GDRIVE, file.getPath()),
						byteArrayInputStream, 0l, true);
		} else if (file.isOtgFile()) {

			// first check whether new file already exists
			DocumentFile fileToCreate = OTGUtil.getDocumentFile(file.getPath(), context, false);
			if (fileToCreate != null) {
				errorCallBack.exists(file);
				throw new IOException("The file already exists!");
			}

			DocumentFile parentDirectory = OTGUtil.getDocumentFile(file.getParent(), context, false);

			if (!parentDirectory.isDirectory()) {
				errorCallBack.done(file, false);
				throw new IOException("Parent isn't a directory!");
			}

			parentDirectory.createFile(file.getName(context).substring(file.getName().lastIndexOf(".")),
					file.getName(context));
		} else {
			if (file.isLocal() || file.isRoot()) {
				int mode = Operations.checkFolder(new File(file.getParent()), context);
				if (mode == 2) {
					errorCallBack.launchSAF(file);
					throw new IOException("Access needed");
				}

				if (mode == 1 || mode == 0) {
					FileUtil.mkfile(file.getFile(), context);
				}

				if (!file.exists() && rootMode) {
					file.setMode(OpenMode.ROOT);
					if (file.exists()) {
						errorCallBack.exists(file);
						throw new IOException("The file already exists!");
					}

					try {
						RootUtils.mkFile(file.getPath());
					} catch (ShellNotRunningException e) {
						throw new ShellNotRunningIOException(e);
					}
				}
			}

			if(!file.exists()) {
				throw new IOException("Created file doesn't exist!");
			}
		}

		errorCallBack.done(file, true);
	}

	@Override
	protected void undo() {

	}
}
