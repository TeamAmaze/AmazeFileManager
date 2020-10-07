package com.amaze.filemanager.ui.dialogs;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.ssh.SshConnectionPool;
import com.amaze.filemanager.shadows.ShadowMultiDex;
import com.amaze.filemanager.ui.activities.AbstractMainActivityTest;
import com.amaze.filemanager.utils.Utils;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.common.SecurityUtils;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowStorageManager;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

import static android.os.Build.VERSION_CODES.KITKAT;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@RunWith(AndroidJUnit4.class)
@Config(minSdk = KITKAT,
        shadows = {
                ShadowMultiDex.class,
                ShadowStorageManager.class
})
@Ignore("Pending SSH infra classes refactoring")
public class SftpConnectDialogTest extends AbstractMainActivityTest {

    private static KeyPair hostKeyPair, userKeyPair;

    private static KeyProvider sshKeyProvider;

    @BeforeClass
    public static void bootstrap() throws Exception {
        hostKeyPair = com.amaze.filemanager.filesystem.ssh.test.TestUtils.createKeyPair();
        userKeyPair = com.amaze.filemanager.filesystem.ssh.test.TestUtils.createKeyPair();
        sshKeyProvider = new KeyProvider() {
            @Override
            public PrivateKey getPrivate() throws IOException {
                return userKeyPair.getPrivate();
            }

            @Override
            public PublicKey getPublic() throws IOException {
                return userKeyPair.getPublic();
            }

            @Override
            public KeyType getType() throws IOException {
                return KeyType.RSA;
            }

            @Override
            public boolean equals(@Nullable Object obj) {
                if(obj == null || !(obj instanceof KeyProvider))
                    return false;
                else {
                    KeyProvider other = (KeyProvider)obj;
                    try {
                        return other.getPrivate().equals(getPrivate()) && other.getPublic().equals(getPublic());
                    } catch (IOException shallNeverHappenHere) { return false; }

                }
            }
        };
        RxJavaPlugins.reset();
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
    }

    @Test
    public void testCreateConnectionDialog() {
        scenario.onActivity(activity -> {
            SftpConnectDialog dialog = new SftpConnectDialog();
            dialog.setArguments(new Bundle());
            dialog.show(activity.getFragmentManager(), "sftpdialog");

            ShadowLooper.idleMainLooper();

            MaterialDialog md = (MaterialDialog)dialog.getDialog();

            View view = md.getView();
            EditText connectionET = view.findViewById(R.id.connectionET);
            EditText addressET = view.findViewById(R.id.ipET);
            EditText portET = view.findViewById(R.id.portET);
            EditText defaultPathET = view.findViewById(R.id.defaultPathET);
            EditText usernameET = view.findViewById(R.id.usernameET);
            EditText passwordET = view.findViewById(R.id.passwordET);
            Button selectPemBTN = view.findViewById(R.id.selectPemBTN);

            connectionET.setText("Test connection");
            addressET.setText("1.2.3.4");
            portET.setText("22");
            usernameET.setText("user");
            passwordET.setText("password");


            md.getActionButton(DialogAction.POSITIVE).callOnClick();

//            dialog.dismissAllowingStateLoss();
        });
    }

    private SSHClient createSshServer(@NonNull String validUsername, @Nullable String validPassword)
            throws IOException {

        SSHClient mock = mock(SSHClient.class);
        doNothing().when(mock).connect("1.2.3.4", 22);
        doNothing().when(mock).addHostKeyVerifier(SecurityUtils.getFingerprint(hostKeyPair.getPublic()));
        doNothing().when(mock).disconnect();
        if(!Utils.isNullOrEmpty(validPassword)) {
            doNothing().when(mock).authPassword(validUsername, validPassword);
            doThrow(new UserAuthException("Invalid login/password")).when(mock).authPassword(not(eq(validUsername)), not(eq(validPassword)));
        }
        else {
            doNothing().when(mock).authPublickey(validUsername, sshKeyProvider);
            doThrow(new UserAuthException("Invalid key")).when(mock).authPublickey(not(eq(validUsername)), eq(sshKeyProvider));
        }
        //reset(mock);
        SshConnectionPool.setSSHClientFactory(config -> mock);
        return mock;
    }
}
