package com.amaze.filemanager.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.GradientDrawable;
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
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.ui.Layoutelements;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.ui.views.RoundedImageView;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.utils.DataUtils;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Arpit on 11-04-2015.
 */
public class Recycleradapter extends RecyclerArrayAdapter<String, RecyclerView.ViewHolder>
        implements StickyRecyclerHeadersAdapter<RecyclerView.ViewHolder>{
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

    public Recycleradapter(Main m,ArrayList<Layoutelements> items,Context context){
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
    public void toggleChecked(int position) {
        if(!stoppedAnimation)main.stopAnimation();
        if (myChecked.get(position)) {
            myChecked.put(position, false);
        } else {
            myChecked.put(position, true);
        }

        notifyDataSetChanged();
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
        }
        notifyDataSetChanged();
        if(main.mActionMode!=null)
            main.mActionMode.invalidate();
        if (getCheckedItemPositions().size() == 0) {
            main.selection = false;
            if(main.mActionMode!=null)
                main.mActionMode.finish();
            main.mActionMode = null;
        }
    }

    public void toggleChecked(boolean b) {
        int a=0;
        for (int i = a; i < items.size(); i++) {
            myChecked.put(i, b);
        }
        notifyDataSetChanged();
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
        public RoundedImageView viewmageV;
        public ImageView imageView,apk;
        public ImageView imageView1;
        public TextView txtTitle;
        public TextView txtDesc;
        public TextView date;
        public TextView perm;
        public View rl;
        public TextView ext;
        public ImageButton about;
        public ViewHolder(View view) {
            super(view);

            txtTitle = (TextView) view.findViewById(R.id.firstline);
            viewmageV = (RoundedImageView) view.findViewById(R.id.cicon);
            imageView = (ImageView) view.findViewById(R.id.icon);
            rl = view.findViewById(R.id.second);
            perm = (TextView) view.findViewById(R.id.permis);
            date = (TextView) view.findViewById(R.id.date);
            txtDesc = (TextView) view.findViewById(R.id.secondLine);
            apk = (ImageView) view.findViewById(R.id.bicon);
            ext = (TextView) view.findViewById(R.id.generictext);
            imageView1 = (ImageView) view.findViewById(R.id.icon_thumb);
            about=(ImageButton) view.findViewById(R.id.properties);
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
            vh.txtTitle.setTextColor(main.getActivity().getResources().getColor(android.R.color.white));
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
        final Recycleradapter.ViewHolder holder = ((Recycleradapter.ViewHolder)vholder);
        if (main.IS_LIST) {
            if ( p == getItemCount() - 1) {
                holder.rl.setMinimumHeight(rowHeight);
                if (items.size() == (main.GO_BACK_ITEM ? 1 : 0))
                    holder.txtTitle.setText(R.string.nofiles);
                else holder.txtTitle.setText("");
                return;
            }
        }
        if(holder.imageView==null)return;
        if (!this.stoppedAnimation && !myanim.get(p))
        {
            animate(holder);
            myanim.put(p,true);
        }
        final Layoutelements rowItem = items.get(p);
        if (main.IS_LIST) {
            holder.rl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    main.onListItemClicked(p, v);
                }
            });

            holder.rl.setOnLongClickListener(new View.OnLongClickListener() {

                public boolean onLongClick(View p1) {

                    if (!rowItem.getSize().equals(main.goback)) {
                        Animation iconAnimation = AnimationUtils.loadAnimation(context, R.anim.holder_anim);
                        holder.imageView.setAnimation(iconAnimation);
                        toggleChecked(p);
                    }

                    return true;
                }
            });

            filetype = -1;
            if (Icons.isPicture((rowItem.getDesc().toLowerCase()))) filetype = 0;
            else if (Icons.isApk((rowItem.getDesc()))) filetype = 1;
            else if (Icons.isVideo(rowItem.getDesc())) filetype = 2;
            holder.txtTitle.setText(rowItem.getTitle());
            holder.imageView.setImageDrawable(rowItem.getImageId());
            holder.ext.setText("");

            if (holder.about != null) {
                if(main.theme1==0)holder.about.setColorFilter(grey_color);
                showPopup(holder.about,rowItem);
            }
            holder.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (!rowItem.getSize().equals(main.goback)) {

                        Animation iconAnimation = AnimationUtils.loadAnimation(context, R.anim.holder_anim);
                        holder.imageView.setAnimation(iconAnimation);
                        toggleChecked(p);
                    } else main.goBack();

                }
            });
            holder.viewmageV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!rowItem.getSize().equals(main.goback)) {

                        Animation iconAnimation = AnimationUtils.loadAnimation(context, R.anim.holder_anim);
                        holder.imageView.setAnimation(iconAnimation);
                        toggleChecked(p);
                    }
                    else main.goBack();
                }
            });
            holder.apk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!rowItem.getSize().equals(main.goback)) {

                        Animation iconAnimation = AnimationUtils.loadAnimation(context, R.anim.holder_anim);
                        holder.imageView.setAnimation(iconAnimation);
                        toggleChecked(p);
                    }
                    else main.goBack();
                }
            });
            holder.imageView.setVisibility(View.VISIBLE);
            holder.viewmageV.setVisibility(View.INVISIBLE);
            if (filetype == 0) {
                if (main.SHOW_THUMBS) {
                    if (main.CIRCULAR_IMAGES) {
                        holder.imageView.setVisibility(View.GONE);
                        holder.apk.setVisibility(View.GONE);
                        holder.viewmageV.setVisibility(View.VISIBLE);
                        holder.viewmageV.setImageDrawable(main.DARK_IMAGE);
                        main.ic.cancelLoad(holder.viewmageV);
                        main.ic.loadDrawable(holder.viewmageV, (rowItem.getDesc()), null);
                    } else {
                        holder.imageView.setVisibility(View.GONE);
                        holder.apk.setVisibility(View.VISIBLE);
                        holder.apk.setImageDrawable(main.DARK_IMAGE);
                        main.ic.cancelLoad(holder.apk);
                        main.ic.loadDrawable(holder.apk, (rowItem.getDesc()), null);
                    }
                }
            } else if (filetype == 1) {
                if (main.SHOW_THUMBS) {
                    holder.viewmageV.setVisibility(View.GONE);
                    holder.imageView.setVisibility(View.GONE);
                    holder.apk.setVisibility(View.VISIBLE);
                    holder.apk.setImageDrawable(main.apk);
                    main.ic.cancelLoad(holder.apk);
                    main.ic.loadDrawable(holder.apk, (rowItem.getDesc()), null);
                }

            } else if (filetype == 2) {
                if (main.SHOW_THUMBS) {
                    if (main.CIRCULAR_IMAGES) {
                        holder.imageView.setVisibility(View.GONE);
                        holder.viewmageV.setVisibility(View.VISIBLE);
                        holder.viewmageV.setImageDrawable(main.DARK_VIDEO);
                        main.ic.cancelLoad(holder.viewmageV);
                        main.ic.loadDrawable(holder.viewmageV,(rowItem.getDesc()), null);
                    } else {
                        holder.imageView.setVisibility(View.GONE);
                        holder.apk.setVisibility(View.VISIBLE);
                        holder.apk.setImageDrawable(main.DARK_VIDEO);
                        main.ic.cancelLoad(holder.apk);
                        main.ic.loadDrawable(holder.apk, (rowItem.getDesc()), null);
                    }
                }
            } else {
                holder.viewmageV.setVisibility(View.GONE);
                holder.apk.setVisibility(View.GONE);
                holder.imageView.setVisibility(View.VISIBLE);
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
                    holder.apk.setVisibility(View.GONE);
                    holder.viewmageV.setVisibility(View.GONE);
                    holder.imageView.setVisibility(View.VISIBLE);
                    holder.imageView.setImageDrawable(main.getResources().getDrawable(R.drawable.abc_ic_cab_done_holo_dark));
                    GradientDrawable gradientDrawable = (GradientDrawable) holder.imageView.getBackground();
                    gradientDrawable.setColor(c1);
                    holder.rl.setSelected(true);
                    holder.ext.setText("");
                } else {
                    GradientDrawable gradientDrawable = (GradientDrawable) holder.imageView.getBackground();
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
                                holder.ext.setText(ext);
                                holder.imageView.setImageDrawable(null);
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
            Boolean checked = myChecked.get(p);
            holder.rl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    main.onListItemClicked(p, v);
                }
            });

            holder.rl.setOnLongClickListener(new View.OnLongClickListener() {

                public boolean onLongClick(View p1) {

                    if (!rowItem.getSize().equals(main.goback)) {
                        Animation iconAnimation = AnimationUtils.loadAnimation(context, R.anim.holder_anim);
                        holder.imageView.setAnimation(iconAnimation);
                        toggleChecked(p);
                    }
                    return true;
                }
            });
            holder.txtTitle.setText(rowItem.getTitle());
            holder.imageView1.setVisibility(View.INVISIBLE);
            holder.imageView.setVisibility(View.VISIBLE);
            holder.imageView.setImageDrawable(rowItem.getImageId());
            if (Icons.isPicture((rowItem.getDesc().toLowerCase())) || Icons.isVideo(rowItem.getDesc().toLowerCase())) {
                holder.imageView.setColorFilter(null);
                holder.imageView1.setVisibility(View.VISIBLE);
                holder.imageView1.setImageDrawable(null);
                if (main.theme == 1)
                    holder.imageView1.setBackgroundColor(Color.BLACK);
                main.ic.cancelLoad(holder.imageView1);
                main.ic.loadDrawable(holder.imageView1, (rowItem.getDesc()), null);
            } else if (Icons.isApk((rowItem.getDesc()))) {
                holder.imageView.setColorFilter(null);
                main.ic.cancelLoad(holder.imageView);
                main.ic.loadDrawable(holder.imageView, (rowItem.getDesc()), null);
            }
            if (rowItem.isDirectory())
                holder.imageView.setColorFilter(main.icon_skin_color);

            else if (Icons.isVideo(rowItem.getDesc()))
                holder.imageView.setColorFilter(c2);

            else if (Icons.isAudio(rowItem.getDesc()))
                holder.imageView.setColorFilter(c3);

            else if (Icons.isPdf(rowItem.getDesc()))
                holder.imageView.setColorFilter(c4);

            else if (Icons.isCode(rowItem.getDesc()))
                holder.imageView.setColorFilter(c5);

            else if (Icons.isText(rowItem.getDesc()))
                holder.imageView.setColorFilter(c6);

            else if (Icons.isArchive(rowItem.getDesc()))
                holder.imageView.setColorFilter(c7);

            else if (Icons.isgeneric(rowItem.getDesc()))
                holder.imageView.setColorFilter(c9);

            else if (Icons.isApk(rowItem.getDesc()) || Icons.isPicture(rowItem.getDesc()))
                holder.imageView.setColorFilter(null);

            else holder.imageView.setColorFilter(main.icon_skin_color);
            if (rowItem.getSize().equals(main.goback))
                holder.imageView.setColorFilter(c1);
            if (checked != null) {

                if (checked) {
                    holder.imageView.setColorFilter(main.icon_skin_color);
                    holder.imageView.setImageDrawable(main.getResources().getDrawable(R.drawable.abc_ic_cab_done_holo_dark));
                    holder.rl.setBackgroundColor(Color.parseColor("#9f757575"));
                } else {
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
        if(main.theme1==1)
            view.setBackgroundResource(R.color.holo_dark_background);
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