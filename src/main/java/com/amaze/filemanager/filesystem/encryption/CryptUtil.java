package com.amaze.filemanager.filesystem.encryption;

import android.content.Context;
import android.os.Build;

import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ProgressHandler;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.files.GenericCopyUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

import javax.crypto.Cipher;

/**
 * Created by vishal on 6/4/17.
 *
 * Class provide helper methods to encrypt/decrypt various type of files, or passwords
 * We take the password from user before encrypting file. First, the password is encrypted against
 * the key created in keystore in android {@see #encryptPassword(String)}.
 * We're using AES encryption with GCM as the processor algorithm.
 * The encrypted password is mapped against the file path to be encrypted in database for later use.
 * This is handled by the service invoking this instance.
 * The service then calls the constructor which fires up the subsequent encryption/decryption process.
 *
 * We differentiate between already encrypted files from <i>new ones</i> by encrypting the plaintext
 * {@link com.amaze.filemanager.fragments.preference_fragments.Preffrag#ENCRYPT_PASSWORD_MASTER}
 * and {@link com.amaze.filemanager.fragments.preference_fragments.Preffrag#ENCRYPT_PASSWORD_FINGERPRINT}
 * against the path in database. At the time of decryption, we check for these values
 * and either retrieve master password from preferences or fire up the fingerprint sensor authentication.
 *
 * From <i>new ones</i> we mean the ones when were encrypted after user changed preference
 * for master password/fingerprint sensor from settings.
 *
 * We use buffered streams to process files, usage of NIO will probably mildly effect the performance.
 *
 * Be sure to use constructors to encrypt/decrypt files only, and to call service through
 * {@link ServiceWatcherUtil} and to initialize watchers beforehand
 */

public class CryptUtil {

    public static final String ALGO_AES = "AES/GCM/NoPadding";
    public static final String ALGO_RSA = "RSA/ECB/PKCS1Padding";
    public static final String KEY_STORE_ANDROID = "AndroidKeyStore";
    public static final String KEY_ALIAS_AMAZE = "AmazeKey";
    public static final String PREFERENCE_KEY = "aes_key";
    // TODO: Generate a random IV every time, and keep track of it (in database against encrypted files)
    public static final String IV = "LxbHiJhhUXcj";    // 12 byte long IV supported by android for GCM

    public static final String CRYPT_EXTENSION = ".aze";

    private ProgressHandler progressHandler;
    private ArrayList<HFile> failedOps;
    private EncryptFunctions encryption;

