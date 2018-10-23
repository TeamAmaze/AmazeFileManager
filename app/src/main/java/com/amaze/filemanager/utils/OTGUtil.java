package com.amaze.filemanager.utils;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.filesystem.SingletonUsbOtg;

import java.util.ArrayList;
import java.util.HashMap;

import static android.content.Context.USB_SERVICE;

/**
 * Created by Vishal on 27-04-2017.
 */

public class OTGUtil {

    public static final String PREFIX_OTG = "otg:/";

    /**
     * Returns an array of list of files at a specific path in OTG
     *
     * @param path    the path to the directory tree, starts with prefix 'otg:/'
     *                Independent of URI (or mount point) for the OTG
     * @param context context for loading
     * @return an array of list of files at the path
     * @deprecated use getDocumentFiles()
     */
    public static ArrayList<HybridFileParcelable> getDocumentFilesList(String path, Context context) {
        final ArrayList<HybridFileParcelable> files = new ArrayList<>();
        getDocumentFiles(path, context, files::add);
        return files;
    }

    /**
     * Get the files at a specific path in OTG
     *
     * @param path    the path to the directory tree, starts with prefix 'otg:/'
     *                Independent of URI (or mount point) for the OTG
     * @param context context for loading
     */
    public static void getDocumentFiles(String path, Context context, OnFileFound fileFound) {
        Uri rootUriString = SingletonUsbOtg.getInstance().getUsbOtgRoot();
        if(rootUriString == null) throw new NullPointerException("USB OTG root not set!");

        DocumentFile rootUri = DocumentFile.fromTreeUri(context, rootUriString);

        String[] parts = path.split("/");
        for (String part : parts) {
            // first omit 'otg:/' before iterating through DocumentFile
            if (path.equals(OTGUtil.PREFIX_OTG + "/")) break;
            if (part.equals("otg:") || part.equals("")) continue;

            // iterating through the required path to find the end point
            rootUri = rootUri.findFile(part);
        }

        // we have the end point DocumentFile, list the files inside it and return
        for (DocumentFile file : rootUri.listFiles()) {
            if (file.exists()) {
                long size = 0;
                if (!file.isDirectory()) size = file.length();
                Log.d(context.getClass().getSimpleName(), "Found file: " + file.getName());
                HybridFileParcelable baseFile = new HybridFileParcelable(path + "/" + file.getName(),
                        RootHelper.parseDocumentFilePermission(file), file.lastModified(), size, file.isDirectory());
                baseFile.setName(file.getName());
                baseFile.setMode(OpenMode.OTG);
                fileFound.onFileFound(baseFile);
            }
        }
    }

    /**
     * Traverse to a specified path in OTG
     *
     * @param createRecursive flag used to determine whether to create new file while traversing to path,
     *                        in case path is not present. Notably useful in opening an output stream.
     */
    public static DocumentFile getDocumentFile(String path, Context context, boolean createRecursive) {
        Uri rootUriString = SingletonUsbOtg.getInstance().getUsbOtgRoot();
        if(rootUriString == null) throw new NullPointerException("USB OTG root not set!");

        // start with root of SD card and then parse through document tree.
        DocumentFile rootUri = DocumentFile.fromTreeUri(context, rootUriString);

        String[] parts = path.split("/");
        for (String part : parts) {
            if (path.equals("otg:/")) break;
            if (part.equals("otg:") || part.equals("")) continue;

            // iterating through the required path to find the end point
            DocumentFile nextDocument = rootUri.findFile(part);
            if (createRecursive && (nextDocument == null || !nextDocument.exists())) {
                nextDocument = rootUri.createFile(part.substring(part.lastIndexOf(".")), part);
            }
            rootUri = nextDocument;
        }

        return rootUri;
    }

    /**
     * Checks if there is at least one USB device connected with class MASS STORAGE.
     */
    public static boolean isMassStorageDeviceConnected(@NonNull final Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(USB_SERVICE);
        if(usbManager == null) return false;

        HashMap<String, UsbDevice> devices = usbManager.getDeviceList();

        for (String deviceName : devices.keySet()) {
            UsbDevice device = devices.get(deviceName);

            for (int i = 0; i < device.getInterfaceCount(); i++){
                if (device.getInterface(i).getInterfaceClass() == UsbConstants.USB_CLASS_MASS_STORAGE){
                    return true;
                }
            }
        }

        return false;
    }

}
