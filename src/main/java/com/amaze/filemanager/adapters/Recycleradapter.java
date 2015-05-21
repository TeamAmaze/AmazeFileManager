package com.amaze.filemanager.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.utils.Icons;
import com.amaze.filemanager.utils.Layoutelements;
import com.amaze.filemanager.utils.MimeTypes;
import com.amaze.filemanager.utils.RoundedImageView;
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
    ColorMatrixColorFilter colorMatrixColorFilter;
    LayoutInflater mInflater;
    int filetype=-1;
    int item_count;
    public Recycleradapter(Main m,ArrayList<Layoutelements> items,Context context){
        this.main=m;
        this.items=items;
        this.context=context;
        for (int i = 0; i < items.size(); i++) {
            myChecked.put(i, false);
        }
        colorMatrixColorFilter=main.colorMatrixColorFilter;
        mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        item_count=items.size()+(main.islist?2:3);
    }
    public void toggleChecked(int position) {
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
            main.mActionMode = main.mainActivity.toolbar.startActionMode(main.mActionModeCallback);
        }
        main.mActionMode.invalidate();
        if (getCheckedItemPositions().size() == 0) {
            main.selection = false;
            main.mActionMode.finish();
            main.mActionMode = null;
        }
    }

    public void toggleChecked(boolean b,String path) {
        int a; if(path.equals("/") || !main.gobackitem)a=0;else a=1;
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
        int a; if(path.equals("/") || !main.gobackitem)a=0;else a=1;
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
            }
    }
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType==0){
            View v= mInflater.inflate(R.layout.list_footer, parent, false);
            return new ViewHolder(v);

        }
        View v;if(main.islist) v= mInflater.inflate(R.layout.rowlayout, parent, false);
        else  v= mInflater.inflate(R.layout.griditem, parent, false);
        ViewHolder vh = new ViewHolder(v);
        if(main.theme1==1)
        vh.txtTitle.setTextColor(main.getActivity().getResources().getColor(android.R.color.white));
        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder vholder,final int p1) {
        final Recycleradapter.ViewHolder holder=((Recycleradapter.ViewHolder)vholder);
        int i=0;
        if(main.islist){
            i=1;
        if(p1==getItemCount()-1){
            holder.rl.setMinimumHeight(main.paddingTop);
            if(item_count==2)
            holder.txtTitle.setText(R.string.nofiles);
            else holder.txtTitle.setText(R.string.guide3);
            return;}
        if(p1==0){
            holder.rl.setMinimumHeight(main.paddingTop);
            return;}}
        else{
            i=3;
            if(p1==0 || p1==1 || p1==2){
                holder.rl.setMinimumHeight(main.paddingTop);
                return;}
        }
        final int p=p1-i;
        final Layoutelements rowItem = items.get(p);
        if (main.islist) {
            holder.rl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    main.onListItemClicked(p, v);
                }
            });

            holder.rl.setOnLongClickListener(new View.OnLongClickListener() {

                public boolean onLongClick(View p1) {
                    if (!rowItem.getSize().equals(main.goback)) {

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

            holder.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Animation animation = AnimationUtils.loadAnimation(context, R.anim.holder_anim);
                    if (!rowItem.getSize().equals(main.goback)) {
                        holder.imageView.setAnimation(animation);
                        toggleChecked(p);
                    } else main.goBack();

                }
            });
            holder.viewmageV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!rowItem.getSize().equals(main.goback))
                        toggleChecked(p);
                    else main.goBack();
                }
            });
            holder.apk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!rowItem.getSize().equals(main.goback))
                        toggleChecked(p);
                    else main.goBack();
                }
            });
            holder.imageView.setVisibility(View.VISIBLE);
            holder.viewmageV.setVisibility(View.INVISIBLE);
            if (filetype == 0) {
                if (main.showThumbs) {
                    if (main.circularImages) {
                        holder.imageView.setVisibility(View.GONE);
                        holder.apk.setVisibility(View.GONE);
                        holder.viewmageV.setVisibility(View.VISIBLE);
                        holder.viewmageV.setImageDrawable(main.darkimage);
                        main.ic.cancelLoad(holder.viewmageV);
                        main.ic.loadDrawable(holder.viewmageV, new File(rowItem.getDesc()), null);
                    } else {
                        holder.imageView.setVisibility(View.GONE);
                        holder.apk.setVisibility(View.VISIBLE);
                        holder.apk.setImageDrawable(main.darkimage);
                        main.ic.cancelLoad(holder.apk);
                        main.ic.loadDrawable(holder.apk, new File(rowItem.getDesc()), null);
                    }
                }
            } else if (filetype == 1) {
                if (main.showThumbs) {
                    holder.viewmageV.setVisibility(View.GONE);
                    holder.imageView.setVisibility(View.GONE);
                    holder.apk.setVisibility(View.VISIBLE);
                    holder.apk.setImageDrawable(main.apk);
                    main.ic.cancelLoad(holder.apk);
                    main.ic.loadDrawable(holder.apk, new File(rowItem.getDesc()), null);
                }

            } else if (filetype == 2) {
                if (main.showThumbs) {
                    if (main.circularImages) {
                        holder.imageView.setVisibility(View.GONE);
                        holder.viewmageV.setVisibility(View.VISIBLE);
                        holder.viewmageV.setImageDrawable(main.darkvideo);
                        main.ic.cancelLoad(holder.viewmageV);
                        main.ic.loadDrawable(holder.viewmageV, new File(rowItem.getDesc()), null);
                    } else {
                        holder.imageView.setVisibility(View.GONE);
                        holder.apk.setVisibility(View.VISIBLE);
                        holder.apk.setImageDrawable(main.darkvideo);
                        main.ic.cancelLoad(holder.apk);
                        main.ic.loadDrawable(holder.apk, new File(rowItem.getDesc()), null);
                    }
                }
            } else {
                holder.viewmageV.setVisibility(View.GONE);
                holder.apk.setVisibility(View.GONE);
            }
            Boolean checked = myChecked.get(p);
            if (checked != null) {

                    if (main.theme1 == 0) {

                        holder.rl.setBackgroundResource(R.drawable.safr_ripple_white);
                    } else {

                        holder.rl.setBackgroundResource(R.drawable.safr_ripple_black);
                    }

                if (checked) {
                    holder.apk.setVisibility(View.GONE);
                    holder.viewmageV.setVisibility(View.GONE);
                    holder.imageView.setVisibility(View.VISIBLE);
                    holder.imageView.setImageDrawable(main.getResources().getDrawable(R.drawable.abc_ic_cab_done_holo_dark));
                    GradientDrawable gradientDrawable = (GradientDrawable) holder.imageView.getBackground();
                    gradientDrawable.setColor(Color.parseColor("#757575"));
                    holder.ext.setText("");
                    if (Build.VERSION.SDK_INT >= 21) {
                        /*if (main.theme1 == 0) {
                            holder.rl.setElevation(6f);
                        } else {
                            holder.rl.setBackgroundColor(context.getResources().getColor(R.color.safr_pressed_dark));
                        }*/
                        holder.rl.setElevation(6f);
                    }
                } else {
                    GradientDrawable gradientDrawable = (GradientDrawable) holder.imageView.getBackground();
                    if (main.coloriseIcons) {
                        if (rowItem.isDirectory())
                            gradientDrawable.setColor(main.icon_skin_color);
                        else if (Icons.isVideo(rowItem.getDesc()))
                            gradientDrawable.setColor(Color.parseColor("#f06292"));
                        else if (Icons.isAudio(rowItem.getDesc()))
                            gradientDrawable.setColor(Color.parseColor("#9575cd"));
                        else if (Icons.isPdf(rowItem.getDesc()))
                            gradientDrawable.setColor(Color.parseColor("#da4336"));
                        else if (Icons.isCode(rowItem.getDesc()))
                            gradientDrawable.setColor(Color.parseColor("#00bfa5"));
                        else if (Icons.isText(rowItem.getDesc()))
                            gradientDrawable.setColor(Color.parseColor("#e06055"));
                        else if (Icons.isArchive(rowItem.getDesc()))
                            gradientDrawable.setColor(Color.parseColor("#f9a825"));
                        else if (Icons.isgeneric(rowItem.getDesc())) {
                            gradientDrawable.setColor(Color.parseColor("#9e9e9e"));
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
                        gradientDrawable.setColor(Color.parseColor("#757575"));

                    if (Build.VERSION.SDK_INT >= 21) {
                        holder.rl.setElevation(0f);
                    }
                }
            }
            if (main.showPermissions)
                holder.perm.setText(rowItem.getPermissions());
            if (main.showLastModified)
                holder.date.setText(rowItem.getDate());
            String size = rowItem.getSize();

            if (size.equals(main.goback)) {

                holder.date.setText(size);

                holder.txtDesc.setText("");
            } else if (main.showSize)

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

                        toggleChecked(p);
                    }
                    return true;
                }
            });
            holder.txtTitle.setText(rowItem.getTitle());
            holder.imageView1.setVisibility(View.INVISIBLE);
            holder.imageView.setVisibility(View.VISIBLE);
            holder.imageView.setImageDrawable(rowItem.getImageId());
            if (Icons.isPicture((rowItem.getDesc().toLowerCase()))) {
                holder.imageView.setColorFilter(null);
                holder.imageView1.setVisibility(View.VISIBLE);
                holder.imageView1.setImageDrawable(null);
                if (main.theme == 1)
                    holder.imageView1.setBackgroundColor(Color.parseColor("#000000"));
                main.ic.cancelLoad(holder.imageView1);
                main.ic.loadDrawable(holder.imageView1, new File(rowItem.getDesc()), null);
            } else if (Icons.isApk((rowItem.getDesc()))) {
                holder.imageView.setColorFilter(null);
                main.ic.cancelLoad(holder.imageView);
                main.ic.loadDrawable(holder.imageView, new File(rowItem.getDesc()), null);
            }
            if (Icons.isVideo(rowItem.getDesc())) {
                holder.imageView.setColorFilter(null);
                holder.imageView1.setVisibility(View.VISIBLE);
                holder.imageView1.setImageDrawable(null);
                if (main.theme == 1)
                    holder.imageView1.setBackgroundColor(Color.parseColor("#000000"));
                main.ic.cancelLoad(holder.imageView1);
                main.ic.loadDrawable(holder.imageView1, new File(rowItem.getDesc()), null);

            }
            if (main.coloriseIcons) {

                if (rowItem.isDirectory())
                    holder.imageView.setColorFilter(main.icon_skin_color);

                else if (Icons.isVideo(rowItem.getDesc()))
                    holder.imageView.setColorFilter(Color.parseColor("#f06292"));

                else if (Icons.isAudio(rowItem.getDesc()))
                    holder.imageView.setColorFilter(Color.parseColor("#9575cd"));

                else if (Icons.isPdf(rowItem.getDesc()))
                    holder.imageView.setColorFilter(Color.parseColor("#da4336"));

                else if (Icons.isCode(rowItem.getDesc()))
                    holder.imageView.setColorFilter(Color.parseColor("#00bfa5"));

                else if (Icons.isText(rowItem.getDesc()))
                    holder.imageView.setColorFilter(Color.parseColor("#e06055"));

                else if (Icons.isArchive(rowItem.getDesc()))
                    holder.imageView.setColorFilter(Color.parseColor("#f9a825"));

                else if (Icons.isgeneric(rowItem.getDesc()))
                    holder.imageView.setColorFilter(Color.parseColor("#9e9e9e"));

                else if (Icons.isApk(rowItem.getDesc()) || Icons.isPicture(rowItem.getDesc()))
                    holder.imageView.setColorFilter(null);

                else holder.imageView.setColorFilter(main.icon_skin_color);

            } else if (!Icons.isApk(rowItem.getDesc()) && !Icons.isPicture(rowItem.getDesc()))
                holder.imageView.setColorFilter(main.icon_skin_color);
            else
                holder.imageView.setColorFilter(null);
            if (rowItem.getSize().equals(main.goback))
                holder.imageView.setColorFilter(Color.parseColor("#757575"));
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
            if (main.showLastModified)
                holder.date.setText(rowItem.getDate());
            if (rowItem.getSize().equals(main.goback)) {
                holder.date.setText(rowItem.getSize());
                holder.txtDesc.setText("");
            } else
                holder.txtDesc.setText(rowItem.getSize());
            if (main.showPermissions)
                holder.perm.setText(rowItem.getPermissions());
        }
    }

    @Override
    public long getHeaderId(int i) {
        if(i>=0 && i<item_count)
        if(main.islist){
            if(i!=0 && i!=item_count-1){
                    if(items.get(i-1).getSize().equals(main.goback))return -1;
                    if(items.get(i-1).isDirectory())return 'D';
                    else return 'F';}
    }
        else{
            if(i!=0 && i!=1 && i!=2){
                if(items.get(i-3).getSize().equals(main.goback))return -1;
                if(items.get(i-3).isDirectory())return 'D';
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

    private boolean isPositionHeader(int position) {
   if(main.islist)     return position == 0 || position==item_count-1;
    else return position == 0 || position==1 || position==2;}
    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        if(i!=0) {
            HeaderViewHolder holder=(HeaderViewHolder)viewHolder;
            if(items.get(i-1).isDirectory())holder.ext.setText("Directories");
            else holder.ext.setText("Files");
        }
    }

    @Override
    public int getItemCount() {
        return item_count;
    }
}

