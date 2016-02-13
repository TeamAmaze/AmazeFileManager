package com.amaze.filemanager.adapters;

/**
 * Created by Arpit on 25-01-2015.
 */

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.ZipViewer;
import com.amaze.filemanager.services.asynctasks.RarHelperTask;
import com.amaze.filemanager.services.asynctasks.ZipExtractTask;
import com.amaze.filemanager.services.asynctasks.ZipHelperTask;
import com.amaze.filemanager.ui.ZipObj;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.views.RoundedImageView;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.filesystem.HFile;
import com.github.junrar.rarfile.FileHeader;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipFile;

public class RarAdapter extends RecyclerArrayAdapter<String, RecyclerView.ViewHolder>
        implements StickyRecyclerHeadersAdapter<RecyclerView.ViewHolder> {
    Context c;
    Drawable folder, unknown;
    ArrayList<FileHeader> enter;
    ArrayList<ZipObj> enter1;
    ZipViewer zipViewer;
    LayoutInflater mInflater;
    private SparseBooleanArray myChecked = new SparseBooleanArray();
    boolean zipMode=false;
    public RarAdapter(Context c,ArrayList<FileHeader> enter, ZipViewer zipViewer) {
        this.enter = enter;
        for (int i = 0; i < enter.size(); i++) {
            myChecked.put(i, false);
        }
        mInflater = (LayoutInflater) c.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        this.c = c;
        folder = c.getResources().getDrawable(R.drawable.ic_grid_folder_new);
        unknown = c.getResources().getDrawable(R.drawable.ic_doc_generic_am);
        this.zipViewer = zipViewer;
    }
    public RarAdapter(Context c, ArrayList<ZipObj> enter, ZipViewer zipViewer,boolean l) {
        this.enter1 = enter;
        for (int i = 0; i < enter.size(); i++) {
            myChecked.put(i, false);
        }
        zipMode=true;
        this.c = c;
        if(c==null)return;
        folder = c.getResources().getDrawable(R.drawable.ic_grid_folder_new);
        unknown = c.getResources().getDrawable(R.drawable.ic_doc_generic_am);
        this.zipViewer = zipViewer;
        mInflater = (LayoutInflater) c
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

    }
    public void toggleChecked(int position) {
        zipViewer.stopAnim();
        stoppedAnimation=true;
        if (myChecked.get(position)) {
            myChecked.put(position, false);
        } else {
            myChecked.put(position, true);
        }

        notifyDataSetChanged();
        if (zipViewer.selection == false || zipViewer.mActionMode == null) {
            zipViewer.selection = true;
            /*zipViewer.mActionMode = zipViewer.getActivity().startActionMode(
                   zipViewer.mActionModeCallback);*/
            zipViewer.mActionMode = zipViewer.mainActivity.toolbar.startActionMode(zipViewer.mActionModeCallback);
        }
        zipViewer.mActionMode.invalidate();
        if (getCheckedItemPositions().size() == 0) {
            zipViewer.selection = false;
            zipViewer.mActionMode.finish();
            zipViewer.mActionMode = null;
        }
    }

    public void toggleChecked(boolean b,String path) {
        int k=0;
       // if(enter.get(0).getEntry()==null)k=1;
        for (int i = k; i < (zipMode?enter1.size():enter.size()); i++) {
            myChecked.put(i, b);
        }
        notifyDataSetChanged();
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public RoundedImageView viewmageV;
        public ImageView imageView,apk;
        public TextView txtTitle;
        public TextView txtDesc;
        public TextView date;
        public TextView perm;
        public View rl;

        public ViewHolder(View view) {
            super(view);
            txtTitle = (TextView) view.findViewById(R.id.firstline);
            viewmageV = (RoundedImageView) view.findViewById(R.id.cicon);
            imageView = (ImageView) view.findViewById(R.id.icon);
            rl = view.findViewById(R.id.second);
            perm = (TextView) view.findViewById(R.id.permis);
            date = (TextView) view.findViewById(R.id.date);
            txtDesc = (TextView) view.findViewById(R.id.secondLine);
            apk=(ImageView)view.findViewById(R.id.bicon);
        }
    }

    @Override
    public long getHeaderId(int position) {
        if (zipMode) return getHeaderid(position);
        if (position < 0) return -1;
        if (position >= 0 && position < enter.size()) {
            if (enter.get(position) == null) return -1;
            else if (enter.get(position).isDirectory()) return 'D';
            else return 'F';
        }
    return -1;}
    long getHeaderid(int position) {
        if (position >= 0 && position < enter1.size())
              if (enter1.get(position ) == null) return -1;
                else if (enter1.get(position).isDirectory()) return 'D';
                else return 'F';

        return -1;
    }
    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public TextView ext;

        public HeaderViewHolder(View view) {
            super(view);

            ext = (TextView) view.findViewById(R.id.headertext);
        }}
    @Override
    public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup viewGroup) {
        View  view = mInflater.inflate(R.layout.listheader, viewGroup, false);
        if(zipViewer.mainActivity.theme1==1)
            view.setBackgroundResource(R.color.holo_dark_background);
        HeaderViewHolder holder = new HeaderViewHolder(view);
        return holder;
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
    if(zipMode && i>=0){
        HeaderViewHolder holder=(HeaderViewHolder)viewHolder;
        if(enter1.get(i)!=null && enter1.get(i).isDirectory())holder.ext.setText("Directories");
        else holder.ext.setText("Files");

    }
        else if(i>=0){
        HeaderViewHolder holder=(HeaderViewHolder)viewHolder;
        if(enter.get(i)!=null && enter.get(i).isDirectory())holder.ext.setText(R.string.directories);
        else holder.ext.setText(R.string.files);
    }}


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType==0){
            View v= mInflater.inflate(R.layout.rowlayout, parent, false);
            v.findViewById(R.id.icon).setVisibility(View.INVISIBLE);
            return new ViewHolder(v);

        }
        View v= mInflater.inflate(R.layout.rowlayout,parent, false);
        ViewHolder vh = new ViewHolder(v);
        if(zipViewer.mainActivity.theme1==1)
            vh.txtTitle.setTextColor(zipViewer.getActivity().getResources().getColor(android.R.color.white));
        ImageButton about = (ImageButton) v.findViewById(R.id.properties);
        about.setVisibility(View.INVISIBLE);
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

    void animate(RarAdapter.ViewHolder holder){
        holder.rl.clearAnimation();
        localAnimation = AnimationUtils.loadAnimation(zipViewer.getActivity(), R.anim.fade_in_top);
        localAnimation.setStartOffset(this.offset);
        holder.rl.startAnimation(localAnimation);
        this.offset = (30 + this.offset);
    }
    public void generate(ArrayList<FileHeader> arrayList){
        offset=0;
        stoppedAnimation=false;
        notifyDataSetChanged();
        enter=arrayList;
    }
    public void generate(ArrayList<ZipObj> arrayList,boolean zipMode){
        offset=0;
        stoppedAnimation=false;
        notifyDataSetChanged();
        enter1=arrayList;
    }
    void onBindView(RecyclerView.ViewHolder vholder,final int position1){
        final RarAdapter.ViewHolder holder = ((RarAdapter.ViewHolder)vholder);
        if (!this.stoppedAnimation)
        {
            animate(holder);
        }
        final ZipObj rowItem=enter1.get(position1);
        final int p=position1;
        GradientDrawable gradientDrawable = (GradientDrawable) holder.imageView.getBackground();
        if(rowItem.getEntry()==null){
            holder.imageView.setImageDrawable(zipViewer.getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
            gradientDrawable.setColor(Color.parseColor("#757575"));
            holder.txtTitle.setText("..");
            holder.txtDesc.setText("");
            holder.date.setText(R.string.goback);
        }
        else {
            holder.imageView.setImageDrawable(Icons.loadMimeIcon(zipViewer.getActivity(), rowItem.getName(), false,zipViewer.res));
            final StringBuilder stringBuilder = new StringBuilder(rowItem.getName());
            if (zipViewer.showLastModified)
                holder.date.setText(new Futils().getdate(rowItem.getTime(), "MMM dd, yyyy", zipViewer.year));
            if (rowItem.isDirectory()) {
                holder.imageView.setImageDrawable(folder);
                gradientDrawable.setColor(Color.parseColor(zipViewer.iconskin));
                if (stringBuilder.toString().length() > 0) {
                    stringBuilder.deleteCharAt(rowItem.getName().length() - 1);
                    try {
                        holder.txtTitle.setText(stringBuilder.toString().substring(stringBuilder.toString().lastIndexOf("/") + 1));
                    } catch (Exception e) {
                        holder.txtTitle.setText(rowItem.getName().substring(0, rowItem.getName().lastIndexOf("/")));
                    }
                } } else {
                if (zipViewer.showSize)
                    holder.txtDesc.setText(new Futils().readableFileSize(rowItem.getSize()));
                holder.txtTitle.setText(rowItem.getName().substring(rowItem.getName().lastIndexOf("/") + 1));
                if (zipViewer.coloriseIcons) {
                    if (Icons.isVideo(rowItem.getName()) || Icons.isPicture(rowItem.getName()))
                        gradientDrawable.setColor(Color.parseColor("#f06292"));
                    else if (Icons.isAudio(rowItem.getName()))
                        gradientDrawable.setColor(Color.parseColor("#9575cd"));
                    else if (Icons.isPdf(rowItem.getName()))
                        gradientDrawable.setColor(Color.parseColor("#da4336"));
                    else if (Icons.isCode(rowItem.getName()))
                        gradientDrawable.setColor(Color.parseColor("#00bfa5"));
                    else if (Icons.isText(rowItem.getName()))
                        gradientDrawable.setColor(Color.parseColor("#e06055"));
                    else if (Icons.isArchive(rowItem.getName()))
                        gradientDrawable.setColor(Color.parseColor("#f9a825"));
                    else if(Icons.isApk(rowItem.getName()))
                        gradientDrawable.setColor(Color.parseColor("#a4c439"));
                    else if (Icons.isgeneric(rowItem.getName()))
                        gradientDrawable.setColor(Color.parseColor("#9e9e9e"));
                    else gradientDrawable.setColor(Color.parseColor(zipViewer.iconskin));
                } else gradientDrawable.setColor(Color.parseColor(zipViewer.iconskin));
            }
        }


        holder.rl.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(rowItem.getEntry()!=null) {

                    final Animation animation = AnimationUtils.loadAnimation(zipViewer.getActivity(), R.anim.holder_anim);

                    holder.imageView.setAnimation(animation);
                    toggleChecked(p);
                }
                return true;
            }
        });holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(rowItem.getEntry()!=null){
                    final Animation animation = AnimationUtils.loadAnimation(zipViewer.getActivity(), R.anim.holder_anim);

                    holder.imageView.setAnimation(animation);
                    toggleChecked(p);}

            }
        });
        Boolean checked = myChecked.get(p);
        if (checked != null) {

            if (zipViewer.mainActivity.theme1 == 0) {

                holder.rl.setBackgroundResource(R.drawable.safr_ripple_white);
            } else {

                holder.rl.setBackgroundResource(R.drawable.safr_ripple_black);

            }
            holder.rl.setSelected(false);
            if (checked) {
                holder.imageView.setImageDrawable(zipViewer.getResources().getDrawable(R.drawable.abc_ic_cab_done_holo_dark));
                gradientDrawable.setColor(Color.parseColor("#757575"));
                holder.rl.setSelected(true);
            }
        }
        holder.rl.setOnClickListener(new View.OnClickListener() {

            public void onClick(View p1) {
                if(rowItem.getEntry()==null)
                    zipViewer.goBack();
                else{
                    if(zipViewer.selection) {
                        final Animation animation = AnimationUtils.loadAnimation(zipViewer.getActivity(), R.anim.holder_anim);
                        holder.imageView.setAnimation(animation);
                        toggleChecked(p);
                    }
                    else {
                        final StringBuilder stringBuilder = new StringBuilder(rowItem.getName());
                        if (rowItem.isDirectory())
                            stringBuilder.deleteCharAt(rowItem.getName().length() - 1);

                        if (rowItem.isDirectory()) {

                            new ZipHelperTask(zipViewer,  stringBuilder.toString()).execute(zipViewer.s);

                        } else {
                            String x=rowItem.getName().substring(rowItem.getName().lastIndexOf("/")+1);
                            BaseFile file = new BaseFile(c.getCacheDir().getAbsolutePath() + "/" + x);
                                  file  .setMode(HFile.LOCAL_MODE);
                            zipViewer.files.clear();
                            zipViewer.files.add(0, file);

                            try {
                                ZipFile zipFile = new ZipFile(zipViewer.f);
                                new ZipExtractTask(zipFile, c.getCacheDir().getAbsolutePath(), zipViewer.getActivity(), x,true,rowItem.getEntry()).execute();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }}
                }}
        });
    }
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder vholder,final int position1) {
        if(zipMode){
            onBindView(vholder,position1);
            return;
        }
        final RarAdapter.ViewHolder holder = ((RarAdapter.ViewHolder)vholder);
        if (!this.stoppedAnimation)
        {
            animate(holder);
        }
        if(position1<0)return;
        final FileHeader rowItem = enter.get(position1);
        zipViewer.elementsRar.add(position1, headerRequired(rowItem));
        final int p = position1;

        GradientDrawable gradientDrawable = (GradientDrawable) holder.imageView.getBackground();

        holder.imageView.setImageDrawable(Icons.loadMimeIcon(zipViewer.getActivity(), rowItem.getFileNameString(), false,zipViewer.res));
        holder.txtTitle.setText(rowItem.getFileNameString().substring(rowItem.getFileNameString().lastIndexOf("\\") + 1));
        if (rowItem.isDirectory()) {
            holder.imageView.setImageDrawable(folder);
            gradientDrawable.setColor(Color.parseColor(zipViewer.iconskin));} else {
            if (zipViewer.coloriseIcons) {
                if (Icons.isVideo(rowItem.getFileNameString()) || Icons.isPicture(rowItem.getFileNameString()))
                    gradientDrawable.setColor(Color.parseColor("#f06292"));
                else if (Icons.isAudio(rowItem.getFileNameString()))
                    gradientDrawable.setColor(Color.parseColor("#9575cd"));
                else if (Icons.isPdf(rowItem.getFileNameString()))
                    gradientDrawable.setColor(Color.parseColor("#da4336"));
                else if (Icons.isCode(rowItem.getFileNameString()))
                    gradientDrawable.setColor(Color.parseColor("#00bfa5"));
                else if (Icons.isText(rowItem.getFileNameString()))
                    gradientDrawable.setColor(Color.parseColor("#e06055"));
                else if (Icons.isArchive(rowItem.getFileNameString()))
                    gradientDrawable.setColor(Color.parseColor("#f9a825"));
                else if(Icons.isApk(rowItem.getFileNameString()))
                    gradientDrawable.setColor(Color.parseColor("#a4c439"));
                else if (Icons.isgeneric(rowItem.getFileNameString()))
                    gradientDrawable.setColor(Color.parseColor("#9e9e9e"));
                else gradientDrawable.setColor(Color.parseColor(zipViewer.iconskin));
            } else gradientDrawable.setColor(Color.parseColor(zipViewer.iconskin));
        }


        holder.rl.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                final Animation animation = AnimationUtils.loadAnimation(zipViewer.getActivity(), R.anim.holder_anim);
                holder.imageView.setAnimation(animation);
                toggleChecked(p);
                return true;
            }
        });
        holder.imageView.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    final Animation animation = AnimationUtils.loadAnimation(zipViewer.getActivity(), R.anim.holder_anim);
                                                    holder.imageView.setAnimation(animation);
                                                    toggleChecked(p);
                                                }

                                            }
        );
        Boolean checked = myChecked.get(p);
        if (checked != null) {

            if (zipViewer.mainActivity.theme1 == 0) {

                holder.rl.setBackgroundResource(R.drawable.safr_ripple_white);
            } else {

                holder.rl.setBackgroundResource(R.drawable.safr_ripple_black);
            }
            holder.rl.setSelected(false);
            if (checked) {
                holder.imageView.setImageDrawable(zipViewer.getResources().getDrawable(R.drawable.abc_ic_cab_done_holo_dark));
                gradientDrawable.setColor(Color.parseColor("#757575"));
                holder.rl.setSelected(true);
            }
        }
        holder.rl.setOnClickListener(new View.OnClickListener() {

            public void onClick(View p1) {
                if(zipViewer.selection) {
                    final Animation animation = AnimationUtils.loadAnimation(zipViewer.getActivity(), R.anim.holder_anim);
                    holder.imageView.setAnimation(animation);
                    toggleChecked(p);
                }
                else {

                    if (rowItem.isDirectory()) {

                        zipViewer.elementsRar.clear();
                        new RarHelperTask(zipViewer,  rowItem.getFileNameString()).execute
                                (zipViewer.f);

                    }else {
                        if (headerRequired(rowItem)!=null) {
                            FileHeader fileHeader = headerRequired(rowItem);
                            BaseFile file1 = new BaseFile(c.getCacheDir().getAbsolutePath()
                                    + "/" + fileHeader.getFileNameString());
                                file1.setMode(HFile.LOCAL_MODE);
                            zipViewer.files.clear();
                            zipViewer.files.add(0, file1);
                            new ZipExtractTask(zipViewer.archive, c.getCacheDir().getAbsolutePath(),
                                    zipViewer.mainActivity, fileHeader.getFileNameString(), false, fileHeader).execute();
                        }

                    }
                }}
        });
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
        return false;}


    private FileHeader headerRequired(FileHeader rowItem) {

        for (FileHeader fileHeader : zipViewer.archive.getFileHeaders()) {
            String req = fileHeader.getFileNameString();
            if (rowItem.getFileNameString().equals(req))
                return fileHeader;
        }
        return null;
    }    @Override
         public int getItemCount() {
        return zipMode?enter1.size():enter.size();
    }

}

