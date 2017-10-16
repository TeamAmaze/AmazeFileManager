package com.amaze.filemanager.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.holders.EmptyViewHolder;
import com.amaze.filemanager.adapters.holders.ItemViewHolder;
import com.amaze.filemanager.adapters.holders.SpecialViewHolder;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.ui.ItemPopupMenu;
import com.amaze.filemanager.ui.LayoutElementParcelable;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.ui.views.CircleGradientDrawable;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.color.ColorUtils;
import com.amaze.filemanager.utils.files.CryptUtil;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.theme.AppTheme;

import java.util.ArrayList;

/**
 * This class is the information that serves to load the files into a "list" (a RecyclerView).
 * There are 3 types of item TYPE_ITEM, TYPE_HEADER_FOLDERS and TYPE_HEADER_FILES and EMPTY_LAST_ITEM
 * represeted by ItemViewHolder, SpecialViewHolder and EmptyViewHolder respectively.
 * The showPopup shows the file's popup menu.
 * The 'go to parent' aka '..' button (go to settings to activate it) is just a folder.
 *
 * Created by Arpit on 11-04-2015 edited by Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *                                edited by Jens Klingenberg <mail@jensklingenberg.de>
 */
public class RecyclerAdapter extends RecyclerArrayAdapter<String, RecyclerView.ViewHolder> {

    private static final int PICTURE_FILETYPE = 0, APK_FILETYPE = 1, VIDEO_FILETYPE = 2,
            GENERIC_FILETYPE = 3, ENCRYPTED_FILETYPE = 4;

    public static final int TYPE_ITEM = 0, TYPE_HEADER_FOLDERS = 1, TYPE_HEADER_FILES = 2, EMPTY_LAST_ITEM = 3;

    private static final int VIDEO = 0, AUDIO = 1, PDF = 2, CODE = 3, TEXT = 4, ARCHIVE = 5,
            GENERIC = 6, APK = 7, PICTURE = 8, ENCRYPTED = 9;

    public boolean stoppedAnimation = false;

    private UtilitiesProviderInterface utilsProvider;
    private MainFragment mainFrag;
    private SharedPreferences sharedPrefs;
    private boolean showHeaders;
    private ArrayList<ListItem> itemsDigested = new ArrayList<>();
    private Context context;
    private LayoutInflater mInflater;
    private float minRowHeight;
    private int grey_color, accentColor, iconSkinColor, goBackColor, videoColor, audioColor,
            pdfColor, codeColor, textColor, archiveColor, genericColor;
    private int offset = 0;

    public RecyclerAdapter(MainFragment m, UtilitiesProviderInterface utilsProvider, SharedPreferences sharedPrefs,
                           ArrayList<LayoutElementParcelable> itemsRaw, Context context, boolean showHeaders) {
        this.mainFrag = m;
        this.utilsProvider = utilsProvider;
        this.context = context;
        this.sharedPrefs = sharedPrefs;
        this.showHeaders = showHeaders;

        mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        accentColor = m.getMainActivity().getColorPreference().getColor(ColorUsage.ACCENT);
        iconSkinColor = m.getMainActivity().getColorPreference().getColor(ColorUsage.ICON_SKIN);
        goBackColor = Utils.getColor(context, R.color.goback_item);
        videoColor = Utils.getColor(context, R.color.video_item);
        audioColor = Utils.getColor(context, R.color.audio_item);
        pdfColor = Utils.getColor(context, R.color.pdf_item);
        codeColor = Utils.getColor(context, R.color.code_item);
        textColor = Utils.getColor(context, R.color.text_item);
        archiveColor = Utils.getColor(context, R.color.archive_item);
        genericColor = Utils.getColor(context, R.color.generic_item);
        minRowHeight = context.getResources().getDimension(R.dimen.minimal_row_height);
        grey_color = Utils.getColor(context, R.color.grey);

        setItems(itemsRaw, false);
    }

