package com.amaze.filemanager.filesystem;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.amaze.filemanager.adapters.data.LayoutElementParcelable;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.exceptions.CloudPluginException;
import com.amaze.filemanager.exceptions.ShellNotRunningException;
import com.amaze.filemanager.filesystem.ssh.Statvfs;
import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;

import com.amaze.filemanager.filesystem.ssh.SFtpClientTemplate;
import com.amaze.filemanager.filesystem.ssh.SshClientTemplate;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.utils.application.AppConfig;

import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OnFileFound;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.RootUtils;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.types.SpaceAllocation;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPException;
import net.schmizz.sshj.xfer.FilePermission;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.EnumSet;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by Arpit on 07-07-2015.
 */
//Hybrid file for handeling all types of files
public class HybridFile {

    private static final String TAG = "HFile";

    String path;
    //public static final int ROOT_MODE=3,LOCAL_MODE=0,SMB_MODE=1,UNKNOWN=-1;
    OpenMode mode = OpenMode.FILE;

    private DataUtils dataUtils = DataUtils.getInstance();

    public HybridFile(OpenMode mode, String path) {
        this.path = path;
        this.mode = mode;
    }

    public HybridFile(OpenMode mode, String path, String name, boolean isDirectory) {
        this.mode = mode;
        if (path.startsWith("smb://") || isSmb()) {
            if (!isDirectory) this.path = path + name;
            else if (!name.endsWith("/")) this.path = path + name + "/";
            else this.path = path + name;
        } else if(path.startsWith("ssh://") || isSftp()) {
            this.path = path + "/" + name;
        } else this.path = path + "/" + name;
    }

