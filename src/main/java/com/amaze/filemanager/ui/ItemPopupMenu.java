package com.amaze.filemanager.ui;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.encryption.EncryptFunctions;
import com.amaze.filemanager.filesystem.encryption.EncryptionManager;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.files.Futils;
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
    private LayoutElement rowItem;
    private int accentColor;
    private EncryptFunctions encryption;

    public ItemPopupMenu(Context c, MainActivity ma, UtilitiesProviderInterface up, MainFragment mainFragment,
                         LayoutElement ri, View anchor, EncryptFunctions encrypt) {
        super(c, anchor);

        context = c;
        mainActivity = ma;
        utilitiesProvider = up;
        this.mainFragment = mainFragment;
        rowItem = ri;
        accentColor = mainActivity.getColorPreference().getColor(ColorUsage.ACCENT);
        encryption = encrypt;

        setOnMenuItemClickListener(this);
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                GeneralDialogCreation.showPropertiesDialogWithPermissions((rowItem).generateBaseFile(),
                        rowItem.getPermissions(), (BaseActivity) mainFragment.getActivity(),
                        BaseActivity.rootMode, utilitiesProvider.getAppTheme());
                                /*
                                PropertiesSheet propertiesSheet = new PropertiesSheet();
                                Bundle arguments = new Bundle();
                                arguments.putParcelable(PropertiesSheet.KEY_FILE, rowItem.generateBaseFile());
                                arguments.putString(PropertiesSheet.KEY_PERMISSION, rowItem.getPermissions());
                                arguments.putBoolean(PropertiesSheet.KEY_ROOT, BaseActivity.rootMode);
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
                        utilitiesProvider.getFutils().shareCloudFile(rowItem.getDesc(), rowItem.getMode(), context);
                        break;
                    default:
                        ArrayList<File> arrayList = new ArrayList<>();
                        arrayList.add(new File(rowItem.getDesc()));
                        utilitiesProvider.getFutils().shareFiles(arrayList,
                                mainFragment.getMainActivity(), utilitiesProvider.getAppTheme(),
                                accentColor);
                        break;
                }
                return true;
            case R.id.rename:
                mainFragment.rename(rowItem.generateBaseFile());
                return true;
            case R.id.cpy:
                mainFragment.getMainActivity().MOVE_PATH = null;
                ArrayList<BaseFile> copies = new ArrayList<>();
                copies.add(rowItem.generateBaseFile());
                mainFragment.getMainActivity().COPY_PATH = copies;
                mainFragment.getMainActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.cut:
                mainFragment.getMainActivity().COPY_PATH = null;
                ArrayList<BaseFile> copie = new ArrayList<>();
                copie.add(rowItem.generateBaseFile());
                mainFragment.getMainActivity().MOVE_PATH = copie;
                mainFragment.getMainActivity().supportInvalidateOptionsMenu();
                return true;
            case R.id.ex:
                mainFragment.getMainActivity().mainActivityHelper.extractFile(new File(rowItem.getDesc()));
                return true;
            case R.id.book:
                MainActivity.dataUtils.addBook(new String[]{rowItem.getTitle(), rowItem.getDesc()}, true);
                mainFragment.getMainActivity().refreshDrawer();
                Toast.makeText(mainFragment.getActivity(), mainFragment.getResources().getString(R.string.bookmarksadded), Toast.LENGTH_LONG).show();
                return true;
            case R.id.delete:
                ArrayList<LayoutElement> positions = new ArrayList<>();
                positions.add(rowItem);
                GeneralDialogCreation.deleteFilesDialog(context,
                        mainFragment.getLayoutElements(),
                        mainFragment.getMainActivity(),
                        positions, utilitiesProvider.getAppTheme());
                return true;
            case R.id.open_with:
                Futils.openWith(new File(rowItem.getDesc()), mainFragment.getActivity());
                return true;
            case R.id.encrypt:
                EncryptionManager.encryptFile(context, mainFragment, utilitiesProvider,
                        rowItem.getMode(), rowItem.generateBaseFile(), encryption);
                return true;
            case R.id.decrypt:
                EncryptionManager.decryptFile(context, mainActivity, mainFragment,
                        mainFragment.openMode, rowItem.generateBaseFile(),
                        rowItem.generateBaseFile().getParent(context), utilitiesProvider, encryption);
                return true;
        }
        return false;
    }

}