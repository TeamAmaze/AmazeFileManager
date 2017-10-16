package com.amaze.filemanager.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.holders.CompressedItemViewHolder;
import com.amaze.filemanager.asynchronous.services.ExtractService;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.fragments.ZipExplorerFragment;
import com.amaze.filemanager.ui.ZipObjectParcelable;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.views.CircleGradientDrawable;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.color.ColorUtils;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.github.junrar.rarfile.FileHeader;

import java.util.ArrayList;

/**
 * Created by Arpit on 25-01-2015 edited by Emmanuel Messulam<emmanuelbendavid@gmail.com>
 */
public class ZipExplorerAdapter extends RecyclerArrayAdapter<String, RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0, TYPE_ITEM = 1;

    public boolean stoppedAnimation = false;

    private Context context;
    private UtilitiesProviderInterface utilsProvider;
    private Drawable folder;
    private ArrayList<FileHeader> enterRar;
    private ArrayList<ZipObjectParcelable> enterZip;
    private ZipExplorerFragment zipExplorerFragment;
    private LayoutInflater mInflater;
    private boolean[] itemsChecked;
    private boolean zipMode = false;  // flag specify whether adapter is based on a Rar file or not
    private int offset = 0;

    public ZipExplorerAdapter(Context c, UtilitiesProviderInterface utilsProvider,
                              ArrayList<ZipObjectParcelable> enterZip, ArrayList<FileHeader> enterRar,
                              ZipExplorerFragment zipExplorerFragment, boolean isZip) {
        this.utilsProvider = utilsProvider;

        zipMode = isZip;

        if(zipMode) this.enterZip = enterZip;
        else this.enterRar = enterRar;

        itemsChecked = new boolean[zipMode? enterZip.size():enterRar.size()];

        context = c;
        if (c == null) return;
        folder = c.getResources().getDrawable(R.drawable.ic_grid_folder_new);
        this.zipExplorerFragment = zipExplorerFragment;
        mInflater = (LayoutInflater) c.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    }

    public void toggleChecked(boolean check) {
        int k = 0;

        for (int i = k; i < (zipMode ? enterZip.size() : enterRar.size()); i++) {
            itemsChecked[i] = check;
            notifyItemChanged(i);
        }
    }

    public ArrayList<Integer> getCheckedItemPositions() {
        ArrayList<Integer> checkedItemPositions = new ArrayList<>();

        for (int i = 0; i < itemsChecked.length; i++) {
            if (itemsChecked[i]) {
                (checkedItemPositions).add(i);
            }
        }

        return checkedItemPositions;
    }

    /**
     * called as to toggle selection of any item in adapter
     *
     * @param position  the position of the item
     * @param imageView the circular {@link CircleGradientDrawable} that is to be animated
     */
    private void toggleChecked(int position, ImageView imageView) {
        zipExplorerFragment.stopAnim();
        stoppedAnimation = true;

        Animation animation;
        if (itemsChecked[position]) {
            animation = AnimationUtils.loadAnimation(context, R.anim.check_out);
        } else {
            animation = AnimationUtils.loadAnimation(context, R.anim.check_in);
        }

        if (imageView != null) {
            imageView.setAnimation(animation);
        } else {
            // TODO: we don't have the check icon object probably because of config change
        }

        itemsChecked[position] = !itemsChecked[position];

        notifyDataSetChanged();
        if (!zipExplorerFragment.selection || zipExplorerFragment.mActionMode == null) {
            zipExplorerFragment.selection = true;
            /*zipExplorerFragment.mActionMode = zipExplorerFragment.getActivity().startActionMode(
                   zipExplorerFragment.mActionModeCallback);*/
            zipExplorerFragment.mActionMode = zipExplorerFragment.mainActivity.getAppbar().getToolbar().startActionMode(zipExplorerFragment.mActionModeCallback);
        }
        zipExplorerFragment.mActionMode.invalidate();
        if (getCheckedItemPositions().size() == 0) {
            zipExplorerFragment.selection = false;
            zipExplorerFragment.mActionMode.finish();
            zipExplorerFragment.mActionMode = null;
        }
    }

    private void animate(CompressedItemViewHolder holder) {
        holder.rl.clearAnimation();
        Animation localAnimation = AnimationUtils.loadAnimation(zipExplorerFragment.getActivity(), R.anim.fade_in_top);
        localAnimation.setStartOffset(this.offset);
        holder.rl.startAnimation(localAnimation);
        this.offset = (30 + this.offset);
    }

    public void generateRar(ArrayList<FileHeader> arrayList) {
        offset = 0;
        stoppedAnimation = false;
        enterRar = arrayList;
        notifyDataSetChanged();
        itemsChecked = new boolean[enterRar.size()];
    }

    public void generateZip(ArrayList<ZipObjectParcelable> arrayList) {
        offset = 0;
        stoppedAnimation = false;
        enterZip = arrayList;
        notifyDataSetChanged();
        itemsChecked = new boolean[enterZip.size()];
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position))
            return TYPE_HEADER;

        return TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = mInflater.inflate(R.layout.rowlayout, parent, false);
            v.findViewById(R.id.picture_icon).setVisibility(View.INVISIBLE);
            return new CompressedItemViewHolder(v);
        } else if(viewType == TYPE_ITEM) {
            View v = mInflater.inflate(R.layout.rowlayout, parent, false);
            CompressedItemViewHolder vh = new CompressedItemViewHolder(v);
            ImageButton about = (ImageButton) v.findViewById(R.id.properties);
            about.setVisibility(View.INVISIBLE);
            return vh;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder vholder, int position1) {
        if (!stoppedAnimation) {
            animate((CompressedItemViewHolder) vholder);
        }

        if (zipMode) onBindViewZip((CompressedItemViewHolder) vholder, position1);
        else onBindViewHolderRar((CompressedItemViewHolder) vholder, position1);
    }

    private void onBindViewZip(final CompressedItemViewHolder holder, final int position) {
        final ZipObjectParcelable rowItem = enterZip.get(position);
        GradientDrawable gradientDrawable = (GradientDrawable) holder.genericIcon.getBackground();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            holder.checkImageView.setBackground(new CircleGradientDrawable(zipExplorerFragment.accentColor,
                    utilsProvider.getAppTheme(), zipExplorerFragment.getResources().getDisplayMetrics()));
        } else
            holder.checkImageView.setBackgroundDrawable(new CircleGradientDrawable(zipExplorerFragment.accentColor,
                    utilsProvider.getAppTheme(), zipExplorerFragment.getResources().getDisplayMetrics()));

        if (rowItem.getEntry() == null) {
            holder.genericIcon.setImageDrawable(zipExplorerFragment.getResources().getDrawable(R.drawable.ic_arrow_left_white_24dp));
            gradientDrawable.setColor(Utils.getColor(context, R.color.goback_item));
            holder.txtTitle.setText("..");
            holder.txtDesc.setText("");
            holder.date.setText(R.string.goback);
        } else {
            holder.genericIcon.setImageDrawable(Icons.loadMimeIcon(rowItem.getName(), false, context.getResources()));
            final StringBuilder stringBuilder = new StringBuilder(rowItem.getName());
            if (zipExplorerFragment.showLastModified)
                holder.date.setText(Utils.getDate(rowItem.getTime(), zipExplorerFragment.year));
            if (rowItem.isDirectory()) {
                holder.genericIcon.setImageDrawable(folder);
                gradientDrawable.setColor(Color.parseColor(zipExplorerFragment.iconskin));
                if (stringBuilder.toString().length() > 0) {
                    stringBuilder.deleteCharAt(rowItem.getName().length() - 1);
                    try {
                        holder.txtTitle.setText(stringBuilder.toString().substring(stringBuilder.toString().lastIndexOf("/") + 1));
                    } catch (Exception e) {
                        holder.txtTitle.setText(rowItem.getName().substring(0, rowItem.getName().lastIndexOf("/")));
                    }
                }
            } else {
                if (zipExplorerFragment.showSize)
                    holder.txtDesc.setText(Formatter.formatFileSize(context, rowItem.getSize()));
                holder.txtTitle.setText(rowItem.getName().substring(rowItem.getName().lastIndexOf("/") + 1));
                if (zipExplorerFragment.coloriseIcons) {
                    ColorUtils.colorizeIcons(context, Icons.getTypeOfFile(rowItem.getName()),
                            gradientDrawable, Color.parseColor(zipExplorerFragment.iconskin));
                } else gradientDrawable.setColor(Color.parseColor(zipExplorerFragment.iconskin));
            }
        }

        holder.rl.setOnLongClickListener(view -> {
            if (rowItem.getEntry() != null) {
                toggleChecked(position, holder.checkImageView);
            }
            return true;
        });
        holder.genericIcon.setOnClickListener(view -> {
            if (rowItem.getEntry() != null) {
                toggleChecked(position, holder.checkImageView);
            }
        });
        if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {
            holder.rl.setBackgroundResource(R.drawable.safr_ripple_white);
        } else {
            holder.rl.setBackgroundResource(R.drawable.safr_ripple_black);
        }
        holder.rl.setSelected(false);
        if (itemsChecked[position]) {
            //holder.genericIcon.setImageDrawable(zipExplorerFragment.getResources().getDrawable(R.drawable.abc_ic_cab_done_holo_dark));
            holder.checkImageView.setVisibility(View.VISIBLE);
            gradientDrawable.setColor(Utils.getColor(context, R.color.goback_item));
            holder.rl.setSelected(true);
        } else holder.checkImageView.setVisibility(View.INVISIBLE);

        holder.rl.setOnClickListener(p1 -> {
            if (rowItem.getEntry() == null)
                zipExplorerFragment.goBack();
            else {
                if (zipExplorerFragment.selection) {
                    toggleChecked(position, holder.checkImageView);
                } else {
                    final StringBuilder stringBuilder = new StringBuilder(rowItem.getName());
                    if (rowItem.isDirectory())
                        stringBuilder.deleteCharAt(rowItem.getName().length() - 1);

                    if (rowItem.isDirectory()) {
                        zipExplorerFragment.changeZipPath(stringBuilder.toString());
                    } else {
                        String fileName = zipExplorerFragment.realZipFile.getName().substring(0,
                                zipExplorerFragment.realZipFile.getName().lastIndexOf("."));
                        String archiveCacheDirPath = zipExplorerFragment.getActivity().getExternalCacheDir().getPath() +
                                "/" + fileName;

                        HybridFileParcelable file = new HybridFileParcelable(archiveCacheDirPath + "/"
                                + rowItem.getName().replaceAll("\\\\", "/"));
                        file.setMode(OpenMode.FILE);
                        // this file will be opened once service finishes up it's extraction
                        zipExplorerFragment.files.add(file);
                        // setting flag for binder to know
                        zipExplorerFragment.isOpen = true;

                        Toast.makeText(zipExplorerFragment.getContext(),
                                zipExplorerFragment.getContext().getResources().getString(R.string.please_wait),
                                Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(zipExplorerFragment.getContext(), ExtractService.class);
                        ArrayList<String> a = new ArrayList<>();

                        // adding name of entry to extract from zip, before opening it
                        a.add(rowItem.getName());
                        intent.putExtra(ExtractService.KEY_PATH_ZIP, zipExplorerFragment.realZipFile.getPath());
                        intent.putExtra(ExtractService.KEY_ENTRIES_ZIP, a);
                        intent.putExtra(ExtractService.KEY_PATH_EXTRACT,
                                zipExplorerFragment.getActivity().getExternalCacheDir().getPath());
                        ServiceWatcherUtil.runService(zipExplorerFragment.getContext(), intent);
                    }
                }
            }
        });
    }

    private void onBindViewHolderRar(final CompressedItemViewHolder holder, int position) {
        if (position < 0) return;
        final FileHeader rowItem = enterRar.get(position);

        GradientDrawable gradientDrawable = (GradientDrawable) holder.genericIcon.getBackground();

        holder.genericIcon.setImageDrawable(Icons.loadMimeIcon(rowItem.getFileNameString(), false, context.getResources()));
        holder.txtTitle.setText(rowItem.getFileNameString().substring(rowItem.getFileNameString().lastIndexOf("\\") + 1));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            holder.checkImageView.setBackground(new CircleGradientDrawable(zipExplorerFragment.accentColor,
                    utilsProvider.getAppTheme(), zipExplorerFragment.getResources().getDisplayMetrics()));
        } else
            holder.checkImageView.setBackgroundDrawable(new CircleGradientDrawable(zipExplorerFragment.accentColor,
                    utilsProvider.getAppTheme(), zipExplorerFragment.getResources().getDisplayMetrics()));

        if (rowItem.isDirectory()) {
            holder.genericIcon.setImageDrawable(folder);
            gradientDrawable.setColor(Color.parseColor(zipExplorerFragment.iconskin));
        } else {
            if (zipExplorerFragment.coloriseIcons) {
                ColorUtils.colorizeIcons(context, Icons.getTypeOfFile(rowItem.getFileNameString()),
                        gradientDrawable, Color.parseColor(zipExplorerFragment.iconskin));
            } else gradientDrawable.setColor(Color.parseColor(zipExplorerFragment.iconskin));
        }

        holder.rl.setOnLongClickListener(view -> {
            toggleChecked(holder.getAdapterPosition(), holder.checkImageView);
            return true;
        });
        holder.genericIcon.setOnClickListener(view -> {
            toggleChecked(holder.getAdapterPosition(), holder.checkImageView);
        });
        if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {
            holder.rl.setBackgroundResource(R.drawable.safr_ripple_white);
        } else {
            holder.rl.setBackgroundResource(R.drawable.safr_ripple_black);
        }
        holder.rl.setSelected(false);
        if (itemsChecked[position]) {
            //holder.genericIcon.setImageDrawable(zipExplorerFragment.getResources().getDrawable(R.drawable.abc_ic_cab_done_holo_dark));
            holder.checkImageView.setVisibility(View.VISIBLE);
            gradientDrawable.setColor(Utils.getColor(context, R.color.goback_item));
            holder.rl.setSelected(true);
        } else holder.checkImageView.setVisibility(View.INVISIBLE);
        holder.rl.setOnClickListener(p1 -> {
            if (zipExplorerFragment.selection) {
                toggleChecked(holder.getAdapterPosition(), holder.checkImageView);
            } else {
                if (rowItem.isDirectory()) {
                    zipExplorerFragment.elementsRar.clear();
                    zipExplorerFragment.changeRarPath(rowItem.getFileNameString().replace("\\", "/"));
                } else {
                    String fileName = zipExplorerFragment.realZipFile.getName().substring(0,
                            zipExplorerFragment.realZipFile.getName().lastIndexOf("."));
                    String archiveCacheDirPath = zipExplorerFragment.getActivity().getExternalCacheDir().getPath() +
                            "/" + fileName;

                    HybridFileParcelable file1 = new HybridFileParcelable(archiveCacheDirPath + "/"
                            + rowItem.getFileNameString().replaceAll("\\\\", "/"));
                    file1.setMode(OpenMode.FILE);

                    // this file will be opened once service finishes up it's extraction
                    zipExplorerFragment.files.add(file1);
                    // setting flag for binder to know
                    zipExplorerFragment.isOpen = true;

                    Toast.makeText(zipExplorerFragment.getContext(),
                            zipExplorerFragment.getContext().getResources().getString(R.string.please_wait),
                            Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(zipExplorerFragment.getContext(), ExtractService.class);
                    ArrayList<String> a = new ArrayList<>();

                    // adding name of entry to extract from zip, before opening it
                    a.add(rowItem.getFileNameString());
                    intent.putExtra(ExtractService.KEY_PATH_ZIP, zipExplorerFragment.realZipFile.getPath());
                    intent.putExtra(ExtractService.KEY_ENTRIES_ZIP, a);
                    intent.putExtra(ExtractService.KEY_PATH_EXTRACT,
                            zipExplorerFragment.getActivity().getExternalCacheDir().getPath());
                    ServiceWatcherUtil.runService(zipExplorerFragment.getContext(), intent);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return zipMode ? enterZip.size() : enterRar.size();
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        ((CompressedItemViewHolder) holder).rl.clearAnimation();
    }

    @Override
    public boolean onFailedToRecycleView(RecyclerView.ViewHolder holder) {
        ((CompressedItemViewHolder) holder).rl.clearAnimation();
        return super.onFailedToRecycleView(holder);
    }

    private boolean isPositionHeader(int position) {
        return false;// TODO:
    }

    private FileHeader headerRequired(FileHeader rowItem) {
        if(zipExplorerFragment.archive != null) {
            for (FileHeader fileHeader : zipExplorerFragment.archive.getFileHeaders()) {
                String req = fileHeader.getFileNameString();
                if (rowItem.getFileNameString().equals(req))
                    return fileHeader;
            }
        }
        return null;
    }

}

