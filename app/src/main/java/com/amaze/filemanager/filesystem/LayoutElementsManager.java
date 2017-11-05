package com.amaze.filemanager.filesystem;

import android.os.Parcel;
import android.os.Parcelable;

import com.amaze.filemanager.ui.LayoutElementParcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is thread safe.
 * Singleton thread safety source: http://regrecall.blogspot.com.ar/2012/05/java-singleton-pattern-thread-safe.html
 *
 * @author Emmanuel
 *         on 5/11/2017, at 13:55.
 */

public class LayoutElementsManager implements Parcelable {

    /**
     * This is not an exact copy of the elements in the adapter
     *
     * Iterations should be done synchronizing with this:
     *
     * synchronized(getLock()) {
     *     Iterator i = list.iterator(); // Must be in synchronized block
     *     while (i.hasNext()) foo(i.next());
     * }
     */
    private List<LayoutElementParcelable> layoutElements;
    private static volatile LayoutElementsManager instance;

    public static LayoutElementsManager getInstance() {
        if(instance == null) {
            synchronized (LayoutElementsManager.class) {
                if(instance == null) {
                    instance = new LayoutElementsManager();
                }
            }
        }

        return instance;
    }

    /**
     * Used for restoring state
     */
    public synchronized static void setInstance(LayoutElementsManager e) {
        instance = e;
    }

    private LayoutElementsManager() {}

    public void addLayoutElement(LayoutElementParcelable layoutElement) {
        layoutElements.add(layoutElement);
    }

    public LayoutElementParcelable getLayoutElement(int index) {
        return layoutElements.get(index);
    }

    public void putLayoutElements(ArrayList<LayoutElementParcelable> layoutElements) {
        this.layoutElements = Collections.synchronizedList(new ArrayList<>(layoutElements));
    }

    /**
     * Get a COPY.
     */
    public ArrayList<LayoutElementParcelable> getLayoutElementsClone() {
        return layoutElements != null? new ArrayList<>(layoutElements):null;
    }

    public int getLayoutElementSize() {
        return layoutElements.size();
    }

    public void clearLayoutElement() {
        layoutElements.clear();
    }

    public void removeLayoutElement(int index) {
        layoutElements.remove(index);
    }

    private LayoutElementsManager(Parcel in) {
        layoutElements = (ArrayList<LayoutElementParcelable>) in.readArrayList(LayoutElementParcelable.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(layoutElements);
    }

    public static final Creator<LayoutElementsManager> CREATOR = new Creator<LayoutElementsManager>() {
        @Override
        public LayoutElementsManager createFromParcel(Parcel in) {
            return new LayoutElementsManager(in);
        }

        @Override
        public LayoutElementsManager[] newArray(int size) {
            return new LayoutElementsManager[size];
        }
    };

}
