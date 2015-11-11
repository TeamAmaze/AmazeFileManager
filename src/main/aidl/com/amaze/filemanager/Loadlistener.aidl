// Loadlistener.aidl
package com.amaze.filemanager;

// Declare any non-default types here with import statements
import com.amaze.filemanager.ui.Layoutelements;
interface Loadlistener {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void load(in List<Layoutelements> layoutelements,String driveId);
    void error(String message,int mode);
}