    /**
     * called as to toggle selection of any item in adapter
     *
     * @param position  the position of the item
     * @param imageView the check {@link CircleGradientDrawable} that is to be animated
     */
    public void toggleChecked(int position, ImageView imageView) {
        if(itemsDigested.get(position).getChecked() == ListItem.UNCHECKABLE) {
            throw new IllegalArgumentException("You have checked a header");
        }

        if (!stoppedAnimation) mainFrag.stopAnimation();
        if (itemsDigested.get(position).getChecked() == ListItem.CHECKED) {
            // if the view at position is checked, un-check it
            itemsDigested.get(position).setChecked(false);

            Animation iconAnimation = AnimationUtils.loadAnimation(context, R.anim.check_out);
            if (imageView != null) {
                imageView.setAnimation(iconAnimation);
            } else {
                // TODO: we don't have the check icon object probably because of config change
            }
        } else {
            // if view is un-checked, check it
            itemsDigested.get(position).setChecked(true);

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
                mainFrag.mActionMode = mainFrag.getMainActivity().startSupportActionMode(mainFrag.mActionModeCallback);
            }
        }

        notifyDataSetChanged();
        //notifyItemChanged(position);
        if (mainFrag.mActionMode != null && mainFrag.selection) {
            // we have the actionmode visible, invalidate it's views
            mainFrag.mActionMode.invalidate();
        }
        if (getCheckedItems().size() == 0) {
            mainFrag.selection = false;
            mainFrag.mActionMode.finish();
            mainFrag.mActionMode = null;
        }
    }

    public void toggleChecked(boolean b, String path) {
        int i = path.equals("/") || !mainFrag.GO_BACK_ITEM ? 0 : 1;

        for (; i < itemsDigested.size(); i++) {
            itemsDigested.get(i).setChecked(b);
            notifyItemChanged(i);
        }

        if (mainFrag.mActionMode != null) {
            mainFrag.mActionMode.invalidate();
        }

        if (getCheckedItems().size() == 0) {
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
        for (int i = 0; i < itemsDigested.size(); i++) {
            itemsDigested.get(i).setChecked(b);
            notifyItemChanged(i);
        }

        if (mainFrag.mActionMode != null) {
            mainFrag.mActionMode.invalidate();
        }

        if (getCheckedItems().size() == 0) {
            mainFrag.selection = false;
            if (mainFrag.mActionMode != null)
                mainFrag.mActionMode.finish();
            mainFrag.mActionMode = null;
        }
    }

    public ArrayList<LayoutElementParcelable> getCheckedItems() {
        ArrayList<LayoutElementParcelable> selected = new ArrayList<>();

        for (int i = 0; i < itemsDigested.size(); i++) {
            if (itemsDigested.get(i).getChecked() == ListItem.CHECKED) {
                selected.add(itemsDigested.get(i).elem);
            }
        }

        return selected;
    }

    public boolean areAllChecked(String path) {
        boolean allChecked = true;
        int i = (path.equals("/") || !mainFrag.GO_BACK_ITEM)? 0:1;

        for (; i < itemsDigested.size(); i++) {
            if (itemsDigested.get(i).getChecked() == ListItem.NOT_CHECKED) {
                allChecked = false;
            }
        }

        return allChecked;
    }

    public ArrayList<Integer> getCheckedItemsIndex() {
        ArrayList<Integer> checked = new ArrayList<>();

        for (int i = 0; i < itemsDigested.size(); i++) {
            if (itemsDigested.get(i).getChecked() == ListItem.CHECKED) {
                checked.add(i);
            }
        }

        return checked;
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if(holder instanceof ItemViewHolder) {
            ((ItemViewHolder) holder).rl.clearAnimation();
        }
    }

    @Override
    public boolean onFailedToRecycleView(RecyclerView.ViewHolder holder) {
        ((ItemViewHolder) holder).rl.clearAnimation();
        return super.onFailedToRecycleView(holder);
    }

    private void animate(ItemViewHolder holder) {
        holder.rl.clearAnimation();
        Animation localAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in_top);
        localAnimation.setStartOffset(this.offset);
        holder.rl.startAnimation(localAnimation);
        this.offset += 30;
    }

    /**
     * Adds item to the end of the list, don't use this unless you are dynamically loading the adapter,
     * after you are finished you must call createHeaders
     * @param e
     */
    public void addItem(LayoutElementParcelable e) {
        if (mainFrag.IS_LIST && itemsDigested.size() > 0) {
            itemsDigested.add(itemsDigested.size()-1, new ListItem(e));
        } else if(mainFrag.IS_LIST) {
            itemsDigested.add(new ListItem(e));
            itemsDigested.add(new ListItem(EMPTY_LAST_ITEM));
        } else {
            itemsDigested.add(new ListItem(e));
        }

        notifyItemInserted(getItemCount());
    }

    public void setItems(ArrayList<LayoutElementParcelable> arrayList) {
        setItems(arrayList, true);
    }

    private void setItems(ArrayList<LayoutElementParcelable> arrayList, boolean invalidate) {
        synchronized (arrayList) {
            itemsDigested.clear();
            offset = 0;
            stoppedAnimation = false;

            for (LayoutElementParcelable e : arrayList) {
                itemsDigested.add(new ListItem(e));
            }

            if (mainFrag.IS_LIST && itemsDigested.size() > 0) {
                itemsDigested.add(new ListItem(EMPTY_LAST_ITEM));
            }

            for (int i = 0; i < itemsDigested.size(); i++) {
                itemsDigested.get(i).setAnimate(false);
            }

            if(showHeaders) {
                createHeaders(invalidate);
            }
        }
    }

    public void createHeaders(boolean invalidate)  {
        boolean[] headers = new boolean[]{false, false};

        for (int i = 0; i < itemsDigested.size(); i++) {
            
                if (itemsDigested.get(i).elem != null) {
                    LayoutElementParcelable nextItem = itemsDigested.get(i).elem;

                    if (!headers[0] && nextItem.isDirectory()) {
                        headers[0] = true;
                        itemsDigested.add(i, new ListItem(TYPE_HEADER_FOLDERS));
                        continue;
                    }

                    if (!headers[1] && !nextItem.isDirectory()
                            && !nextItem.getTitle().equals(".") && !nextItem.getTitle().equals("..")) {
                        headers[1] = true;
                        itemsDigested.add(i, new ListItem(TYPE_HEADER_FILES));
                        continue;//leave this continue for symmetry
                    }
                }

        }

        if(invalidate) {
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        return itemsDigested.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(itemsDigested.get(position).specialType != -1) {
            return itemsDigested.get(position).specialType;
        } else {
            return TYPE_ITEM;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        switch (viewType) {
            case TYPE_HEADER_FOLDERS:
            case TYPE_HEADER_FILES:

                if (mainFrag.IS_LIST) {

                    view = mInflater.inflate(R.layout.list_header, parent, false);
                } else {

                    view = mInflater.inflate(R.layout.grid_header, parent, false);
                }

                int type = viewType == TYPE_HEADER_FOLDERS ? SpecialViewHolder.HEADER_FOLDERS : SpecialViewHolder.HEADER_FILES;

                return new SpecialViewHolder(context, view, utilsProvider, type);
            case TYPE_ITEM:
                if (mainFrag.IS_LIST) view = mInflater.inflate(R.layout.rowlayout, parent, false);
                else view = mInflater.inflate(R.layout.griditem, parent, false);

                return new ItemViewHolder(view);
            case EMPTY_LAST_ITEM:
                int totalFabHeight = (int) context.getResources().getDimension(R.dimen.fab_height),
                        marginFab = (int) context.getResources().getDimension(R.dimen.fab_margin);
                view = new View(context);
                view.setMinimumHeight(totalFabHeight + marginFab);
                return new EmptyViewHolder(view);
            default:
                throw new IllegalArgumentException("Illegal: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder vholder, int p) {
        if(vholder instanceof ItemViewHolder) {
            final ItemViewHolder holder = (ItemViewHolder) vholder;
            final boolean isBackButton = mainFrag.GO_BACK_ITEM && p == 0;

            if (mainFrag.IS_LIST) {
                if (p == getItemCount() - 1) {
                    holder.rl.setMinimumHeight((int) minRowHeight);
                    if (itemsDigested.size() == (mainFrag.GO_BACK_ITEM ? 1 : 0))
                        holder.txtTitle.setText(R.string.nofiles);
                    else holder.txtTitle.setText("");
                    return;
                }
            }
            if (!this.stoppedAnimation && !itemsDigested.get(p).getAnimating()) {
                animate(holder);
                itemsDigested.get(p).setAnimate(true);
            }
            final LayoutElementParcelable rowItem = itemsDigested.get(p).elem;
            if (mainFrag.IS_LIST) {
                holder.rl.setOnClickListener(v -> {
                    mainFrag.onListItemClicked(isBackButton, vholder.getAdapterPosition(), rowItem,
                            holder.checkImageView);
                });

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    holder.checkImageView.setBackground(new CircleGradientDrawable(accentColor,
                            utilsProvider.getAppTheme(), mainFrag.getResources().getDisplayMetrics()));
                } else {
                    holder.checkImageView.setBackgroundDrawable(new CircleGradientDrawable(accentColor,
                            utilsProvider.getAppTheme(), mainFrag.getResources().getDisplayMetrics()));
                }

                holder.rl.setOnLongClickListener(p1 -> {
                    // check if the item on which action is performed is not the first {goback} item
                    if (!isBackButton) {
                        toggleChecked(vholder.getAdapterPosition(), holder.checkImageView);
                    }

                    return true;
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
                holder.genericIcon.setOnClickListener(v -> {
                    int id = v.getId();
                    if (id == R.id.generic_icon || id == R.id.picture_icon || id == R.id.apk_icon) {
                        // TODO: transform icon on press to the properties dialog with animation
                        if (!isBackButton) {
                            toggleChecked(vholder.getAdapterPosition(), holder.checkImageView);
                        } else mainFrag.goBack();
                    }
                });

                holder.pictureIcon.setOnClickListener(view -> {
                    if (!isBackButton) {
                        toggleChecked(vholder.getAdapterPosition(), holder.checkImageView);
                    } else mainFrag.goBack();
                });

                holder.apkIcon.setOnClickListener(view -> {
                    if (!isBackButton) {
                        toggleChecked(vholder.getAdapterPosition(), holder.checkImageView);
                    } else mainFrag.goBack();
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
                                mainFrag.iconHolder.cancelLoad(holder.pictureIcon);
                                mainFrag.iconHolder.loadDrawable(holder.pictureIcon, (rowItem.getDesc()), null);
                            } else {
                                holder.apkIcon.setVisibility(View.VISIBLE);
                                holder.apkIcon.setImageDrawable(mainFrag.DARK_IMAGE);
                                mainFrag.iconHolder.cancelLoad(holder.apkIcon);
                                mainFrag.iconHolder.loadDrawable(holder.apkIcon, (rowItem.getDesc()), null);
                            }
                        }
                        break;
                    case APK_FILETYPE:
                        if (mainFrag.SHOW_THUMBS) {
                            holder.genericIcon.setVisibility(View.GONE);
                            holder.pictureIcon.setVisibility(View.GONE);
                            holder.apkIcon.setVisibility(View.VISIBLE);
                            holder.apkIcon.setImageDrawable(mainFrag.apk);
                            mainFrag.iconHolder.cancelLoad(holder.apkIcon);
                            mainFrag.iconHolder.loadDrawable(holder.apkIcon, (rowItem.getDesc()), null);
                        }
                        break;
                    case VIDEO_FILETYPE:
                        if (mainFrag.SHOW_THUMBS) {
                            holder.genericIcon.setVisibility(View.GONE);
                            if (mainFrag.CIRCULAR_IMAGES) {
                                holder.pictureIcon.setVisibility(View.VISIBLE);
                                holder.pictureIcon.setImageDrawable(mainFrag.DARK_VIDEO);
                                mainFrag.iconHolder.cancelLoad(holder.pictureIcon);
                                mainFrag.iconHolder.loadDrawable(holder.pictureIcon, (rowItem.getDesc()), null);
                            } else {
                                holder.apkIcon.setVisibility(View.VISIBLE);
                                holder.apkIcon.setImageDrawable(mainFrag.DARK_VIDEO);
                                mainFrag.iconHolder.cancelLoad(holder.apkIcon);
                                mainFrag.iconHolder.loadDrawable(holder.apkIcon, (rowItem.getDesc()), null);
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
                if (itemsDigested.get(p).getChecked() == ListItem.CHECKED) {
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
                            gradientDrawable.setColor(iconSkinColor);
                        } else {
                            ColorUtils.colorizeIcons(context, Icons.getTypeOfFile(rowItem.getDesc()),
                                    gradientDrawable, iconSkinColor);
                        }
                    } else gradientDrawable.setColor(iconSkinColor);

                    if (isBackButton)
                        gradientDrawable.setColor(goBackColor);
                }
                if (mainFrag.SHOW_PERMISSIONS)
                    holder.perm.setText(rowItem.getPermissions());
                if (mainFrag.SHOW_LAST_MODIFIED) {
                    holder.date.setText(rowItem.getDate());
                } else {
                    holder.date.setVisibility(View.GONE);
                }

                if (isBackButton) {
                    holder.date.setText(rowItem.getSize());
                    holder.txtDesc.setText("");
                } else if (mainFrag.SHOW_SIZE) {
                    holder.txtDesc.setText(rowItem.getSize());
                }
            } else {
                // view is a grid view
                holder.checkImageViewGrid.setColorFilter(accentColor);
                holder.rl.setOnClickListener(v -> {
                    mainFrag.onListItemClicked(isBackButton, vholder.getAdapterPosition(), rowItem,
                            holder.checkImageViewGrid);
                });

                holder.rl.setOnLongClickListener(p1 -> {
                    if (!isBackButton) {
                        toggleChecked(vholder.getAdapterPosition(), holder.checkImageViewGrid);
                    }
                    return true;
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
                    mainFrag.iconHolder.cancelLoad(holder.imageView1);
                    mainFrag.iconHolder.loadDrawable(holder.imageView1, (rowItem.getDesc()), null);
                } else if (Icons.isApk((rowItem.getDesc()))) {
                    holder.genericIcon.setColorFilter(null);
                    mainFrag.iconHolder.cancelLoad(holder.genericIcon);
                    mainFrag.iconHolder.loadDrawable(holder.genericIcon, (rowItem.getDesc()), null);
                }

                if (rowItem.isDirectory()) {
                    holder.genericIcon.setColorFilter(iconSkinColor);
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
                            holder.genericIcon.setColorFilter(iconSkinColor);
                            break;
                    }
                }

                if (isBackButton)
                    holder.genericIcon.setColorFilter(goBackColor);

                if (itemsDigested.get(p).getChecked() == ListItem.CHECKED) {
                    holder.genericIcon.setColorFilter(iconSkinColor);
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
                if (isBackButton) {
                    holder.date.setText(rowItem.getSize());
                    holder.txtDesc.setText("");
                }/*else if(main.SHOW_SIZE)
                holder.txtDesc.setText(rowItem.getSize());
           */
                if (mainFrag.SHOW_PERMISSIONS)
                    holder.perm.setText(rowItem.getPermissions());
            }
        }
    }

    private void showPopup(View v, final LayoutElementParcelable rowItem, final int position) {
        v.setOnClickListener(view -> {
            PopupMenu popupMenu = new ItemPopupMenu(context, mainFrag.getMainActivity(),
                    utilsProvider, mainFrag, rowItem, view, sharedPrefs);
            popupMenu.inflate(R.menu.item_extras);
            String description = rowItem.getDesc().toLowerCase();

            if (rowItem.isDirectory()) {
                popupMenu.getMenu().findItem(R.id.open_with).setVisible(false);
                popupMenu.getMenu().findItem(R.id.share).setVisible(false);

                if (mainFrag.getMainActivity().mReturnIntent) {
                    popupMenu.getMenu().findItem(R.id.return_select).setVisible(true);
                }
            } else {
                popupMenu.getMenu().findItem(R.id.book).setVisible(false);
            }

            if (description.endsWith(".zip") || description.endsWith(".jar")
                    || description.endsWith(".apk") || description.endsWith(".rar")
                    || description.endsWith(".tar") || description.endsWith(".tar.gz"))
                popupMenu.getMenu().findItem(R.id.ex).setVisible(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                if (description.endsWith(CryptUtil.CRYPT_EXTENSION))
                    popupMenu.getMenu().findItem(R.id.decrypt).setVisible(true);
                else popupMenu.getMenu().findItem(R.id.encrypt).setVisible(true);
            }

            popupMenu.show();
        });
    }

    private static class ListItem {
        public static final int CHECKED = 0, NOT_CHECKED = 1, UNCHECKABLE = 2;

        private LayoutElementParcelable elem;
        private int specialType;
        private boolean checked;
        private boolean animate;

        ListItem(LayoutElementParcelable elem) {
            this.elem = elem;
            specialType = TYPE_ITEM;
        }

        ListItem(int specialType) {
            this.specialType = specialType;
        }

        public void setChecked(boolean checked) {
            if(specialType == TYPE_ITEM) this.checked = checked;
        }

        public int getChecked() {
            if(checked) return CHECKED;
            else if(specialType == TYPE_ITEM) return NOT_CHECKED;
            else return UNCHECKABLE;
        }

        public void setAnimate(boolean animating) {
            if(specialType == -1) this.animate = animating;
        }

        public boolean getAnimating() {
            return animate;
        }
    }

}
