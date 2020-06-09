package com.amaze.filemanager.utils.application;

import com.bumptech.glide.Glide;
import com.bumptech.glide.MemoryCategory;

/**
 * @author Emmanuel
 *         on 22/11/2017, at 17:18.
 */

public class GlideApplication extends LeakCanaryApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH);
    }
}
