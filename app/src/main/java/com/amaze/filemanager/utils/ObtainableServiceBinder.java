package com.amaze.filemanager.utils;

import android.app.Service;
import android.os.Binder;

/**
 * @author Emmanuel
 *         on 28/11/2017, at 19:04.
 */

public class ObtainableServiceBinder<T extends Service> extends Binder {

    private final T service;

    public ObtainableServiceBinder(T service) {
        this.service = service;
    }

    public T getService() {
        return service;
    }

}
