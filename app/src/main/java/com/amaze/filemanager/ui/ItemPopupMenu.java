package com.amaze.filemanager.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.asynchronous.services.EncryptService;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.PasteHelper;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.fragments.preference_fragments.PrefFrag;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.files.EncryptDecryptUtils;
import com.amaze.filemanager.utils.files.FileUtils;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;

import java.io.File;
import java.util.ArrayList;

/**
 * This class contains the functionality of the PopupMenu for each file in the MainFragment
 *
 * @author Emmanuel
 *         on 25/5/2017, at 16:39.
 */

public class ItemPopupMenu extends PopupMenu implements PopupMenu.OnMenuItemClickListener {

    private Context context;
    private MainActivity mainActivity;
    private UtilitiesProviderInterface utilitiesProvider;
    private MainFragment mainFragment;
    private SharedPreferences sharedPrefs;
    private LayoutElementParcelable rowItem;
    private int accentColor;

    public ItemPopupMenu(Context c, MainActivity ma, UtilitiesProviderInterface up, MainFragment mainFragment,
                         LayoutElementParcelable ri, View anchor, SharedPreferences sharedPreferences) {
        super(c, anchor);

        context = c;
        mainActivity = ma;
        utilitiesProvider = up;
        this.mainFragment = mainFragment;
        sharedPrefs = sharedPreferences;
        rowItem = ri;
        accentColor = mainActivity.getColorPreference().getColor(ColorUsage.ACCENT);

        setOnMenuItemClickListener(this);
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                GeneralDialogCreation.showPropertiesDialogWithPermissions((rowItem).generateBaseFile(),
                        rowItem.getPermissions(), (ThemedActivity) mainFragment.getActivity(),
                        ThemedActivity.rootMode, utilitiesProvider.getAppTheme());
                                /*
                                PropertiesSheet propertiesSheet = new PropertiesSheet();
                                Bundle arguments = new Bundle();
                                arguments.putParcelable(PropertiesSheet.KEY_FILE, rowItem.generateBaseFile());
                                arguments.putString(PropertiesSheet.KEY_PERMISSION, rowItem.getPermissions());
                                arguments.putBoolean(PropertiesSheet.KEY_ROOT, ThemedActivity.rootMode);
                                propertiesSheet.setArguments(arguments);
                                propertiesSheet.show(main.getFragmentManager(), PropertiesSheet.TAG_FRAGMENT);
                                */
                return true;
            case R.id.share:
                switch (rowItem.getMode()) {
                    case DROPBOX:
                    case BOX:
                    case GDRIVE:
                    case ONEDRIVE:
                        FileUtils.shareCloudFile(rowItem.getDesc(), rowItem.getMode(), context);
                        break;
                    default:
                        ArrayList<File> arrayList = new ArrayList<>();
                        arrayList.add(new File(rowItem.getDesc()));
                        FileUtils.shareFiles(arrayList,
                                mainFragment.getMainActivity(), utilitiesProvider.getAppTheme(),
                                accentColor);
                        break;
                }
                return true;
            case R.id.rename:
                mainFragment.rename(rowItem.generateBaseFile());
                return true;
            case R.id.cpy:
            case R.id.cut: {
                int op = item.getItemId() == R.id.cpy? PasteHelper.OPERATION_COPY:PasteHelper.OPERATION_CUT;
                PasteHelper pasteHelper = new PasteHelper(op, new HybridFileParcelable[]{rowItem.generateBaseFile()});
                mainFragment.getMainActivity().setPaste(pasteHelper);
                return true;
            }
            case R.id.ex:
                mainFragment.getMainActivity().mainActivityHelper.extractFile(new File(rowItem.getDesc()));
                return true;
            case R.id.book:
                DataUtils dataUtils = DataUtils.getInstance();
                dataUtils.addBook(new String[]{rowItem.getTitle(), rowItem.getDesc()}, true);
                mainFragment.getMainActivity().refreshDrawer();
                Toast.makeText(mainFragment.getActivity(), mainFragment.getResources().getString(R.string.bookmarksadded), Toast.LENGTH_LONG).show();
                return true;
            case R.id.delete:
                ArrayList<LayoutElementParcelable> positions = new ArrayList<>();
                positions.add(rowItem);
                GeneralDialogCreation.deleteFilesDialog(context,
                        mainFragment.getLayoutElements(),
                        mainFragment.getMainActivity(),
                        positions, utilitiesProvider.getAppTheme());
                return true;
            case R.id.open_with:
                boolean useNewStack = sharedPrefs.getBoolean(PrefFrag.PREFERENCE_TEXTEDITOR_NEWSTACK, false);
                FileUtils.openWith(new File(rowItem.getDesc()), mainFragment.getActivity(), useNewStack);
                return true;
            case R.id.encrypt:
                final Intent encryptIntent = new Intent(context, EncryptService.class);
                encryptIntent.putExtra(EncryptService.TAG_OPEN_MODE, rowItem.getMode().ordinal());
                encryptIntent.putExtra(EncryptService.TAG_CRYPT_MODE,
                        EncryptService.CryptEnum.ENCRYPT.ordinal());
                encryptIntent.putExtra(EncryptService.TAG_SOURCE, rowItem.generateBaseFile());

                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

                final EncryptDecryptUtils.EncryptButtonCallbackInterface encryptButtonCallbackInterfaceAuthenticate =
                        new EncryptDecryptUtils.EncryptButtonCallbackInterface() {
                            @Override
                            public void onButtonPressed(Intent intent) {
                            }

                            @Override
                            public void onButtonPressed(Intent intent, String password) throws Exception {
                                EncryptDecryptUtils.startEncryption(context,
                                        rowItem.generateBaseFile().getPath(), password, intent);
                            }
                        };

                EncryptDecryptUtils.EncryptButtonCallbackInterface encryptButtonCallbackInterface =
                        new EncryptDecryptUtils.EncryptButtonCallbackInterface() {

                            @Override
                            public void onButtonPressed(Intent intent) throws Exception {
                                // check if a master password or fingerprint is set
                                if (!preferences.getString(PrefFrag.PREFERENCE_CRYPT_MASTER_PASSWORD,
                                        PrefFrag.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT).equals("")) {

                                    EncryptDecryptUtils.startEncryption(context,
                                            rowItem.generateBaseFile().getPath(),
                                            PrefFrag.ENCRYPT_PASSWORD_MASTER, encryptIntent);
                                } else if (preferences.getBoolean(PrefFrag.PREFERENCE_CRYPT_FINGERPRINT,
                                        PrefFrag.PREFERENCE_CRYPT_FINGERPRINT_DEFAULT)) {

                                    EncryptDecryptUtils.startEncryption(context,
                                            rowItem.generateBaseFile().getPath(),
                                            PrefFrag.ENCRYPT_PASSWORD_FINGERPRINT, encryptIntent);
                                } else {
                                    // let's ask a password from user
                                    GeneralDialogCreation.showEncryptAuthenticateDialog(context, encryptIntent,
                                            mainFragment.getMainActivity(), utilitiesProvider.getAppTheme(),
                                            encryptButtonCallbackInterfaceAuthenticate);
                                }
                            }

                            @Override
                            public void onButtonPressed(Intent intent, String password) {
                            }
                        };

                if (preferences.getBoolean(PrefFrag.PREFERENCE_CRYPT_WARNING_REMEMBER,
                        PrefFrag.PREFERENCE_CRYPT_WARNING_REMEMBER_DEFAULT)) {
                    // let's skip warning dialog call
                    try {
                        encryptButtonCallbackInterface.onButtonPressed(encryptIntent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(context,
                                mainFragment.getResources().getString(R.string.crypt_encryption_fail),
                                Toast.LENGTH_LONG).show();
                    }
                } else {

                    GeneralDialogCreation.showEncryptWarningDialog(encryptIntent,
                            mainFragment, utilitiesProvider.getAppTheme(), encryptButtonCallbackInterface);
                }
                return true;
            case R.id.decrypt:
                EncryptDecryptUtils.decryptFile(context, mainActivity, mainFragment,
                        mainFragment.openMode, rowItem.generateBaseFile(),
                        rowItem.generateBaseFile().getParent(context), utilitiesProvider, false);
                return true;
            case R.id.return_select:
                mainFragment.returnIntentResults(rowItem.generateBaseFile());
                return true;
        }
        return false;
    }

}