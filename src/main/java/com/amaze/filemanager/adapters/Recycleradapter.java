package com.amaze.filemanager.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
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
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.ui.Layoutelements;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.ui.views.CircleGradientDrawable;
import com.amaze.filemanager.ui.views.RoundedImageView;
import com.amaze.filemanager.utils.DataUtils;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Arpit on 11-04-2015.
 */
public class Recycleradapter extends RecyclerArrayAdapter<String, RecyclerView.ViewHolder>
        implements StickyRecyclerHeadersAdapter<RecyclerView.ViewHolder> {
    Main main;
    ArrayList<Layoutelements> items;
    Context context;
    private SparseBooleanArray myChecked = new SparseBooleanArray();
    private SparseBooleanArray myanim = new SparseBooleanArray();
    ColorMatrixColorFilter colorMatrixColorFilter;
    LayoutInflater mInflater;
    int filetype=-1;
    int column,rowHeight;
    boolean topFab;
    int grey_color;
    int c1,c2,c3,c4,c5,c6,c7,c8,c9,anim;

    public Recycleradapter(Main m, ArrayList<Layoutelements> items, Context context){
        this.main=m;
        this.items=items;
        this.context=context;
        for (int i = 0; i < items.size(); i++) {
            myChecked.put(i, false);
            myanim.put(i,false);
        }
        colorMatrixColorFilter=main.colorMatrixColorFilter;
        mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        c1=Color.parseColor("#757575");
        c2=Color.parseColor("#f06292");
        c3=Color.parseColor("#9575cd");
        c4=Color.parseColor("#da4336");
        c5=Color.parseColor("#00bfa5");
        c6=Color.parseColor("#e06055");
        c7=Color.parseColor("#f9a825");
        c8=Color.parseColor("#a4c439");
        c9=Color.parseColor("#9e9e9e");
        column=main.columns;
        rowHeight=main.dpToPx(100);
        grey_color=Color.parseColor("#ff666666");
        anim = /*main.IS_LIST?R.anim.fade_in_top:*/R.anim.fade_in_top;
    }

    public void addItem(){
        //notifyDataSetChanged();
        notifyItemInserted(getItemCount());

    }

    /**
     * called as to toggle selection of any item in adapter
     * @param position the position of the item
     * @param imageView the check {@link CircleGradientDrawable} that is to be animated
     */
    public void toggleChecked(int position, ImageView imageView) {
        if(!stoppedAnimation)main.stopAnimation();
        if (myChecked.get(position)) {
            // if the view at position is checked, un-check it
            myChecked.put(position, false);

            Animation iconAnimation = AnimationUtils.loadAnimation(context, R.anim.check_out);
            if (imageView!=null) {

                imageView.setAnimation(iconAnimation);
            } else {

                // TODO: we don't have the check icon object probably because of config change
            }
        } else {
            // if view is un-checked, check it
            myChecked.put(position, true);

            Animation iconAnimation = AnimationUtils.loadAnimation(context, R.anim.check_in);
            if (imageView!=null) {

                imageView.setAnimation(iconAnimation);
            } else {

                // TODO: we don't have the check icon object probably because of config change
            }
        }

        notifyDataSetChanged();
        //notifyItemChanged(position);
        if (main.selection == false || main.mActionMode == null) {
            main.selection = true;
            /*main.mActionMode = main.getActivity().startActionMode(
                    main.mActionModeCallback);*/
            main.mActionMode = main.MAIN_ACTIVITY.startSupportActionMode(main.mActionModeCallback);

        }
        if(main.mActionMode!=null && main.selection)
            main.mActionMode.invalidate();
        if (getCheckedItemPositions().size() == 0) {
            main.selection = false;
            main.mActionMode.finish();
            main.mActionMode = null;
        }
    }

    public void toggleChecked(boolean b,String path) {
        int a; if(path.equals("/") || !main.GO_BACK_ITEM)a=0;else a=1;
        for (int i = a; i < items.size(); i++) {
            myChecked.put(i, b);
            notifyItemChanged(i);
        }
        if(main.mActionMode!=null)
            main.mActionMode.invalidate();
        if (getCheckedItemPositions().size() == 0) {
            main.selection = false;
            if(main.mActionMode!=null)
                main.mActionMode.finish();
            main.mActionMode = null;
        }
    }

    /**
     * called when we would want to toggle check for all items in the adapter
     * @param b if to toggle true or false
     */
    public void toggleChecked(boolean b) {
        int a=0;
        for (int i = a; i < items.size(); i++) {
            myChecked.put(i, b);
            notifyItemChanged(i);
        }

        if(main.mActionMode!=null)main.mActionMode.invalidate();
        if (getCheckedItemPositions().size() == 0) {
            main.selection = false;
            if(main.mActionMode!=null)
                main.mActionMode.finish();
            main.mActionMode = null;
        }
    }
    public ArrayList<Integer> getCheckedItemPositions() {
        ArrayList<Integer> checkedItemPositions = new ArrayList<Integer>();

        for (int i = 0; i < myChecked.size(); i++) {
            if (myChecked.get(i)) {
                (checkedItemPositions).add(i);
            }
        }

        return checkedItemPositions;
    }

    public boolean areAllChecked(String path) {
        boolean b = true;
        int a; if(path.equals("/") || !main.GO_BACK_ITEM)a=0;else a=1;
        for (int i = a; i < myChecked.size(); i++) {
            if (!myChecked.get(i)) {
                b = false;
            }
        }
        return b;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public RoundedImageView pictureIcon;
        public ImageView genericIcon, apkIcon;
        public ImageView imageView1;
        public TextView txtTitle;
        public TextView txtDesc;
        public TextView date;
        public TextView perm;
        public View rl;
        public TextView genericText;
        public ImageButton about;
        public ImageView checkImageView;
        public ImageView checkImageViewGrid;

        public ViewHolder(View view) {
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
            about=(ImageButton) view.findViewById(R.id.properties);
            checkImageView = (ImageView) view.findViewById(R.id.check_icon);
            genericIcon = (ImageView) view.findViewById(R.id.generic_icon);
            checkImageViewGrid = (ImageView) view.findViewById(R.id.check_icon_grid);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType==0){
            View v= mInflater.inflate(R.layout.list_footer, parent, false);
            return new ViewHolder(v);

        }
        View v;if(main.IS_LIST) v= mInflater.inflate(R.layout.rowlayout, parent, false);
        else  v= mInflater.inflate(R.layout.griditem, parent, false);
        ViewHolder vh = new ViewHolder(v);
        if(main.theme1==1)
            vh.txtTitle.setTextColor(main.MAIN_ACTIVITY.getResources().getColor(android.R.color.white));
        return vh;
    }
    int offset=0;
    public boolean stoppedAnimation=false;
    Animation localAnimation;

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        ((ViewHolder)holder).rl.clearAnimation();
    }

    @Override
    public boolean onFailedToRecycleView(RecyclerView.ViewHolder holder) {
        ((ViewHolder)holder).rl.clearAnimation();
        return super.onFailedToRecycleView(holder);
    }

    void animate(Recycleradapter.ViewHolder holder){
        holder.rl.clearAnimation();
        localAnimation = AnimationUtils.loadAnimation(context,anim);
        localAnimation.setStartOffset(this.offset);
        holder.rl.startAnimation(localAnimation);
        this.offset+=30;
    }
    public void generate(ArrayList<Layoutelements> arrayList){
        offset=0;
        stoppedAnimation=false;
        notifyDataSetChanged();
        column=main.columns;
        items=arrayList;
        for (int i = 0; i < items.size(); i++) {
            myChecked.put(i, false);
            myanim.put(i,false);
        }
    }
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder vholder,final int p) {
        final ViewHolder holder = ((ViewHolder)vholder);
        if (main.IS_LIST) {
            if ( p == getItemCount() - 1) {
                holder.rl.setMinimumHeight(rowHeight);
                if (items.size() == (main.GO_BACK_ITEM ? 1 : 0))
                    holder.txtTitle.setText(R.string.nofiles);
                else holder.txtTitle.setText("");
                return;
            }
        }
        if (!this.stoppedAnimation && !myanim.get(p))
        {
            animate(holder);
            myanim.put(p, true);
        }
        final Layoutelements rowItem = items.get(p);
        if (main.IS_LIST) {
            holder.rl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    main.onListItemClicked(p, holder.checkImageView);
                }
            });

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                holder.checkImageView.setBackground(new CircleGradientDrawable(main.fabSkin,
                        main.theme1, main.getResources().getDisplayMetrics()));
            } else holder.checkImageView.setBackgroundDrawable(new CircleGradientDrawable(main.fabSkin,
                    main.theme1, main.getResources().getDisplayMetrics()));

            holder.rl.setOnLongClickListener(new View.OnLongClickListener() {

                public boolean onLongClick(View p1) {

                    // check if the item on which action is performed is not the first {goback} item
                    if (!rowItem.getSize().equals(main.goback)) {

                        toggleChecked(p, holder.checkImageView);
                    }

                    return true;
                }
            });

            filetype = -1;
            if (Icons.isPicture((rowItem.getDesc().toLowerCase()))) filetype = 0;
            else if (Icons.isApk((rowItem.getDesc()))) filetype = 1;
            else if (Icons.isVideo(rowItem.getDesc())) filetype = 2;
            holder.txtTitle.setText(rowItem.getTitle());
            holder.genericIcon.setImageDrawable(rowItem.getImageId());
            holder.genericText.setText("");

            if (holder.about != null) {
                if(main.theme1==0)holder.about.setColorFilter(grey_color);
                showPopup(holder.about,rowItem);
            }
            holder.genericIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    int id = v.getId();
                    if (id == R.id.generic_icon || id == R.id.picture_icon
                            || id == R.id.apk_icon) {

                        // TODO: transform icon on press to the properties dialog with animation
                        if (!rowItem.getSize().equals(main.goback)) {

                            toggleChecked(p, holder.checkImageView);
                        } else main.goBack();
                    }

                }
            });

            holder.pictureIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!rowItem.getSize().equals(main.goback)) {

                        toggleChecked(p, holder.checkImageView);
                    }
                    else main.goBack();
                }
            });
            holder.apkIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!rowItem.getSize().equals(main.goback)) {

                        toggleChecked(p, holder.checkImageView);
                    }
                    else main.goBack();
                }
            });
            holder.genericIcon.setVisibility(View.VISIBLE);
            holder.pictureIcon.setVisibility(View.INVISIBLE);
            if (filetype == 0) {
                if (main.SHOW_THUMBS) {
                    holder.genericIcon.setVisibility(View.GONE);

                    if (main.CIRCULAR_IMAGES) {
                        holder.apkIcon.setVisibility(View.GONE);
                        holder.pictureIcon.setVisibility(View.VISIBLE);
                        holder.pictureIcon.setImageDrawable(main.DARK_IMAGE);
                        main.ic.cancelLoad(holder.pictureIcon);
                        main.ic.loadDrawable(holder.pictureIcon, (rowItem.getDesc()), null);
                    } else {
                        holder.checkImageView.setVisibility(View.INVISIBLE);
                        holder.apkIcon.setVisibility(View.VISIBLE);
                        holder.apkIcon.setImageDrawable(main.DARK_IMAGE);
                        main.ic.cancelLoad(holder.apkIcon);
                        main.ic.loadDrawable(holder.apkIcon, (rowItem.getDesc()), null);
                    }
                }
            } else if (filetype == 1) {
                if (main.SHOW_THUMBS) {
                    holder.genericIcon.setVisibility(View.GONE);
                    holder.pictureIcon.setVisibility(View.GONE);
                    holder.checkImageView.setVisibility(View.INVISIBLE);
                    holder.apkIcon.setVisibility(View.VISIBLE);
                    holder.apkIcon.setImageDrawable(main.apk);
                    main.ic.cancelLoad(holder.apkIcon);
                    main.ic.loadDrawable(holder.apkIcon, (rowItem.getDesc()), null);
                }

            } else if (filetype == 2) {
                if (main.SHOW_THUMBS) {
                    holder.genericIcon.setVisibility(View.GONE);
                    if (main.CIRCULAR_IMAGES) {
                        holder.checkImageView.setVisibility(View.INVISIBLE);
                        holder.pictureIcon.setVisibility(View.VISIBLE);
                        holder.pictureIcon.setImageDrawable(main.DARK_VIDEO);
                        main.ic.cancelLoad(holder.pictureIcon);
                        main.ic.loadDrawable(holder.pictureIcon,(rowItem.getDesc()), null);
                    } else {
                        holder.checkImageView.setVisibility(View.INVISIBLE);
                        holder.apkIcon.setVisibility(View.VISIBLE);
                        holder.apkIcon.setImageDrawable(main.DARK_VIDEO);
                        main.ic.cancelLoad(holder.apkIcon);
                        main.ic.loadDrawable(holder.apkIcon, (rowItem.getDesc()), null);
                    }
                }
            } else {
                holder.pictureIcon.setVisibility(View.GONE);
                holder.apkIcon.setVisibility(View.GONE);
                holder.genericIcon.setVisibility(View.VISIBLE);
            }
            Boolean checked = myChecked.get(p);
            if (checked != null) {

                if (main.theme1 == 0) {

                    holder.rl.setBackgroundResource(R.drawable.safr_ripple_white);
                } else {

                    holder.rl.setBackgroundResource(R.drawable.safr_ripple_black);
                }
                holder.rl.setSelected(false);
                if (checked) {
                    holder.checkImageView.setVisibility(View.VISIBLE);
                    // making sure the generic icon background color filter doesn't get changed
                    // to grey on picture/video/apk/generic text icons when checked
                    // so that user can still look at the thumbs even after selection
                    if ((filetype!=0 && filetype!=1 && filetype!=2)) {
                        holder.apkIcon.setVisibility(View.GONE);
                        holder.pictureIcon.setVisibility(View.GONE);
                        holder.genericIcon.setVisibility(View.VISIBLE);
                        GradientDrawable gradientDrawable = (GradientDrawable) holder.genericIcon.getBackground();
                        gradientDrawable.setColor(c1);
                    }
                    holder.rl.setSelected(true);
                    holder.genericText.setText("");
                } else {
                    holder.checkImageView.setVisibility(View.INVISIBLE);
                    GradientDrawable gradientDrawable = (GradientDrawable) holder.genericIcon.getBackground();
                    if (main.COLORISE_ICONS) {
                        if (rowItem.isDirectory())
                            gradientDrawable.setColor(main.icon_skin_color);
                        else if (Icons.isVideo(rowItem.getDesc()) || Icons.isPicture(rowItem
                                .getDesc()))
                            gradientDrawable.setColor(c2);
                        else if (Icons.isAudio(rowItem.getDesc()))
                            gradientDrawable.setColor(c3);
                        else if (Icons.isPdf(rowItem.getDesc()))
                            gradientDrawable.setColor(c4);
                        else if (Icons.isCode(rowItem.getDesc()))
                            gradientDrawable.setColor(c5);
                        else if (Icons.isText(rowItem.getDesc()))
                            gradientDrawable.setColor(c6);
                        else if (Icons.isArchive(rowItem.getDesc()))
                            gradientDrawable.setColor(c7);
                        else if(Icons.isApk(rowItem.getDesc()))
                            gradientDrawable.setColor(c8);
                        else if (Icons.isgeneric(rowItem.getDesc())) {
                            gradientDrawable.setColor(c9);
                            String ext = MimeTypes.getExtension(new File(rowItem.getDesc()).getName());
                            if (ext != null && ext.trim().length() != 0) {
                                holder.genericText.setText(ext);
                                holder.genericIcon.setImageDrawable(null);
                            }
                        } else {
                            gradientDrawable.setColor(main.icon_skin_color);
                        }
                    } else gradientDrawable.setColor((main.icon_skin_color));
                    if (rowItem.getSize().equals(main.goback))
                        gradientDrawable.setColor(c1);


                }
            }
            if (main.SHOW_PERMISSIONS)
                holder.perm.setText(rowItem.getPermissions());
            if (main.SHOW_LAST_MODIFIED)
                holder.date.setText(rowItem.getDate());
            String size = rowItem.getSize();

            if (size.equals(main.goback)) {

                holder.date.setText(size);

                holder.txtDesc.setText("");
            } else if (main.SHOW_SIZE)

                holder.txtDesc.setText(rowItem.getSize());
        } else {
            // view is a grid view
            Boolean checked = myChecked.get(p);

            holder.checkImageViewGrid.setColorFilter(Color.parseColor(main.fabSkin));
            holder.rl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    main.onListItemClicked(p, holder.checkImageViewGrid);
                }
            });

            holder.rl.setOnLongClickListener(new View.OnLongClickListener() {

                public boolean onLongClick(View p1) {

                    if (!rowItem.getSize().equals(main.goback)) {

                        toggleChecked(p, holder.checkImageViewGrid);
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
                if (main.theme == 1)
                    holder.imageView1.setBackgroundColor(Color.BLACK);
                main.ic.cancelLoad(holder.imageView1);
                main.ic.loadDrawable(holder.imageView1, (rowItem.getDesc()), null);
            } else if (Icons.isApk((rowItem.getDesc()))) {
                holder.genericIcon.setColorFilter(null);
                main.ic.cancelLoad(holder.genericIcon);
                main.ic.loadDrawable(holder.genericIcon, (rowItem.getDesc()), null);
            }
            if (rowItem.isDirectory())
                holder.genericIcon.setColorFilter(main.icon_skin_color);
                    else if (Icons.isVideo(rowItem.getDesc()))
                holder.genericIcon.setColorFilter(c2);
                    else if (Icons.isAudio(rowItem.getDesc()))
                holder.genericIcon.setColorFilter(c3);
                    else if (Icons.isPdf(rowItem.getDesc()))
                holder.genericIcon.setColorFilter(c4);
                    else if (Icons.isCode(rowItem.getDesc()))
                holder.genericIcon.setColorFilter(c5);
                    else if (Icons.isText(rowItem.getDesc()))
                holder.genericIcon.setColorFilter(c6);
                    else if (Icons.isArchive(rowItem.getDesc()))
                holder.genericIcon.setColorFilter(c7);
                    else if (Icons.isgeneric(rowItem.getDesc()))
                holder.genericIcon.setColorFilter(c9);
                    else if (Icons.isApk(rowItem.getDesc()) || Icons.isPicture(rowItem.getDesc()))
                holder.genericIcon.setColorFilter(null);
            else holder.genericIcon.setColorFilter(main.icon_skin_color);
            if (rowItem.getSize().equals(main.goback))
                holder.genericIcon.setColorFilter(c1);



            if (checked != null) {

                if (checked) {
                    holder.genericIcon.setColorFilter(main.icon_skin_color);
                    //holder.genericIcon.setImageDrawable(main.getResources().getDrawable(R.drawable.abc_ic_cab_done_holo_dark));

                    holder.checkImageViewGrid.setVisibility(View.VISIBLE);
                    holder.rl.setBackgroundColor(Color.parseColor("#9f757575"));
                } else {
                    holder.checkImageViewGrid.setVisibility(View.INVISIBLE);
                    if (main.theme1 == 0)
                        holder.rl.setBackgroundResource(R.drawable.item_doc_grid);
                    else{
                        holder.rl.setBackgroundResource(R.drawable.ic_grid_card_background_dark);
                        holder.rl.findViewById(R.id.icon_frame).setBackgroundColor(Color.parseColor("#303030"));
                    }
                }
            }

            if (holder.about != null) {
                if(main.theme1==0)holder.about.setColorFilter(grey_color);
                showPopup(holder.about,rowItem);
            }
            if (main.SHOW_LAST_MODIFIED)
                holder.date.setText(rowItem.getDate());
            if (rowItem.getSize().equals(main.goback)) {
                holder.date.setText(rowItem.getSize());
                holder.txtDesc.setText("");
            }/*else if(main.SHOW_SIZE)
                holder.txtDesc.setText(rowItem.getSize());
           */ if (main.SHOW_PERMISSIONS)
                holder.perm.setText(rowItem.getPermissions());
        }
    }

    @Override
    public long getHeaderId(int i) {
        if(items.size()==0)return -1;
        if(i>=0 && i<items.size())
            if(main.IS_LIST){
                if(i!=items.size()){
                    if(items.get(i).getSize().equals(main.goback))return -1;
                    if(items.get(i).isDirectory())return 'D';
                    else return 'F';}
            }
        return -1;}
    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public TextView ext;

        public HeaderViewHolder(View view) {
            super(view);

            ext = (TextView) view.findViewById(R.id.headertext);
        }}
    @Override
    public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup viewGroup) {
        View  view = mInflater.inflate(R.layout.listheader, viewGroup, false);
        /*if(main.theme1==1)
            view.setBackgroundResource(R.color.holo_dark_background);*/
        HeaderViewHolder holder = new HeaderViewHolder(view);
        if (main.theme1==0)holder.ext.setTextColor(Color.parseColor("#8A000000"));
        else holder.ext.setTextColor(Color.parseColor("#B3ffffff"));
        return holder;
    }
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position))
            return TYPE_HEADER;

        return TYPE_ITEM;
    }
    void showPopup(View v,final Layoutelements rowItem){
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(main.getActivity(), view);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.about:
                                main.utils.showProps((rowItem).generateBaseFile(), rowItem.getPermissions(), main, main.ROOT_MODE);
                                return true;
                            case R.id.share:
                                ArrayList<File> arrayList = new ArrayList<File>();
                                arrayList.add(new File(rowItem.getDesc()));
                                main.utils.shareFiles(arrayList, main.MAIN_ACTIVITY, main.theme1, Color.parseColor(main.fabSkin));
                                return true;
                            case R.id.rename:
                                main.rename(rowItem.generateBaseFile());
                                return true;
                            case R.id.cpy:
                                MainActivity MAIN_ACTIVITY=main.MAIN_ACTIVITY;
                                main.MAIN_ACTIVITY.MOVE_PATH = null;
                                ArrayList<BaseFile> copies = new ArrayList<>();
                                copies.add(rowItem.generateBaseFile());
                                MAIN_ACTIVITY.COPY_PATH = copies;
                                MAIN_ACTIVITY.supportInvalidateOptionsMenu();
                                return true;
                            case R.id.cut:
                                MainActivity MAIN_ACTIVITY1=main.MAIN_ACTIVITY;
                                MAIN_ACTIVITY1.COPY_PATH = null;
                                ArrayList<BaseFile> copie = new ArrayList<>();
                                copie.add(rowItem.generateBaseFile());
                                MAIN_ACTIVITY1.MOVE_PATH = copie;
                                MAIN_ACTIVITY1.supportInvalidateOptionsMenu();
                                return true;
                            case R.id.ex:
                                main.MAIN_ACTIVITY.mainActivityHelper.extractFile(new File(rowItem.getDesc()));
                                return true;
                            case R.id.book:
                                    DataUtils.addBook(new String[]{rowItem.getTitle(),rowItem.getDesc()},true);
                                main.MAIN_ACTIVITY.updateDrawer();
                                Toast.makeText(main.getActivity(), main.utils.getString(main.getActivity(), R.string.bookmarksadded), Toast.LENGTH_LONG).show();
                                return true;

                        }
                        return false;
                    }
                });
                popupMenu.inflate(R.menu.item_extras);
                String x = rowItem.getDesc().toLowerCase();
                if(rowItem.isDirectory())popupMenu.getMenu().findItem(R.id.share).setVisible(false);
                if (x.endsWith(".zip") || x.endsWith(".jar") || x.endsWith(".apk") || x.endsWith(".rar") || x.endsWith(".tar") || x.endsWith(".tar.gz"))
                    popupMenu.getMenu().findItem(R.id.ex).setVisible(true);
                popupMenu.show();
            }
        });

    }
    private boolean isPositionHeader(int position) {
        if(main.IS_LIST)
            return  (position== items.size());
        return false;}
    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        if(i!=getItemCount()-1) {
            HeaderViewHolder holder=(HeaderViewHolder)viewHolder;
            if(items.get(i).isDirectory())holder.ext.setText(R.string.directories);
            else holder.ext.setText(R.string.files);
        }
    }

    @Override
    public int getItemCount() {
        return main.IS_LIST?items.size()+1:items.size();
    }
}