// RegisterCallback.aidl
package com.amaze.filemanager;

// Declare any non-default types here with import statements
import com.amaze.filemanager.ProgressListener;
import com.amaze.filemanager.utils.DataPackage;
interface RegisterCallback {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void registerCallBack(in ProgressListener p);
    List<DataPackage> getCurrent();
}
