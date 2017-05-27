package com.amaze.filemanager.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.database.EncryptedEntry;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.fragments.preference_fragments.Preffrag;
import com.amaze.filemanager.services.EncryptService;
import com.amaze.filemanager.ui.LayoutElement;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.ui.views.CircleGradientDrawable;
import com.amaze.filemanager.ui.views.RoundedImageView;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.color.ColorUtils;
import com.amaze.filemanager.utils.files.CryptUtil;
import com.amaze.filemanager.utils.files.Futils;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Arpit on 11-04-2015 edited by Emmanuel Messulam <emmanuelbendavid@gmail.com>
 */
public class RecyclerAdapter extends RecyclerArrayAdapter<String, RecyclerView.ViewHolder>
        implements StickyRecyclerHeadersAdapter<RecyclerView.ViewHolder> {

    private static final int PICTURE_FILETYPE = 0, APK_FILETYPE = 1, VIDEO_FILETYPE = 2,
            GENERIC_FILETYPE = 3, ENCRYPTED_FILETYPE = 4;

    private static final int TYPE_HEADER = 0, TYPE_ITEM = 1;

    private static final long FILE = 'F', DIRECTORY = 'D';

    private static final int VIDEO = 0, AUDIO = 1, PDF = 2, CODE = 3, TEXT = 4, ARCHIVE = 5,
            GENERIC = 6, APK = 7, PICTURE = 8, ENCRYPTED = 9;

    private UtilitiesProviderInterface utilsProvider;

    private MainFragment mainFrag;
    private ArrayList<LayoutElement> items;
    private Context context;
    private SparseBooleanArray checkedItems = new SparseBooleanArray();
    private SparseBooleanArray animation = new SparseBooleanArray();
    private LayoutInflater mInflater;
    private int rowHeight;
    private int grey_color;
    private int goBackColor, videoColor, audioColor, pdfColor, codeColor, textColor, archiveColor, apkColor, genericColor;

    private int offset = 0;
    public boolean stoppedAnimation = false;

    public RecyclerAdapter(MainFragment m, UtilitiesProviderInterface utilsProvider, ArrayList<LayoutElement> items, Context context) {
        this.mainFrag = m;
        this.utilsProvider = utilsProvider;
        this.items = items;
        this.context = context;
        for (int i = 0; i < items.size(); i++) {
            checkedItems.put(i, false);
            animation.put(i, false);
        }
        mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        goBackColor = Utils.getColor(context, R.color.goback_item);
        videoColor = Utils.getColor(context, R.color.video_item);
        audioColor = Utils.getColor(context, R.color.audio_item);
        pdfColor = Utils.getColor(context, R.color.pdf_item);
        codeColor = Utils.getColor(context, R.color.code_item);
        textColor = Utils.getColor(context, R.color.text_item);
        archiveColor = Utils.getColor(context, R.color.archive_item);
        apkColor = Utils.getColor(context, R.color.apk_item);
        genericColor = Utils.getColor(context, R.color.generic_item);
        rowHeight = Utils.dpToPx(100, context);// TODO: 23/5/2017 don't hardcode pixel measurements
        grey_color = Utils.getColor(context, R.color.grey);
    }

    public void addItem() {
        //notifyDataSetChanged();
        notifyItemInserted(getItemCount());
    }

    /**
     * called as to toggle selection of any item in adapter
     *
     * @param position  the position of the item
     * @param imageView the check {@link CircleGradientDrawable} that is to be animated
     */
    public void toggleChecked(int position, ImageView imageView) {
        if (!stoppedAnimation) mainFrag.stopAnimation();
        if (checkedItems.get(position)) {
            // if the view at position is checked, un-check it
            checkedItems.put(position, false);

            Animation iconAnimation = AnimationUtils.loadAnimation(context, R.anim.check_out);
            if (imageView != null) {
                imageView.setAnimation(iconAnimation);
            } else {
                // TODO: we don't have the check icon object probably because of config change
            }
        } else {
            // if view is un-checked, check it
            checkedItems.put(position, true);

            Animation iconAnimation = AnimationUtils.loadAnimation(context, R.anim.check_in);
            if (imageView != null) {
                imageView.setAnimation(iconAnimation);
            } else {
                // TODO: we don't have the check icon object probably because of config change
            }
            if (mainFrag.mActionMode == null || !mainFrag.selection) {
                // start actionmode if not already started
                // null condition if there is config change
                mainFrag.selection = true;
                mainFrag.mActionMode = mainFrag.MAIN_ACTIVITY.startSupportActionMode(mainFrag.mActionModeCallback);
            }
        }

        notifyDataSetChanged();
        //notifyItemChanged(position);
        if (mainFrag.mActionMode != null && mainFrag.selection) {
            // we have the actionmode visible, invalidate it's views
            mainFrag.mActionMode.invalidate();
        }
        if (getCheckedItemPositions().size() == 0) {
            mainFrag.selection = false;
            mainFrag.mActionMode.finish();
            mainFrag.mActionMode = null;
        }
    }

    public void toggleChecked(boolean b, String path) {
        int i = path.equals("/") || !mainFrag.GO_BACK_ITEM ? 0 : 1;

        for (; i < items.size(); i++) {
            checkedItems.put(i, b);
            notifyItemChanged(i);
        }

        if (mainFrag.mActionMode != null) {
            mainFrag.mActionMode.invalidate();
        }

        if (getCheckedItemPositions().size() == 0) {
            mainFrag.selection = false;
            if (mainFrag.mActionMode != null) {
                mainFrag.mActionMode.finish();
            }
            mainFrag.mActionMode = null;
        }
    }

    /**
     * called when we would want to toggle check for all items in the adapter
     *
     * @param b if to toggle true or false
     */
    public void toggleChecked(boolean b) {
        for (int i = 0; i < items.size(); i++) {
            checkedItems.put(i, b);
            notifyItemChanged(i);
        }

        if (mainFrag.mActionMode != null) {
            mainFrag.mActionMode.invalidate();
        }

        if (getCheckedItemPositions().size() == 0) {
            mainFrag.selection = false;
            if (mainFrag.mActionMode != null)
                mainFrag.mActionMode.finish();
            mainFrag.mActionMode = null;
        }
    }

    public ArrayList<Integer> getCheckedItemPositions() {
        ArrayList<Integer> checkedItemPositions = new ArrayList<>();

        for (int i = 0; i < checkedItems.size(); i++) {
            if (checkedItems.get(i)) {
                (checkedItemPositions).add(i);
            }
        }

        return checkedItemPositions;
    }

    public boolean areAllChecked(String path) {
        boolean allChecked = true;
        int i;
        if (path.equals("/") || !mainFrag.GO_BACK_ITEM) i = 0;
        else i = 1;
        for (; i < checkedItems.size(); i++) {
            if (!checkedItems.get(i)) {
                allChecked = false;
            }
        }
        return allChecked;
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        RoundedImageView pictureIcon;
        ImageView genericIcon, apkIcon;
        ImageView imageView1;
        TextView txtTitle;
        TextView txtDesc;
        public TextView date;
        TextView perm;
        View rl;
        TextView genericText;
        public ImageButton about;
        ImageView checkImageView;
        ImageView checkImageViewGrid;

        ViewHolder(View view) {
            super(view);

            txtTitle = (TextView) view.findViewById(R.id.firstline);
            pictureIcon = (RoundedImageView) view.findViewById(R.id.picture_icon);
            rl = view.findViewById(R.id.second);
            perm = (TextView) view.findViewById(R.id.permis);
            date = (TextView) view.findViewById(R.id.date);
            txtDesc = (TextView) view.findViewById(R.id.secondLine);
            apkIcon = (ImageView) view.findViewById(R.id.apk_icon);
            genericText = (TextView) view.findViewById(R.id.generictext);
            imageView1 = (ImageView) view.findViewById(R.id.icon_thumb);
            about = (ImageButton) view.findViewById(R.id.properties);
            checkImageView = (ImageView) view.findViewById(R.id.check_icon);
            genericIcon = (ImageView) view.findViewById(R.id.generic_icon);
            checkImageViewGrid = (ImageView) view.findViewById(R.id.check_icon_grid);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View v = mInflater.inflate(R.layout.list_footer, parent, false);
            return new ViewHolder(v);
        }
        View v;
        if (mainFrag.IS_LIST) v = mInflater.inflate(R.layout.rowlayout, parent, false);
        else v = mInflater.inflate(R.layout.griditem, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        ((ViewHolder) holder).rl.clearAnimation();
    }

    @Override
    public boolean onFailedToRecycleView(RecyclerView.ViewHolder holder) {
        ((ViewHolder) holder).rl.clearAnimation();
        return super.onFailedToRecycleView(holder);
    }

    private void animate(RecyclerAdapter.ViewHolder holder) {
        holder.rl.clearAnimation();
        Animation localAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in_top);
        localAnimation.setStartOffset(this.offset);
        holder.rl.startAnimation(localAnimation);
        this.offset += 30;
    }

    public void generate(ArrayList<LayoutElement> arrayList) {
        synchronized (arrayList) {
            offset = 0;
            stoppedAnimation = false;
            notifyDataSetChanged();
            items = arrayList;
            for (int i = 0; i < items.size(); i++) {
                checkedItems.put(i, false);
                animation.put(i, false);
            }
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder vholder, int p) {
        final ViewHolder holder = ((ViewHolder) vholder);
        if (mainFrag.IS_LIST) {
            if (p == getItemCount() - 1) {
                holder.rl.setMinimumHeight(rowHeight);
                if (items.size() == (mainFrag.GO_BACK_ITEM ? 1 : 0))
                    holder.txtTitle.setText(R.string.nofiles);
                else holder.txtTitle.setText("");
                return;
            }
        }
        if (!this.stoppedAnimation && !animation.get(p)) {
            animate(holder);
            animation.put(p, true);
        }
        final LayoutElement rowItem = items.get(p);
        if (mainFrag.IS_LIST) {
            holder.rl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mainFrag.onListItemClicked(vholder.getAdapterPosition(), holder.checkImageView);
                }
            });

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                holder.checkImageView.setBackground(new CircleGradientDrawable(mainFrag.fabSkin,
                        utilsProvider.getAppTheme(), mainFrag.getResources().getDisplayMetrics()));
            } else {
                holder.checkImageView.setBackgroundDrawable(new CircleGradientDrawable(mainFrag.fabSkin,
                        utilsProvider.getAppTheme(), mainFrag.getResources().getDisplayMetrics()));
            }

            holder.rl.setOnLongClickListener(new View.OnLongClickListener() {

                public boolean onLongClick(View p1) {
                    // check if the item on which action is performed is not the first {goback} item
                    if (!rowItem.getSize().equals(mainFrag.goback)) {
                        toggleChecked(vholder.getAdapterPosition(), holder.checkImageView);
                    }

                    return true;
                }
            });

            int filetype = -1;

            switch (Icons.getTypeOfFile(rowItem.getDesc())) {
                case PICTURE:
                    filetype = PICTURE_FILETYPE;
                    break;
                case APK:
                    filetype = APK_FILETYPE;
                    break;
                case VIDEO:
                    filetype = VIDEO_FILETYPE;
                    break;
                case ENCRYPTED:
                    if (!rowItem.isDirectory()) filetype = ENCRYPTED_FILETYPE;
                    break;
                case GENERIC:
                    filetype = GENERIC_FILETYPE;
                    break;
            }

            holder.txtTitle.setText(rowItem.getTitle());
            holder.genericIcon.setImageDrawable(rowItem.getImageId());
            holder.genericText.setText("");

            if (holder.about != null) {
                if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT))
                    holder.about.setColorFilter(grey_color);
                showPopup(holder.about, rowItem, p);
            }
            holder.genericIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int id = v.getId();
                    if (id == R.id.generic_icon || id == R.id.picture_icon || id == R.id.apk_icon) {
                        // TODO: transform icon on press to the properties dialog with animation
                        if (!rowItem.getSize().equals(mainFrag.goback)) {
                            toggleChecked(vholder.getAdapterPosition(), holder.checkImageView);
                        } else mainFrag.goBack();
                    }
                }
            });

            holder.pictureIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!rowItem.getSize().equals(mainFrag.goback)) {
                        toggleChecked(vholder.getAdapterPosition(), holder.checkImageView);
                    } else mainFrag.goBack();
                }
            });

            holder.apkIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!rowItem.getSize().equals(mainFrag.goback)) {
                        toggleChecked(vholder.getAdapterPosition(), holder.checkImageView);
                    } else mainFrag.goBack();
                }
            });

            // resetting icons visibility
            holder.genericIcon.setVisibility(View.VISIBLE);
            holder.pictureIcon.setVisibility(View.INVISIBLE);
            holder.apkIcon.setVisibility(View.INVISIBLE);
            holder.checkImageView.setVisibility(View.INVISIBLE);

            // setting icons for various cases
            // apkIcon holder refers to square/non-circular drawable
            // pictureIcon is circular drawable
            switch (filetype) {
                case PICTURE_FILETYPE:
                    if (mainFrag.SHOW_THUMBS) {
                        holder.genericIcon.setVisibility(View.GONE);

                        if (mainFrag.CIRCULAR_IMAGES) {
                            holder.apkIcon.setVisibility(View.GONE);
                            holder.pictureIcon.setVisibility(View.VISIBLE);
                            holder.pictureIcon.setImageDrawable(mainFrag.DARK_IMAGE);
                            mainFrag.ic.cancelLoad(holder.pictureIcon);
                            mainFrag.ic.loadDrawable(holder.pictureIcon, (rowItem.getDesc()), null);
                        } else {
                            holder.apkIcon.setVisibility(View.VISIBLE);
                            holder.apkIcon.setImageDrawable(mainFrag.DARK_IMAGE);
                            mainFrag.ic.cancelLoad(holder.apkIcon);
                            mainFrag.ic.loadDrawable(holder.apkIcon, (rowItem.getDesc()), null);
                        }
                    }
                    break;
                case APK_FILETYPE:
                    if (mainFrag.SHOW_THUMBS) {
                        holder.genericIcon.setVisibility(View.GONE);
                        holder.pictureIcon.setVisibility(View.GONE);
                        holder.apkIcon.setVisibility(View.VISIBLE);
                        holder.apkIcon.setImageDrawable(mainFrag.apk);
                        mainFrag.ic.cancelLoad(holder.apkIcon);
                        mainFrag.ic.loadDrawable(holder.apkIcon, (rowItem.getDesc()), null);
                    }
                    break;
                case VIDEO_FILETYPE:
                    if (mainFrag.SHOW_THUMBS) {
                        holder.genericIcon.setVisibility(View.GONE);
                        if (mainFrag.CIRCULAR_IMAGES) {
                            holder.pictureIcon.setVisibility(View.VISIBLE);
                            holder.pictureIcon.setImageDrawable(mainFrag.DARK_VIDEO);
                            mainFrag.ic.cancelLoad(holder.pictureIcon);
                            mainFrag.ic.loadDrawable(holder.pictureIcon, (rowItem.getDesc()), null);
                        } else {
                            holder.apkIcon.setVisibility(View.VISIBLE);
                            holder.apkIcon.setImageDrawable(mainFrag.DARK_VIDEO);
                            mainFrag.ic.cancelLoad(holder.apkIcon);
                            mainFrag.ic.loadDrawable(holder.apkIcon, (rowItem.getDesc()), null);
                        }
                    }
                    break;
                case GENERIC_FILETYPE:
                    // if the file type is any unknown variable
                    String ext = !rowItem.isDirectory() ? MimeTypes.getExtension(rowItem.getTitle()) : null;
                    if (ext != null && ext.trim().length() != 0) {
                        holder.genericText.setText(ext);
                        holder.genericIcon.setImageDrawable(null);
                        //holder.genericIcon.setVisibility(View.INVISIBLE);
                    } else {

                        // we could not find the extension, set a generic file type icon
                        // probably a directory
                        holder.genericIcon.setVisibility(View.VISIBLE);
                    }
                    holder.pictureIcon.setVisibility(View.GONE);
                    holder.apkIcon.setVisibility(View.GONE);
                    break;
                case ENCRYPTED_FILETYPE:
                    Bitmap lockBitmap = BitmapFactory.decodeResource(mainFrag.getResources(),
                            R.drawable.ic_file_lock_white_36dp);
                    BitmapDrawable lockBitmapDrawable = new BitmapDrawable(mainFrag.getResources(), lockBitmap);

                    if (mainFrag.SHOW_THUMBS) {
                        holder.genericIcon.setVisibility(View.VISIBLE);
                        holder.pictureIcon.setVisibility(View.GONE);
                        holder.apkIcon.setVisibility(View.GONE);
                        holder.genericIcon.setImageDrawable(lockBitmapDrawable);
                        //main.ic.cancelLoad(holder.apkIcon);
                        //main.ic.loadDrawable(holder.apkIcon, (rowItem.getDesc()), null);
                    }
                    break;
                default:
                    holder.pictureIcon.setVisibility(View.GONE);
                    holder.apkIcon.setVisibility(View.GONE);
                    holder.genericIcon.setVisibility(View.VISIBLE);
                    break;
            }

            if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {
                holder.rl.setBackgroundResource(R.drawable.safr_ripple_white);
            } else {
                holder.rl.setBackgroundResource(R.drawable.safr_ripple_black);
            }
            holder.rl.setSelected(false);
            if (checkedItems.get(p)) {
                holder.checkImageView.setVisibility(View.VISIBLE);
                // making sure the generic icon background color filter doesn't get changed
                // to grey on picture/video/apk/generic text icons when checked
                // so that user can still look at the thumbs even after selection
                if ((filetype != PICTURE_FILETYPE && filetype != APK_FILETYPE
                        && filetype != VIDEO_FILETYPE)) {
                    holder.apkIcon.setVisibility(View.GONE);
                    holder.pictureIcon.setVisibility(View.GONE);
                    holder.genericIcon.setVisibility(View.VISIBLE);
                    GradientDrawable gradientDrawable = (GradientDrawable) holder.genericIcon.getBackground();
                    gradientDrawable.setColor(goBackColor);
                }
                holder.rl.setSelected(true);
                //holder.genericText.setText("");
            } else {
                holder.checkImageView.setVisibility(View.INVISIBLE);
                GradientDrawable gradientDrawable = (GradientDrawable) holder.genericIcon.getBackground();
                if (mainFrag.COLORISE_ICONS) {
                    if (rowItem.isDirectory()) {
                        gradientDrawable.setColor(mainFrag.icon_skin_color);
                    } else {
                        ColorUtils.colorizeIcons(context, Icons.getTypeOfFile(rowItem.getDesc()),
                                gradientDrawable, mainFrag.icon_skin_color);
                    }
                } else gradientDrawable.setColor((mainFrag.icon_skin_color));

                if (rowItem.getSize().equals(mainFrag.goback))
                    gradientDrawable.setColor(goBackColor);
            }
            if (mainFrag.SHOW_PERMISSIONS)
                holder.perm.setText(rowItem.getPermissions());
            if (mainFrag.SHOW_LAST_MODIFIED) {
                holder.date.setText(rowItem.getDate());
            } else {
                holder.date.setVisibility(View.GONE);
            }
            String size = rowItem.getSize();

            if (size.equals(mainFrag.goback)) {

                holder.date.setText(size);

                holder.txtDesc.setText("");
            } else if (mainFrag.SHOW_SIZE)

                holder.txtDesc.setText(rowItem.getSize());
        } else {
            // view is a grid view
            Boolean checked = checkedItems.get(p);

            holder.checkImageViewGrid.setColorFilter(Color.parseColor(mainFrag.fabSkin));
            holder.rl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mainFrag.onListItemClicked(vholder.getAdapterPosition(), holder.checkImageViewGrid);
                }
            });

            holder.rl.setOnLongClickListener(new View.OnLongClickListener() {

                public boolean onLongClick(View p1) {
                    if (!rowItem.getSize().equals(mainFrag.goback)) {
                        toggleChecked(vholder.getAdapterPosition(), holder.checkImageViewGrid);
                    }
                    return true;
                }
            });
            holder.txtTitle.setText(rowItem.getTitle());
            holder.imageView1.setVisibility(View.INVISIBLE);
            holder.genericIcon.setVisibility(View.VISIBLE);
            holder.checkImageViewGrid.setVisibility(View.INVISIBLE);
            holder.genericIcon.setImageDrawable(rowItem.getImageId());

            if (Icons.isPicture((rowItem.getDesc().toLowerCase())) || Icons.isVideo(rowItem.getDesc().toLowerCase())) {
                holder.genericIcon.setColorFilter(null);
                holder.imageView1.setVisibility(View.VISIBLE);
                holder.imageView1.setImageDrawable(null);
                if (utilsProvider.getAppTheme().equals(AppTheme.DARK))
                    holder.imageView1.setBackgroundColor(Color.BLACK);
                mainFrag.ic.cancelLoad(holder.imageView1);
                mainFrag.ic.loadDrawable(holder.imageView1, (rowItem.getDesc()), null);
            } else if (Icons.isApk((rowItem.getDesc()))) {
                holder.genericIcon.setColorFilter(null);
                mainFrag.ic.cancelLoad(holder.genericIcon);
                mainFrag.ic.loadDrawable(holder.genericIcon, (rowItem.getDesc()), null);
            }

            if (rowItem.isDirectory()) {
                holder.genericIcon.setColorFilter(mainFrag.icon_skin_color);
            } else {
                switch (Icons.getTypeOfFile(rowItem.getDesc())) {
                    case VIDEO:
                        holder.genericIcon.setColorFilter(videoColor);
                        break;
                    case AUDIO:
                        holder.genericIcon.setColorFilter(audioColor);
                        break;
                    case PDF:
                        holder.genericIcon.setColorFilter(pdfColor);
                        break;
                    case CODE:
                        holder.genericIcon.setColorFilter(codeColor);
                        break;
                    case TEXT:
                        holder.genericIcon.setColorFilter(textColor);
                        break;
                    case ARCHIVE:
                        holder.genericIcon.setColorFilter(archiveColor);
                        break;
                    case GENERIC:
                        holder.genericIcon.setColorFilter(genericColor);
                        break;
                    case APK:
                    case PICTURE:
                        holder.genericIcon.setColorFilter(null);
                        break;
                    default:
                        holder.genericIcon.setColorFilter(mainFrag.icon_skin_color);
                        break;
                }
            }

            if (rowItem.getSize().equals(mainFrag.goback))
                holder.genericIcon.setColorFilter(goBackColor);

            if (checked) {
                holder.genericIcon.setColorFilter(mainFrag.icon_skin_color);
                //holder.genericIcon.setImageDrawable(main.getResources().getDrawable(R.drawable.abc_ic_cab_done_holo_dark));

                holder.checkImageViewGrid.setVisibility(View.VISIBLE);
                holder.rl.setBackgroundColor(Utils.getColor(context, R.color.item_background));
            } else {
                holder.checkImageViewGrid.setVisibility(View.INVISIBLE);
                if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT))
                    holder.rl.setBackgroundResource(R.drawable.item_doc_grid);
                else {
                    holder.rl.setBackgroundResource(R.drawable.ic_grid_card_background_dark);
                    holder.rl.findViewById(R.id.icon_frame).setBackgroundColor(Utils.getColor(context, R.color.icon_background_dark));
                }
            }

            if (holder.about != null) {
                if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT))
                    holder.about.setColorFilter(grey_color);
                showPopup(holder.about, rowItem, p);
            }
            if (mainFrag.SHOW_LAST_MODIFIED)
                holder.date.setText(rowItem.getDate());
            if (rowItem.getSize().equals(mainFrag.goback)) {
                holder.date.setText(rowItem.getSize());
                holder.txtDesc.setText("");
            }/*else if(main.SHOW_SIZE)
                holder.txtDesc.setText(rowItem.getSize());
           */
            if (mainFrag.SHOW_PERMISSIONS)
                holder.perm.setText(rowItem.getPermissions());
        }
    }

    @Override
    public long getHeaderId(int i) {
        if (items.size() == 0 || !mainFrag.SHOW_HEADERS) return -1;
        if (i >= 0 && i < items.size())
            if (mainFrag.IS_LIST) {
                if (i != items.size()) {
                    if (items.get(i).getSize().equals(mainFrag.goback)) return -1;
                    if (items.get(i).isDirectory()) return DIRECTORY;
                    else return FILE;
                }
            }
        return -1;
    }

    @Override
    public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup viewGroup, int i) {
        View view = mInflater.inflate(R.layout.listheader, viewGroup, false);
        /*if(utilsProvider.getAppTheme().equals(AppTheme.DARK))
            view.setBackgroundResource(R.color.holo_dark_background);*/
        HeaderViewHolder holder = new HeaderViewHolder(view);
        if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT))
            holder.headerText.setTextColor(Utils.getColor(context, R.color.text_light));
        else holder.headerText.setTextColor(Utils.getColor(context, R.color.text_dark));
        return holder;
    }

    @Override
    public int getItemViewType(int position) {
        if (mainFrag.IS_LIST && (position == items.size()))
            return TYPE_HEADER;

        return TYPE_ITEM;
    }

    private void showPopup(View v, final LayoutElement rowItem, final int position) {
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(mainFrag.getActivity(), view);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.about:
                                GeneralDialogCreation.showPropertiesDialogWithPermissions((rowItem).generateBaseFile(),
                                        rowItem.getPermissions(), (BaseActivity) mainFrag.getActivity(),
                                        BaseActivity.rootMode, utilsProvider.getAppTheme());
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
                                        utilsProvider.getFutils().shareCloudFile(rowItem.getDesc(), rowItem.getMode(), context);
                                        break;
                                    default:
                                        ArrayList<File> arrayList = new ArrayList<>();
                                        arrayList.add(new File(rowItem.getDesc()));
                                        utilsProvider.getFutils().shareFiles(arrayList, mainFrag.MAIN_ACTIVITY, utilsProvider.getAppTheme(), Color.parseColor(mainFrag.fabSkin));
                                        break;
                                }
                                return true;
                            case R.id.rename:
                                mainFrag.rename(rowItem.generateBaseFile());
                                return true;
                            case R.id.cpy:
                                MainActivity MAIN_ACTIVITY = mainFrag.MAIN_ACTIVITY;
                                mainFrag.MAIN_ACTIVITY.MOVE_PATH = null;
                                ArrayList<BaseFile> copies = new ArrayList<>();
                                copies.add(rowItem.generateBaseFile());
                                MAIN_ACTIVITY.COPY_PATH = copies;
                                MAIN_ACTIVITY.supportInvalidateOptionsMenu();
                                return true;
                            case R.id.cut:
                                MainActivity MAIN_ACTIVITY1 = mainFrag.MAIN_ACTIVITY;
                                MAIN_ACTIVITY1.COPY_PATH = null;
                                ArrayList<BaseFile> copie = new ArrayList<>();
                                copie.add(rowItem.generateBaseFile());
                                MAIN_ACTIVITY1.MOVE_PATH = copie;
                                MAIN_ACTIVITY1.supportInvalidateOptionsMenu();
                                return true;
                            case R.id.ex:
                                mainFrag.MAIN_ACTIVITY.mainActivityHelper.extractFile(new File(rowItem.getDesc()));
                                return true;
                            case R.id.book:
                                MainActivity.dataUtils.addBook(new String[]{rowItem.getTitle(), rowItem.getDesc()}, true);
                                mainFrag.MAIN_ACTIVITY.refreshDrawer();
                                Toast.makeText(mainFrag.getActivity(), mainFrag.getResources().getString(R.string.bookmarksadded), Toast.LENGTH_LONG).show();
                                return true;
                            case R.id.delete:
                                ArrayList<Integer> positions = new ArrayList<>();
                                positions.add(position);
                                GeneralDialogCreation.deleteFilesDialog(mainFrag.getLayoutElements(), mainFrag, positions, utilsProvider.getAppTheme());
                                return true;
                            case R.id.open_with:
                                Futils.openWith(new File(rowItem.getDesc()), mainFrag.getActivity());
                                return true;
                            case R.id.encrypt:
                                final Intent encryptIntent = new Intent(context, EncryptService.class);
                                encryptIntent.putExtra(EncryptService.TAG_OPEN_MODE, rowItem.getMode().ordinal());
                                encryptIntent.putExtra(EncryptService.TAG_CRYPT_MODE,
                                        EncryptService.CryptEnum.ENCRYPT.ordinal());
                                encryptIntent.putExtra(EncryptService.TAG_SOURCE, rowItem.generateBaseFile());

                                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

                                final EncryptButtonCallbackInterface encryptButtonCallbackInterfaceAuthenticate =
                                        new EncryptButtonCallbackInterface() {
                                            @Override
                                            public void onButtonPressed(Intent intent) {
                                            }

                                            @Override
                                            public void onButtonPressed(Intent intent, String password) throws Exception {
                                                startEncryption(rowItem.generateBaseFile().getPath(), password, intent);
                                            }
                                        };

                                EncryptButtonCallbackInterface encryptButtonCallbackInterface =
                                        new EncryptButtonCallbackInterface() {

                                            @Override
                                            public void onButtonPressed(Intent intent) throws Exception {
                                                // check if a master password or fingerprint is set
                                                if (!preferences.getString(Preffrag.PREFERENCE_CRYPT_MASTER_PASSWORD,
                                                        Preffrag.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT).equals("")) {

                                                    startEncryption(rowItem.generateBaseFile().getPath(),
                                                            Preffrag.ENCRYPT_PASSWORD_MASTER, encryptIntent);
                                                } else if (preferences.getBoolean(Preffrag.PREFERENCE_CRYPT_FINGERPRINT,
                                                        Preffrag.PREFERENCE_CRYPT_FINGERPRINT_DEFAULT)) {

                                                    startEncryption(rowItem.generateBaseFile().getPath(),
                                                            Preffrag.ENCRYPT_PASSWORD_FINGERPRINT, encryptIntent);
                                                } else {
                                                    // let's ask a password from user
                                                    GeneralDialogCreation.showEncryptAuthenticateDialog(encryptIntent,
                                                            mainFrag, utilsProvider.getAppTheme(),
                                                            encryptButtonCallbackInterfaceAuthenticate);
                                                }
                                            }

                                            @Override
                                            public void onButtonPressed(Intent intent, String password) {
                                            }
                                        };

                                if (preferences.getBoolean(Preffrag.PREFERENCE_CRYPT_WARNING_REMEMBER,
                                        Preffrag.PREFERENCE_CRYPT_WARNING_REMEMBER_DEFAULT)) {
                                    // let's skip warning dialog call
                                    try {
                                        encryptButtonCallbackInterface.onButtonPressed(encryptIntent);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Toast.makeText(context,
                                                mainFrag.getResources().getString(R.string.crypt_encryption_fail),
                                                Toast.LENGTH_LONG).show();
                                    }
                                } else {

                                    GeneralDialogCreation.showEncryptWarningDialog(encryptIntent,
                                            mainFrag, utilsProvider.getAppTheme(), encryptButtonCallbackInterface);
                                }
                                return true;
                            case R.id.decrypt:
                                MainFragment.decryptFile(mainFrag, mainFrag.openMode, rowItem.generateBaseFile(),
                                        rowItem.generateBaseFile().getParent(context),
                                        utilsProvider);
                                return true;
                        }
                        return false;
                    }
                });

                popupMenu.inflate(R.menu.item_extras);
                String description = rowItem.getDesc().toLowerCase();

                if (rowItem.isDirectory()) {
                    popupMenu.getMenu().findItem(R.id.open_with).setVisible(false);
                    popupMenu.getMenu().findItem(R.id.share).setVisible(false);
                } else {
                    popupMenu.getMenu().findItem(R.id.book).setVisible(false);
                }

                if (description.endsWith(".zip") || description.endsWith(".jar")
                        || description.endsWith(".apk") || description.endsWith(".rar")
                        || description.endsWith(".tar") || description.endsWith(".tar.gz"))
                    popupMenu.getMenu().findItem(R.id.ex).setVisible(true);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (description.endsWith(CryptUtil.CRYPT_EXTENSION))
                        popupMenu.getMenu().findItem(R.id.decrypt).setVisible(true);
                    else popupMenu.getMenu().findItem(R.id.encrypt).setVisible(true);
                }

                popupMenu.show();
            }
        });
    }

    /**
     * Queries database to map path and password.
     * Starts the encryption process after database query
     *
     * @param path     the path of file to encrypt
     * @param password the password in plaintext
     */
    private void startEncryption(final String path, final String password, Intent intent) throws Exception {
        CryptHandler cryptHandler = new CryptHandler(context);
        EncryptedEntry encryptedEntry = new EncryptedEntry(path.concat(CryptUtil.CRYPT_EXTENSION),
                password);
        cryptHandler.addEntry(encryptedEntry);

        // start the encryption process
        ServiceWatcherUtil.runService(mainFrag.getContext(), intent);
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        if (i < getItemCount() - 1) {
            HeaderViewHolder holder = (HeaderViewHolder) viewHolder;
            if (items.get(i).isDirectory()) holder.headerText.setText(R.string.directories);
            else holder.headerText.setText(R.string.files);
        }
    }

    @Override
    public int getItemCount() {
        return mainFrag.IS_LIST ? items.size() + 1 : items.size();
    }

    @Override
    public int getNumColumns() {
        return 1;
    }

    @Override
    public int getSpanSize(int i) {
        return 1;
    }

    public interface EncryptButtonCallbackInterface {

        /**
         * Callback fired when we've just gone through warning dialog before encryption
         *
         * @param intent
         * @throws Exception
         */
        void onButtonPressed(Intent intent) throws Exception;

        /**
         * Callback fired when user has entered a password for encryption
         * Not called when we've a master password set or enable fingerprint authentication
         *
         * @param intent
         * @param password the password entered by user
         * @throws Exception
         */
        void onButtonPressed(Intent intent, String password) throws Exception;
    }

    public interface DecryptButtonCallbackInterface {
        /**
         * Callback fired when we've confirmed the password matches the database
         *
         * @param intent
         */
        void confirm(Intent intent);

        /**
         * Callback fired when password doesn't match the value entered by user
         */
        void failed();
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerText;

        HeaderViewHolder(View view) {
            super(view);
            headerText = (TextView) view.findViewById(R.id.headertext);
        }
    }

}