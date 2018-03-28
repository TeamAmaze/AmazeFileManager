package com.amaze.filemanager.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.amaze.filemanager.GlideApp;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.superclasses.PreferenceActivity;
import com.amaze.filemanager.adapters.data.IconDataParcelable;
import com.amaze.filemanager.adapters.data.LayoutElementParcelable;
import com.amaze.filemanager.adapters.glide.RecyclerPreloadModelProvider;
import com.amaze.filemanager.adapters.glide.RecyclerPreloadSizeProvider;
import com.amaze.filemanager.adapters.holders.EmptyViewHolder;
import com.amaze.filemanager.adapters.holders.ItemViewHolder;
import com.amaze.filemanager.adapters.holders.SpecialViewHolder;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.ItemPopupMenu;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.ui.views.CircleGradientDrawable;
import com.amaze.filemanager.ui.views.RoundedImageView;
import com.amaze.filemanager.utils.GlideConstants;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.color.ColorUtils;
import com.amaze.filemanager.utils.files.CryptUtil;
import com.amaze.filemanager.utils.provider.UtilitiesProvider;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.List;

import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_COLORIZE_ICONS;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_FILE_SIZE;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_GOBACK_BUTTON;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_HEADERS;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_LAST_MODIFIED;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_PERMISSIONS;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_THUMB;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_USE_CIRCULAR_IMAGES;

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
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements RecyclerPreloadSizeProvider.RecyclerPreloadSizeProviderCallback {

    public static final int TYPE_ITEM = 0, TYPE_HEADER_FOLDERS = 1, TYPE_HEADER_FILES = 2, EMPTY_LAST_ITEM = 3;

    private static final int VIEW_GENERIC = 0, VIEW_PICTURE = 1, VIEW_APK = 2, VIEW_THUMB = 3;

    public boolean stoppedAnimation = false;

    private PreferenceActivity preferenceActivity;
    private UtilitiesProvider utilsProvider;
    private MainFragment mainFrag;
    private SharedPreferences sharedPrefs;
    private RecyclerViewPreloader<IconDataParcelable> preloader;
    private RecyclerPreloadSizeProvider sizeProvider;
    private RecyclerPreloadModelProvider modelProvider;
    private ArrayList<ListItem> itemsDigested = new ArrayList<>();
    private Context context;
    private LayoutInflater mInflater;
    private float minRowHeight;
    private int grey_color, accentColor, iconSkinColor, goBackColor, videoColor, audioColor,
            pdfColor, codeColor, textColor, archiveColor, genericColor;
    private int offset = 0;

    public RecyclerAdapter(PreferenceActivity preferenceActivity, MainFragment m,
                           UtilitiesProvider utilsProvider, SharedPreferences sharedPrefs,
                           RecyclerView recyclerView,  ArrayList<LayoutElementParcelable> itemsRaw,
                           Context context) {
        setHasStableIds(true);

        this.preferenceActivity = preferenceActivity;
        this.mainFrag = m;
        this.utilsProvider = utilsProvider;
        this.context = context;
        this.sharedPrefs = sharedPrefs;

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

        setItems(recyclerView, itemsRaw, false);

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
        int i = path.equals("/") || !getBoolean(PREFERENCE_SHOW_GOBACK_BUTTON) ? 0 : 1;

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
        int i = (path.equals("/") || !getBoolean(PREFERENCE_SHOW_GOBACK_BUTTON))? 0:1;

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

    public void setItems(RecyclerView recyclerView, ArrayList<LayoutElementParcelable> arrayList) {
        setItems(recyclerView, arrayList, true);
    }

    private void setItems(RecyclerView recyclerView, ArrayList<LayoutElementParcelable> arrayList, boolean invalidate) {
        if(preloader != null)  {
            recyclerView.removeOnScrollListener(preloader);
            preloader = null;
        }

        itemsDigested.clear();
        offset = 0;
        stoppedAnimation = false;

        ArrayList<IconDataParcelable> uris = new ArrayList<>(itemsDigested.size());

        for (LayoutElementParcelable e : arrayList) {
            itemsDigested.add(new ListItem(e));
            uris.add(e != null? e.iconData:null);
        }

        if (mainFrag.IS_LIST && itemsDigested.size() > 0) {
            itemsDigested.add(new ListItem(EMPTY_LAST_ITEM));
            uris.add(null);
        }

        for (int i = 0; i < itemsDigested.size(); i++) {
            itemsDigested.get(i).setAnimate(false);
        }

        if (getBoolean(PREFERENCE_SHOW_HEADERS)) {
            createHeaders(invalidate, uris);
        }

        sizeProvider = new RecyclerPreloadSizeProvider(this);
        modelProvider = new RecyclerPreloadModelProvider(mainFrag, uris,
                getBoolean(PREFERENCE_SHOW_THUMB));

        preloader = new RecyclerViewPreloader<>(GlideApp.with(mainFrag), modelProvider, sizeProvider, GlideConstants.MAX_PRELOAD_FILES);

        recyclerView.addOnScrollListener(preloader);
    }

    public void createHeaders(boolean invalidate, List<IconDataParcelable> uris)  {
        boolean[] headers = new boolean[]{false, false};

        for (int i = 0; i < itemsDigested.size(); i++) {
            
                if (itemsDigested.get(i).elem != null) {
                    LayoutElementParcelable nextItem = itemsDigested.get(i).elem;

                    if (!headers[0] && nextItem.isDirectory) {
                        headers[0] = true;
                        itemsDigested.add(i, new ListItem(TYPE_HEADER_FOLDERS));
                        uris.add(i, null);
                        continue;
                    }

                    if (!headers[1] && !nextItem.isDirectory
                            && !nextItem.title.equals(".") && !nextItem.title.equals("..")) {
                        headers[1] = true;
                        itemsDigested.add(i, new ListItem(TYPE_HEADER_FILES));
                        uris.add(i, null);
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
    public long getItemId(int position) {
        return position;
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
                if (mainFrag.IS_LIST) {
                    view = mInflater.inflate(R.layout.rowlayout, parent, false);
                    sizeProvider.addView(VIEW_GENERIC, view.findViewById(R.id.generic_icon));
                    sizeProvider.addView(VIEW_PICTURE, view.findViewById(R.id.picture_icon));
                    sizeProvider.addView(VIEW_APK, view.findViewById(R.id.apk_icon));
                } else {
                    view = mInflater.inflate(R.layout.griditem, parent, false);
                    sizeProvider.addView(VIEW_GENERIC, view.findViewById(R.id.generic_icon));
                    sizeProvider.addView(VIEW_THUMB, view.findViewById(R.id.icon_thumb));
                }

                sizeProvider.closeOffAddition();

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
        if (vholder instanceof ItemViewHolder) {
            final ItemViewHolder holder = (ItemViewHolder) vholder;
            final boolean isBackButton = getBoolean(PREFERENCE_SHOW_GOBACK_BUTTON) && p == 0;
            if(isBackButton){
                holder.about.setVisibility(View.GONE);
            }
            if (mainFrag.IS_LIST) {
                if (p == getItemCount() - 1) {
                    holder.rl.setMinimumHeight((int) minRowHeight);
                    if (itemsDigested.size() == (getBoolean(PREFERENCE_SHOW_GOBACK_BUTTON)? 1:0))
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

                holder.txtTitle.setText(rowItem.title);
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
                holder.genericIcon.setVisibility(View.GONE);
                holder.pictureIcon.setVisibility(View.GONE);
                holder.apkIcon.setVisibility(View.GONE);
                holder.checkImageView.setVisibility(View.INVISIBLE);

                // setting icons for various cases
                // apkIcon holder refers to square/non-circular drawable
                // pictureIcon is circular drawable
                switch (rowItem.filetype) {
                    case Icons.IMAGE:
                    case Icons.VIDEO:
                        if (getBoolean(PREFERENCE_SHOW_THUMB)) {
                            if (getBoolean(PREFERENCE_USE_CIRCULAR_IMAGES)) {
                                showThumbnailWithBackground(holder, rowItem.iconData, holder.pictureIcon,
                                        rowItem.iconData::setImageBroken);
                            } else {
                                showThumbnailWithBackground(holder, rowItem.iconData, holder.apkIcon,
                                        rowItem.iconData::setImageBroken);
                            }
                        }
                        break;
                    case Icons.APK:
                        if (getBoolean(PREFERENCE_SHOW_THUMB)) {
                            showThumbnailWithBackground(holder, rowItem.iconData, holder.apkIcon,
                                    rowItem.iconData::setImageBroken);
                        }
                        break;
                    case Icons.NOT_KNOWN:
                        holder.genericIcon.setVisibility(View.VISIBLE);
                        // if the file type is any unknown variable
                        String ext = !rowItem.isDirectory? MimeTypes.getExtension(rowItem.title):null;
                        if (ext != null && ext.trim().length() != 0) {
                            holder.genericText.setText(ext);
                            holder.genericIcon.setImageDrawable(null);
                            //holder.genericIcon.setVisibility(View.INVISIBLE);
                        } else {
                            // we could not find the extension, set a generic file type icon probably a directory
                            modelProvider.getPreloadRequestBuilder(rowItem.iconData).into(holder.genericIcon);
                        }
                        break;
                    case Icons.ENCRYPTED:
                        if (getBoolean(PREFERENCE_SHOW_THUMB)) {
                            holder.genericIcon.setVisibility(View.VISIBLE);
                            modelProvider.getPreloadRequestBuilder(rowItem.iconData).into(holder.genericIcon);
                        }
                        break;
                    default:
                        holder.genericIcon.setVisibility(View.VISIBLE);
                        modelProvider.getPreloadRequestBuilder(rowItem.iconData).into(holder.genericIcon);
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
                    if ((rowItem.filetype != Icons.IMAGE && rowItem.filetype != Icons.APK && rowItem.filetype != Icons.VIDEO)) {
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

                    if(!rowItem.iconData.isImageBroken()) {
                        GradientDrawable gradientDrawable = (GradientDrawable) holder.genericIcon.getBackground();
                        if (getBoolean(PREFERENCE_COLORIZE_ICONS)) {
                            if (rowItem.isDirectory) {
                                gradientDrawable.setColor(iconSkinColor);
                            } else {
                                ColorUtils.colorizeIcons(context, rowItem.filetype,
                                        gradientDrawable, iconSkinColor);
                            }
                        } else gradientDrawable.setColor(iconSkinColor);

                        if (isBackButton) {
                            gradientDrawable.setColor(goBackColor);
                        }
                    }
                }
                if (getBoolean(PREFERENCE_SHOW_PERMISSIONS))
                    holder.perm.setText(rowItem.permissions);
                if (getBoolean(PREFERENCE_SHOW_LAST_MODIFIED)) {
                    holder.date.setText(rowItem.date1);
                } else {
                    holder.date.setVisibility(View.GONE);
                }

                if (isBackButton) {
                    holder.date.setText(rowItem.size);
                    holder.txtDesc.setText("");
                } else if (getBoolean(PREFERENCE_SHOW_FILE_SIZE)) {
                    holder.txtDesc.setText(rowItem.size);
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
                holder.txtTitle.setText(rowItem.title);
                holder.imageView1.setVisibility(View.INVISIBLE);
                holder.genericIcon.setVisibility(View.VISIBLE);
                holder.checkImageViewGrid.setVisibility(View.INVISIBLE);

                ((RoundedImageView) holder.genericIcon).setRelativeSize(1.25f, 1.25f);
                GlideApp.with(mainFrag).load(rowItem.iconData.image).into(holder.genericIcon);

                if (rowItem.filetype == Icons.IMAGE || rowItem.filetype == Icons.VIDEO) {
                    holder.imageView1.setVisibility(View.VISIBLE);
                    holder.imageView1.setImageDrawable(null);
                    if (utilsProvider.getAppTheme().equals(AppTheme.DARK) || utilsProvider.getAppTheme().equals(AppTheme.BLACK))
                        holder.imageView1.setBackgroundColor(Color.BLACK);
                    showRoundedThumbnail(holder, rowItem.iconData, holder.imageView1, rowItem.iconData::setImageBroken);
                } else if (rowItem.filetype == Icons.APK) {
                    showRoundedThumbnail(holder, rowItem.iconData, holder.genericIcon, rowItem.iconData::setImageBroken);
                }

                if(holder.genericIcon.getVisibility() == View.VISIBLE) {
                    View iconBackground = getBoolean(PREFERENCE_USE_CIRCULAR_IMAGES)? holder.genericIcon:holder.iconLayout;

                    if (rowItem.isDirectory) {
                        iconBackground.setBackgroundColor(iconSkinColor);
                    } else {
                        switch (rowItem.filetype) {
                            case Icons.VIDEO:
                                iconBackground.setBackgroundColor(videoColor);
                                break;
                            case Icons.AUDIO:
                                iconBackground.setBackgroundColor(audioColor);
                                break;
                            case Icons.PDF:
                                iconBackground.setBackgroundColor(pdfColor);
                                break;
                            case Icons.CODE:
                                iconBackground.setBackgroundColor(codeColor);
                                break;
                            case Icons.TEXT:
                                iconBackground.setBackgroundColor(textColor);
                                break;
                            case Icons.COMPRESSED:
                                iconBackground.setBackgroundColor(archiveColor);
                                break;
                            case Icons.NOT_KNOWN:
                                iconBackground.setBackgroundColor(genericColor);
                                break;
                            case Icons.APK:
                            case Icons.IMAGE:
                                //While these are loading or if they fail, color has to be set (done in showRoundedThumbnail()).
                                break;
                            default:
                                iconBackground.setBackgroundColor(iconSkinColor);
                                break;
                        }
                    }

                    if (isBackButton) {
                        iconBackground.setBackgroundColor(goBackColor);
                    }
                }



                if (itemsDigested.get(p).getChecked() == ListItem.CHECKED) {
                    if (holder.genericIcon.getVisibility() == View.VISIBLE) {
                        View iconBackground = getBoolean(PREFERENCE_USE_CIRCULAR_IMAGES)? holder.genericIcon:holder.iconLayout;

                        iconBackground.setBackgroundColor(iconSkinColor);
                        //holder.genericIcon.setImageDrawable(main.getResources().getDrawable(R.drawable.abc_ic_cab_done_holo_dark));
                    }

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
                if (getBoolean(PREFERENCE_SHOW_LAST_MODIFIED))
                    holder.date.setText(rowItem.date1);
                if (isBackButton) {
                    holder.date.setText(rowItem.size);
                    holder.txtDesc.setText("");
                }/*else if(main.SHOW_SIZE)
                holder.txtDesc.setText(rowItem.getSize());
           */
                if (getBoolean(PREFERENCE_SHOW_PERMISSIONS))
                    holder.perm.setText(rowItem.permissions);
            }
        }
    }

    @Override
    public int getCorrectView(IconDataParcelable item, int adapterPosition) {
        if (mainFrag.IS_LIST) {
            if(getBoolean(PREFERENCE_SHOW_THUMB)) {
                int filetype = itemsDigested.get(adapterPosition).elem.filetype;

                if (filetype == Icons.VIDEO || filetype == Icons.IMAGE) {
                    if (getBoolean(PREFERENCE_USE_CIRCULAR_IMAGES)) {
                        return VIEW_PICTURE;
                    } else {
                        return VIEW_APK;
                    }
                } else if (filetype == Icons.APK) {
                    return VIEW_APK;
                }
            }

            return VIEW_GENERIC;
        } else {
            if (item.type == IconDataParcelable.IMAGE_FROMFILE) {
                return VIEW_THUMB;
            } else {
                return VIEW_GENERIC;
            }
        }
    }

    private void showThumbnailWithBackground(ItemViewHolder viewHolder, IconDataParcelable iconData,
                                             ImageView view, OnImageProcessed errorListener) {
        if(iconData.isImageBroken()) {
            viewHolder.genericIcon.setVisibility(View.VISIBLE);
            GlideApp.with(mainFrag).load(R.drawable.ic_broken_image_white_24dp).into(viewHolder.genericIcon);
            GradientDrawable gradientDrawable = (GradientDrawable) viewHolder.genericIcon.getBackground();
            gradientDrawable.setColor(grey_color);

            errorListener.onImageProcessed(true);
            return;
        }

        viewHolder.genericIcon.setVisibility(View.VISIBLE);
        GlideApp.with(mainFrag).load(iconData.loadingImage).into(viewHolder.genericIcon);
        GradientDrawable gradientDrawable = (GradientDrawable) viewHolder.genericIcon.getBackground();
        gradientDrawable.setColor(iconSkinColor);

        modelProvider.getPreloadRequestBuilder(iconData).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
                GlideApp.with(mainFrag).load(R.drawable.ic_broken_image_white_24dp).into(viewHolder.genericIcon);
                gradientDrawable.setColor(grey_color);

                errorListener.onImageProcessed(true);
                return true;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
                                           DataSource dataSource, boolean isFirstResource) {
                viewHolder.genericIcon.setImageDrawable(null);
                viewHolder.genericIcon.setVisibility(View.GONE);
                view.setVisibility(View.VISIBLE);

                errorListener.onImageProcessed(false);
                return false;
            }
        }).into(view);
    }

    private void showRoundedThumbnail(ItemViewHolder viewHolder, IconDataParcelable iconData,
                                      ImageView view, OnImageProcessed errorListener) {
        if(iconData.isImageBroken()) {
            View iconBackground = getBoolean(PREFERENCE_USE_CIRCULAR_IMAGES)? viewHolder.genericIcon:viewHolder.iconLayout;

            viewHolder.genericIcon.setVisibility(View.VISIBLE);
            iconBackground.setBackgroundColor(grey_color);
            GlideApp.with(mainFrag).load(R.drawable.ic_broken_image_white_24dp).into(viewHolder.genericIcon);
            view.setVisibility(View.INVISIBLE);

            errorListener.onImageProcessed(true);
            return;
        }

        View iconBackground = getBoolean(PREFERENCE_USE_CIRCULAR_IMAGES)? viewHolder.genericIcon:viewHolder.iconLayout;

        iconBackground.setBackgroundColor(iconSkinColor);
        viewHolder.genericIcon.setVisibility(View.VISIBLE);
        GlideApp.with(mainFrag).load(iconData.loadingImage).into(viewHolder.genericIcon);
        view.setVisibility(View.INVISIBLE);

        modelProvider.getPreloadRequestBuilder(iconData).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
                iconBackground.setBackgroundColor(grey_color);
                GlideApp.with(mainFrag).load(R.drawable.ic_broken_image_white_24dp).into(viewHolder.genericIcon);
                errorListener.onImageProcessed(true);
                return true;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
                                           DataSource dataSource, boolean isFirstResource) {
                viewHolder.genericIcon.setImageDrawable(null);
                viewHolder.genericIcon.setVisibility(View.GONE);
                view.setVisibility(View.VISIBLE);
                errorListener.onImageProcessed(false);
                return false;
            }
        }).into(view);
    }

    private void showPopup(View v, final LayoutElementParcelable rowItem, final int position) {
        v.setOnClickListener(view -> {
            PopupMenu popupMenu = new ItemPopupMenu(context, mainFrag.getMainActivity(),
                    utilsProvider, mainFrag, rowItem, view, sharedPrefs);
            popupMenu.inflate(R.menu.item_extras);
            String description = rowItem.desc.toLowerCase();

            if (rowItem.isDirectory) {
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

    private boolean getBoolean(String key) {
        return preferenceActivity.getBoolean(key);
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

    private interface OnImageProcessed {
        void onImageProcessed(boolean isImageBroken);
    }

}
