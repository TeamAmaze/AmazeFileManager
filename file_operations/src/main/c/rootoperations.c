#include <jni.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>
#include <stdbool.h>

JNIEXPORT jboolean JNICALL Java_com_amaze_filemanager_fileoperations_filesystem_root_NativeOperations_isDirectory(
        JNIEnv * env,
        jobject thiz,
        jstring path
        ) {
    struct stat path_stat;
    const char * cPath = (*env)->GetStringUTFChars(env, path, NULL);
    int returnCode = stat(cPath, &path_stat);
    (*env)->ReleaseStringUTFChars(env, path, cPath);

    if(returnCode == -1) {
        switch (errno) {
            case ELOOP:
                return true;
            default:
                return false;
        }
    }

    //This follows links
    return S_ISDIR(path_stat.st_mode);
}