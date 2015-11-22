// ProgressListener.aidl
package com.amaze.filemanager;

// Declare any non-default types here with import statements
import com.amaze.filemanager.utils.DataPackage;
interface ProgressListener {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
        void onUpdate(in DataPackage dataPackage);
        void refresh();
}
