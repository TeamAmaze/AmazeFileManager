// IMyAidlInterface.aidl
package com.amaze.filemanager;

import com.amaze.filemanager.Loadlistener;
// Declare any non-default types here with import statements
interface IMyAidlInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void load(String id,Loadlistener listener);
    void goback(String id,Loadlistener listener);
    void create(Loadlistener listener);
}