    public void generateMode(Context context) {
        if (path.startsWith("smb://")) {
            mode = OpenMode.SMB;
        } else if (path.startsWith("ssh://")) {
            mode = OpenMode.SFTP;
        } else if (path.startsWith(OTGUtil.PREFIX_OTG)) {
            mode = OpenMode.OTG;
        } else if (isCustomPath()) {
            mode = OpenMode.CUSTOM;
        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_BOX)) {
            mode = OpenMode.BOX;
        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_ONE_DRIVE)) {
            mode = OpenMode.ONEDRIVE;
        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE)) {
            mode = OpenMode.GDRIVE;
        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_DROPBOX)) {
            mode = OpenMode.DROPBOX;
        } else if(context == null) {
            mode = OpenMode.FILE;
        } else {
            boolean rootmode = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferencesConstants.PREFERENCE_ROOTMODE, false);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                mode = OpenMode.FILE;
                if (rootmode && !getFile().canRead()) {
                    mode = OpenMode.ROOT;
                }
            } else {
                if (FileUtil.isOnExtSdCard(getFile(), context)) {
                    mode = OpenMode.FILE;
                } else if (rootmode && !getFile().canRead()) {
                    mode = OpenMode.ROOT;
                }

                if (mode == OpenMode.UNKNOWN) {
                    mode = OpenMode.FILE;
                }
            }
        }

    }

    public void setMode(OpenMode mode) {
        this.mode = mode;
    }

    public OpenMode getMode() {
        return mode;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isLocal() {
        return mode == OpenMode.FILE;
    }

    public boolean isRoot() {
        return mode == OpenMode.ROOT;
    }

    public boolean isSmb() {
        return mode == OpenMode.SMB;
    }

    public boolean isSftp() { return mode == OpenMode.SFTP; }

    public boolean isOtgFile() {
        return mode == OpenMode.OTG;
    }

    public boolean isBoxFile() {
        return mode == OpenMode.BOX;
    }

    public boolean isDropBoxFile() {
        return mode == OpenMode.DROPBOX;
    }

    public boolean isOneDriveFile() {
        return mode == OpenMode.ONEDRIVE;
    }

    public boolean isGoogleDriveFile() {
        return mode == OpenMode.GDRIVE;
    }

    public File getFile() {
        return new File(path);
    }

    HybridFileParcelable generateBaseFileFromParent() {
        ArrayList<HybridFileParcelable> arrayList = RootHelper.getFilesList(getFile().getParent(), true, true, null);
        for (HybridFileParcelable baseFile : arrayList) {
            if (baseFile.getPath().equals(path))
                return baseFile;
        }
        return null;
    }

    public long lastModified() throws SmbException {
        return new File("/").lastModified();
    }

    /**
     * @deprecated use {@link #length(Context)} to handle content resolvers
     * @return
     */
    public long length() {
        return 0L;
    }

    /**
     * Helper method to find length
     * @param context
     * @return
     */
    public long length(Context context) {
        return 0L;
    }

    public String getPath() {
        return path;
    }

    /**
     * @deprecated use {@link #getName(Context)}
     * @return
     */
    public String getName() {
        StringBuilder builder = new StringBuilder(path);
        String name = builder.substring(builder.lastIndexOf("/") + 1, builder.length());
        return name;
    }

    public String getName(Context context) {
        return getName();
    }

    public boolean isCustomPath() {
        return path.equals("0") ||
                path.equals("1") ||
                path.equals("2") ||
                path.equals("3") ||
                path.equals("4") ||
                path.equals("5") ||
                path.equals("6");
    }

    /**
     * Returns a path to parent for various {@link #mode}
     * @deprecated use {@link #getParent(Context)} to handle content resolvers
     *
     * @return
     */
    public String getParent() {
        StringBuilder builder = new StringBuilder(path);
        String parentPath = builder.substring(0, builder.length() - (getName().length() + 1));
        return parentPath;
    }

    /**
     * Helper method to get parent path
     *
     * @param context
     * @return
     */
    public String getParent(Context context) {
        StringBuilder builder = new StringBuilder(path);
        StringBuilder parentPathBuilder = new StringBuilder(builder.substring(0,
                builder.length()-(getName(context).length()+1)));
        return parentPathBuilder.toString();
    }

    public String getParentName() {
        StringBuilder builder = new StringBuilder(path);
        StringBuilder parentPath = new StringBuilder(builder.substring(0,
                builder.length() - (getName().length() + 1)));
        String parentName = parentPath.substring(parentPath.lastIndexOf("/") + 1,
                parentPath.length());
        return parentName;
    }

    /**
     * Whether this object refers to a directory or file, handles all types of files
     * @deprecated use {@link #isDirectory(Context)} to handle content resolvers
     *
     * @return
     */
    public boolean isDirectory() {
        return new File(path).isDirectory();
    }

    public boolean isDirectory(Context context) {
        return new File(path).isDirectory();
    }

    /**
     * @deprecated use {@link #folderSize(Context)}
     * @return
     */
    public long folderSize() {
        return 0L;
    }

    /**
     * Helper method to get length of folder in an otg
     *
     * @param context
     * @return
     */
    public long folderSize(Context context) {
        return 0L;
    }


    /**
     * Gets usable i.e. free space of a device
     * @return
     */
    public long getUsableSpace() {
        return 0L;
    }

    /**
     * Gets total size of the disk
     * @param context
     * @return
     */
    public long getTotal(Context context) {
        return 0L;
    }

    /**
     * Helper method to list children of this file
     */
    public void forEachChildrenFile(Context context, boolean isRoot, OnFileFound onFileFound) {
        switch (mode) {
            case SFTP:
                try {
                    SshClientUtils.execute(new SFtpClientTemplate(path) {
                        @Override
                        public Void execute(SFTPClient client) throws IOException {
                            try {
                                for (RemoteResourceInfo info : client.ls(SshClientUtils.extractRemotePathFrom(path))) {
                                    HybridFileParcelable f = new HybridFileParcelable(String.format("%s/%s", path, info.getName()));
                                    f.setName(info.getName());
                                    f.setMode(OpenMode.SFTP);
                                    f.setDirectory(info.isDirectory());
                                    f.setDate(info.getAttributes().getMtime() * 1000);
                                    f.setSize(f.isDirectory() ? 0 : info.getAttributes().getSize());
                                    f.setPermission(Integer.toString(FilePermission.toMask(info.getAttributes().getPermissions()), 8));
                                    onFileFound.onFileFound(f);
                                }
                            } catch (IOException e) {
                                Log.w("DEBUG.listFiles", "IOException", e);
                            }
                            return null;
                        }
                    });
                } catch(Exception e){
                    e.printStackTrace();
                }
                break;
            case SMB:
                try {
                    SmbFile smbFile = new SmbFile(path);
                    for (SmbFile smbFile1 : smbFile.listFiles()) {
                        HybridFileParcelable baseFile=new HybridFileParcelable(smbFile1.getPath());
                        baseFile.setName(smbFile1.getName());
                        baseFile.setMode(OpenMode.SMB);
                        baseFile.setDirectory(smbFile1.isDirectory());
                        baseFile.setDate(smbFile1.lastModified());
                        baseFile.setSize(baseFile.isDirectory()?0:smbFile1.length());
                        onFileFound.onFileFound(baseFile);
                    }
                } catch (MalformedURLException | SmbException e) {
                    e.printStackTrace();
                }
                break;
            case OTG:
                OTGUtil.getDocumentFiles(path, context, onFileFound);
                break;
            case DROPBOX:
            case BOX:
            case GDRIVE:
            case ONEDRIVE:
                try {
                    CloudUtil.getCloudFiles(path, dataUtils.getAccount(mode), mode, onFileFound);
                } catch (CloudPluginException e) {
                    e.printStackTrace();
                }
                break;
            default:
                RootHelper.getFiles(path, isRoot, true, null, onFileFound);

        }
    }

    /**
     * Helper method to list children of this file
     * @deprecated use forEachChildrenFile()
     */
    public ArrayList<HybridFileParcelable> listFiles(Context context, boolean isRoot) {
        ArrayList<HybridFileParcelable> arrayList = new ArrayList<>();
        switch (mode) {
            case SFTP:
                try {
                    arrayList = SshClientUtils.execute(new SFtpClientTemplate(path) {
                        @Override
                        public ArrayList<HybridFileParcelable> execute(SFTPClient client) throws IOException {
                            ArrayList<HybridFileParcelable> retval = new ArrayList<HybridFileParcelable>();
                            try {
                                for (RemoteResourceInfo info : client.ls(SshClientUtils.extractRemotePathFrom(path))) {
                                    HybridFileParcelable f = new HybridFileParcelable(String.format("%s/%s", path, info.getName()));
                                    f.setName(info.getName());
                                    f.setMode(OpenMode.SFTP);
                                    f.setDirectory(info.isDirectory());
                                    f.setDate(info.getAttributes().getMtime() * 1000);
                                    f.setSize(f.isDirectory() ? 0 : info.getAttributes().getSize());
                                    f.setPermission(Integer.toString(FilePermission.toMask(info.getAttributes().getPermissions()), 8));
                                    retval.add(f);
                                }
                            } catch (IOException e) {
                                Log.w("DEBUG.listFiles", "IOException", e);
                            }
                            return retval;
                        }
                    });
                } catch(Exception e){
                    e.printStackTrace();
                    arrayList.clear();
                }
                break;
            case SMB:
                try {
                    SmbFile smbFile = new SmbFile(path);
                    for (SmbFile smbFile1 : smbFile.listFiles()) {
                        HybridFileParcelable baseFile=new HybridFileParcelable(smbFile1.getPath());
                        baseFile.setName(smbFile1.getName());
                        baseFile.setMode(OpenMode.SMB);
                        baseFile.setDirectory(smbFile1.isDirectory());
                        baseFile.setDate(smbFile1.lastModified());
                        baseFile.setSize(baseFile.isDirectory()?0:smbFile1.length());
                        arrayList.add(baseFile);
                    }
                } catch (MalformedURLException e) {
                    if (arrayList != null) arrayList.clear();
                    e.printStackTrace();
                } catch (SmbException e) {
                    if (arrayList != null) arrayList.clear();
                    e.printStackTrace();
                }
                break;
            case OTG:
                arrayList = OTGUtil.getDocumentFilesList(path, context);
                break;
            case DROPBOX:
            case BOX:
            case GDRIVE:
            case ONEDRIVE:
                try {
                    arrayList = CloudUtil.listFiles(path, dataUtils.getAccount(mode), mode);
                } catch (CloudPluginException e) {
                    e.printStackTrace();
                    arrayList = new ArrayList<>();
                }
                break;
            default:
                arrayList = RootHelper.getFilesList(path, isRoot, true, null);

        }

        return arrayList;
    }

    public String getReadablePath(String path) {
        if (isSftp())
            return parseSftpPath(path);
        if (isSmb())
            return parseSmbPath(path);
        return path;
    }

    String parseSftpPath(String a) {
        if (a.contains("@"))
            return "ssh://" + a.substring(a.indexOf("@") + 1, a.length());
        else return a;
    }

    String parseSmbPath(String a) {
        if (a.contains("@"))
            return "smb://" + a.substring(a.indexOf("@") + 1, a.length());
        else return a;
    }

    /**
     * Handles getting input stream for various {@link OpenMode}
     * @deprecated use {@link #getInputStream(Context)} which allows handling content resolver
     * @return
     */
    public InputStream getInputStream() {
        InputStream inputStream;
        if (isSftp()) {
            return SshClientUtils.execute(new SFtpClientTemplate(path) {
                @Override
                public InputStream execute(SFTPClient client) throws IOException {
                    final RemoteFile rf = client.open(SshClientUtils.extractRemotePathFrom(path));
                    return rf. new RemoteFileInputStream(){
                        @Override
                        public void close() throws IOException {
                            try
                            {
                                super.close();
                            }
                            finally
                            {
                                rf.close();
                            }
                        }
                    };
                }
            });
        }
        else if (isSmb()) {
            try {
                inputStream = new SmbFile(path).getInputStream();
            } catch (IOException e) {
                inputStream = null;
                e.printStackTrace();
            }
        } else {
            try {
                inputStream = new FileInputStream(path);
            } catch (FileNotFoundException e) {
                inputStream = null;
                e.printStackTrace();
            }
        }
        return inputStream;
    }

    public InputStream getInputStream(Context context) {
        InputStream inputStream;

        switch (mode) {
            case SFTP:
                inputStream = SshClientUtils.execute(new SFtpClientTemplate(path, false) {
                    @Override
                    public InputStream execute(final SFTPClient client) throws IOException {
                        final RemoteFile rf = client.open(SshClientUtils.extractRemotePathFrom(path));
                        return rf. new RemoteFileInputStream(){
                            @Override
                            public void close() throws IOException {
                                try
                                {
                                    super.close();
                                }
                                finally
                                {
                                    rf.close();
                                    client.close();
                                }
                            }
                        };
                    }
                });
                break;
            case SMB:
                try {
                    inputStream = new SmbFile(path).getInputStream();
                } catch (IOException e) {
                    inputStream = null;
                    e.printStackTrace();
                }
                break;
            case OTG:
                ContentResolver contentResolver = context.getContentResolver();
                DocumentFile documentSourceFile = OTGUtil.getDocumentFile(path,
                        context, false);
                try {
                    inputStream = contentResolver.openInputStream(documentSourceFile.getUri());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    inputStream = null;
                }
                break;
            case DROPBOX:
                CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
                Log.d(getClass().getSimpleName(), CloudUtil.stripPath(OpenMode.DROPBOX, path));
                inputStream = cloudStorageDropbox.download(CloudUtil.stripPath(OpenMode.DROPBOX, path));
                break;
            case BOX:
                CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
                inputStream = cloudStorageBox.download(CloudUtil.stripPath(OpenMode.BOX, path));
                break;
            case GDRIVE:
                CloudStorage cloudStorageGDrive = dataUtils.getAccount(OpenMode.GDRIVE);
                inputStream = cloudStorageGDrive.download(CloudUtil.stripPath(OpenMode.GDRIVE, path));
                break;
            case ONEDRIVE:
                CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
                inputStream = cloudStorageOneDrive.download(CloudUtil.stripPath(OpenMode.ONEDRIVE, path));
                break;
            default:
                try {
                    inputStream = new FileInputStream(path);
                } catch (FileNotFoundException e) {
                    inputStream = null;
                    e.printStackTrace();
                }
                break;
        }
        return inputStream;
    }

    public OutputStream getOutputStream(Context context) {
        OutputStream outputStream;
        switch (mode) {
            case SFTP:
                return SshClientUtils.execute(new SshClientTemplate(path, false) {
                    @Override
                    public OutputStream execute(final SSHClient ssh) throws IOException {
                        final SFTPClient client = ssh.newSFTPClient();
                        final RemoteFile rf = client.open(SshClientUtils.extractRemotePathFrom(path),
                                EnumSet.of(net.schmizz.sshj.sftp.OpenMode.WRITE,
                                        net.schmizz.sshj.sftp.OpenMode.CREAT));
                        return rf.new RemoteFileOutputStream(){
                            @Override
                            public void close() throws IOException {
                                try
                                {
                                    super.close();
                                }
                                finally
                                {
                                    rf.close();
                                    client.close();
                                }
                            }
                        };
                    }
                });
            case SMB:
                try {
                    outputStream = new SmbFile(path).getOutputStream();
                } catch (IOException e) {
                    outputStream = null;
                    e.printStackTrace();
                }
                break;
            case OTG:
                ContentResolver contentResolver = context.getContentResolver();
                DocumentFile documentSourceFile = OTGUtil.getDocumentFile(path,
                        context, true);
                try {
                    outputStream = contentResolver.openOutputStream(documentSourceFile.getUri());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    outputStream = null;
                }
                break;
            default:
                try {
                    outputStream = FileUtil.getOutputStream(new File(path), context);
                } catch (Exception e) {
                    outputStream=null;
                    e.printStackTrace();
                }

        }
        return outputStream;
    }

    public boolean exists() {
        return false;
    }

    /**
     * Helper method to check file existence in otg
     *
     * @param context
     * @return
     */
    public boolean exists(Context context) {
        return exists();
    }

    /**
     * Whether file is a simple file (i.e. not a directory/smb/otg/other)
     *
     * @return true if file; other wise false
     */
    public boolean isSimpleFile() {
        return !isSmb() && !isOtgFile() && !isCustomPath()
                && !android.util.Patterns.EMAIL_ADDRESS.matcher(path).matches() &&
                !new File(path).isDirectory() && !isOneDriveFile() && !isGoogleDriveFile()
                && !isDropBoxFile() && !isBoxFile() && !isSftp();
    }

    public boolean setLastModified(final long date) {
        if (isSmb()) {
            try {
                new SmbFile(path).setLastModified(date);
                return true;
            } catch (SmbException e) {
                return false;
            } catch (MalformedURLException e) {
                return false;
            }
        }
        File f = new File(path);
        return f.setLastModified(date);

    }

    public void mkdir(Context context) {
        if(isSftp()) {
            SshClientUtils.execute(new SFtpClientTemplate(path) {
                @Override
                public Void execute(SFTPClient client) throws IOException {
                    try {
                        client.mkdir(SshClientUtils.extractRemotePathFrom(path));
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            });
        } else if (isSmb()) {
            try {
                new SmbFile(path).mkdirs();
            } catch (SmbException | MalformedURLException e) {
                e.printStackTrace();
            }
        } else if (isOtgFile()) {
            if (!exists(context)) {
                DocumentFile parentDirectory = OTGUtil.getDocumentFile(getParent(context), context, false);
                if (parentDirectory.isDirectory()) {
                    parentDirectory.createDirectory(getName(context));
                }
            }
        } else if (isDropBoxFile()) {
            CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
            try {
                cloudStorageDropbox.createFolder(CloudUtil.stripPath(OpenMode.DROPBOX, path));
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else if (isBoxFile()) {
            CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
            try {
                cloudStorageBox.createFolder(CloudUtil.stripPath(OpenMode.BOX, path));
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else if (isOneDriveFile()) {
            CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
            try {
                cloudStorageOneDrive.createFolder(CloudUtil.stripPath(OpenMode.ONEDRIVE, path));
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else if (isGoogleDriveFile()) {
            CloudStorage cloudStorageGdrive = dataUtils.getAccount(OpenMode.GDRIVE);
            try {
                cloudStorageGdrive.createFolder(CloudUtil.stripPath(OpenMode.GDRIVE, path));
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else
            FileUtil.mkdir(new File(path), context);
    }

    public boolean delete(Context context, boolean rootmode) throws ShellNotRunningException {
        if (isSftp()) {
            SshClientUtils.execute(new SFtpClientTemplate(path) {
                @Override
                public Void execute(SFTPClient client) throws IOException {
                    if(isDirectory(AppConfig.getInstance()))
                        client.rmdir(SshClientUtils.extractRemotePathFrom(path));
                    else
                        client.rm(SshClientUtils.extractRemotePathFrom(path));
                    return null;
                }
            });
            return true;
        } else if (isSmb()) {
            try {
                new SmbFile(path).delete();
            } catch (SmbException | MalformedURLException e) {
                e.printStackTrace();
            }
        } else {
            if (isRoot() && rootmode) {
                setMode(OpenMode.ROOT);
                RootUtils.delete(getPath());
            } else {
                FileUtil.deleteFile(new File(path), context);
            }
        }
        return !exists();
    }

    /**
     * Returns the name of file excluding it's extension
     * If no extension is found then whole file name is returned
     * @param context
     * @return
     */
    public String getNameString(Context context) {
        String fileName = getName(context);

        int extensionStartIndex = fileName.lastIndexOf(".");
        return fileName.substring(0, extensionStartIndex == -1 ? fileName.length() : extensionStartIndex);
    }

    /**
     * Generates a {@link LayoutElementParcelable} adapted compatible element.
     * Currently supports only local filesystem
     */
    public LayoutElementParcelable generateLayoutElement(boolean showThumbs) {
        switch (mode) {
            case FILE:
            case ROOT:
                File file = new File(path);
                LayoutElementParcelable layoutElement;
                if (isDirectory()) {

                    layoutElement = new LayoutElementParcelable(path, RootHelper.parseFilePermission(file),
                            "", folderSize() + "", 0, true,
                            false, file.lastModified() + "", showThumbs);
                } else {
                    layoutElement = new LayoutElementParcelable(
                            file.getPath(), RootHelper.parseFilePermission(file),
                            file.getPath(), file.length() + "", file.length(), false,
                            false, file.lastModified() + "", showThumbs);
                }
                layoutElement.setMode(mode);
                return layoutElement;
            default:
                return null;
        }
    }
}