    public static EncryptFunctions getCompatibleEncryptionInstance() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new AESEncryption();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return new RSAEncryption();
        } else return null;
    }

    /**
     * Constructor will start encryption process serially. Make sure to call with background thread.
     * The result file of encryption will be in the same directory with a {@link #CRYPT_EXTENSION} extension
     *
     * Make sure you're done with encrypting password for this file and map it with this file in database
     *
     * Be sure to use constructors to encrypt/decrypt files only, and to call service through
     * {@link ServiceWatcherUtil} and to initialize watchers beforehand
     *
     * @param context
     * @param sourceFile the file to encrypt
     */
    public CryptUtil(Context context, BaseFile sourceFile, ProgressHandler progressHandler,
                     ArrayList<HFile> failedOps, EncryptFunctions crypt)
            throws IOException, GeneralSecurityException {

        this.progressHandler = progressHandler;
        this.failedOps = failedOps;
        encryption = crypt;

        // target encrypted file
        HFile hFile = new HFile(sourceFile.getMode(), sourceFile.getParent(context));

        encrypt(context, sourceFile, hFile);
    }

    /**
     * Decrypt the file in specified path. Can be used to open the file (decrypt in cache) or
     * simply decrypt the file in the same (or in a custom preference) directory
     * Make sure to decrypt and check user provided passwords beforehand from database
     *
     * Be sure to use constructors to encrypt/decrypt files only, and to call service through
     * {@link ServiceWatcherUtil} and to initialize watchers beforehand
     *
     * @param context
     * @param baseFile the encrypted file
     * @param targetPath the directory in which file is to be decrypted
     *                   the source's parent in normal case
     */
    public CryptUtil(Context context, BaseFile baseFile, String targetPath,
                     ProgressHandler progressHandler, ArrayList<HFile> failedOps,
                     EncryptFunctions crypt)
            throws IOException, GeneralSecurityException {

        this.progressHandler = progressHandler;
        this.failedOps = failedOps;
        encryption = crypt;

        HFile targetDirectory = new HFile(OpenMode.FILE, targetPath);
        if (!targetPath.equals(context.getExternalCacheDir().toString())) {

            // same file system as of base file
            targetDirectory.setMode(baseFile.getMode());
        }

        decrypt(context, baseFile, targetDirectory);
    }

    /**
     * Wrapper around handling decryption for directory tree
     * @param context
     * @param sourceFile        the source file to decrypt
     * @param targetDirectory   the target directory inside which we're going to decrypt
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private void decrypt(Context context, BaseFile sourceFile, HFile targetDirectory)
            throws IOException, GeneralSecurityException{

        if (sourceFile.isDirectory()) {

            HFile hFile = new HFile(targetDirectory.getMode(), targetDirectory.getPath(),
                    sourceFile.getName().replace(CRYPT_EXTENSION, ""), sourceFile.isDirectory());
            FileUtil.mkdirs(context, hFile);

            for (BaseFile baseFile : sourceFile.listFiles(context, sourceFile.isRoot())) {
                decrypt(context, baseFile, hFile);
            }
        } else {

            if (!sourceFile.getPath().endsWith(CRYPT_EXTENSION)) {
                failedOps.add(sourceFile);
                return;
            }

            BufferedInputStream inputStream = new BufferedInputStream(sourceFile.getInputStream(context),
                    GenericCopyUtil.DEFAULT_BUFFER_SIZE);

            HFile targetFile = new HFile(targetDirectory.getMode(),
                    targetDirectory.getPath(), sourceFile.getName().replace(CRYPT_EXTENSION, ""),
                    sourceFile.isDirectory());

            if(progressHandler != null)
                progressHandler.setFileName(sourceFile.getName());

            BufferedOutputStream outputStream = new BufferedOutputStream(targetFile.getOutputStream(context),
                    GenericCopyUtil.DEFAULT_BUFFER_SIZE);

            if (progressHandler != null && progressHandler.getCancelled()) return;

            encryption.decrypt(context, inputStream, outputStream);
        }
    }

    /**
     * Wrapper around handling encryption in directory tree
     * @param context
     * @param sourceFile        the source file to encrypt
     * @param targetDirectory   the target directory in which we're going to encrypt
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private void encrypt(Context context, BaseFile sourceFile, HFile targetDirectory)
            throws IOException, GeneralSecurityException {

        if (sourceFile.isDirectory()) {

            // succeed #CRYPT_EXTENSION at end of directory/file name
            HFile hFile = new HFile(targetDirectory.getMode(),
                    targetDirectory.getPath(), sourceFile.getName() + CRYPT_EXTENSION,
                    sourceFile.isDirectory());
            FileUtil.mkdirs(context, hFile);

            for (BaseFile baseFile : sourceFile.listFiles(context, sourceFile.isRoot())) {
                encrypt(context, baseFile, hFile);
            }
        } else {

            if (sourceFile.getName().endsWith(CRYPT_EXTENSION)) {
                failedOps.add(sourceFile);
                return;
            }

            BufferedInputStream inputStream = new BufferedInputStream(sourceFile.getInputStream(context),
                    GenericCopyUtil.DEFAULT_BUFFER_SIZE);

            // succeed #CRYPT_EXTENSION at end of directory/file name
            HFile targetFile = new HFile(targetDirectory.getMode(),
                    targetDirectory.getPath(), sourceFile.getName() + CRYPT_EXTENSION,
                    sourceFile.isDirectory());
            if(progressHandler != null)
                progressHandler.setFileName(sourceFile.getName());

            BufferedOutputStream outputStream = new BufferedOutputStream(targetFile.getOutputStream(context),
                    GenericCopyUtil.DEFAULT_BUFFER_SIZE);

            if (progressHandler != null && progressHandler.getCancelled()) return;

            encryption.encrypt(context, inputStream, outputStream);
        }
    }

    /**
     * Method handles encryption of plain text on various APIs
     * @param context
     * @param plainText
     * @return
     */
    public String encryptPassword(Context context, String plainText)
            throws IOException, GeneralSecurityException {

        return encryption.encryptPassword(context, plainText);
    }

    /**
     * Method handles decryption of cipher text on various APIs
     * @param context
     * @param cipherText
     * @return
     */
    public String decryptPassword(Context context, String cipherText)
            throws IOException, GeneralSecurityException {

        return encryption.decryptPassword(context, cipherText);
    }

    /**
     * Method initializes a Cipher to be used by {@link android.hardware.fingerprint.FingerprintManager}
     * @param context
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public Cipher initCipher(Context context) throws IOException, GeneralSecurityException {
        return encryption.initCipher(context);
    }

}
