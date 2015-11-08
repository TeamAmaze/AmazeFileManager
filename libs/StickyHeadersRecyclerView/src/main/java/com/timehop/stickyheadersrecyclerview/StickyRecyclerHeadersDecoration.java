package com.timehop.stickyheadersrecyclerview;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;

import com.timehop.stickyheadersrecyclerview.caching.HeaderProvider;
import com.timehop.stickyheadersrecyclerview.caching.HeaderViewCache;
import com.timehop.stickyheadersrecyclerview.calculation.DimensionCalculator;
import com.timehop.stickyheadersrecyclerview.rendering.HeaderRenderer;
import com.timehop.stickyheadersrecyclerview.util.LinearLayoutOrientationProvider;
import com.timehop.stickyheadersrecyclerview.util.OrientationProvider;

public class StickyRecyclerHeadersDecoration extends RecyclerView.ItemDecoration {

  private final StickyRecyclerHeadersAdapter mAdapter;
  private final SparseArray<Rect> mHeaderRects = new SparseArray<>();
  private final HeaderProvider mHeaderProvider;
  private final OrientationProvider mOrientationProvider;
  private final HeaderPositionCalculator mHeaderPositionCalculator;
  private final HeaderRenderer mRenderer;
  private final DimensionCalculator mDimensionCalculator;

  // TODO: Consider passing in orientation to simplify orientation accounting within calculation
  public StickyRecyclerHeadersDecoration(StickyRecyclerHeadersAdapter adapter) {
    this(adapter, new LinearLayoutOrientationProvider(), new DimensionCalculator());
  }

  private StickyRecyclerHeadersDecoration(StickyRecyclerHeadersAdapter adapter, OrientationProvider orientationProvider,
      DimensionCalculator dimensionCalculator) {
    this(adapter, orientationProvider, dimensionCalculator, new HeaderRenderer(orientationProvider),
        new HeaderViewCache(adapter, orientationProvider));
  }

  private StickyRecyclerHeadersDecoration(StickyRecyclerHeadersAdapter adapter, OrientationProvider orientationProvider,
      DimensionCalculator dimensionCalculator, HeaderRenderer headerRenderer, HeaderProvider headerProvider) {
    this(adapter, headerRenderer, orientationProvider, dimensionCalculator, headerProvider,
        new HeaderPositionCalculator(adapter, headerProvider, orientationProvider,
            dimensionCalculator));
  }

  private StickyRecyclerHeadersDecoration(StickyRecyclerHeadersAdapter adapter, HeaderRenderer headerRenderer,
      OrientationProvider orientationProvider, DimensionCalculator dimensionCalculator, HeaderProvider headerProvider,
      HeaderPositionCalculator headerPositionCalculator) {
    mAdapter = adapter;
    mHeaderProvider = headerProvider;
    mOrientationProvider = orientationProvider;
    mRenderer = headerRenderer;
    mDimensionCalculator = dimensionCalculator;
    mHeaderPositionCalculator = headerPositionCalculator;
  }

  @Override
  public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
    super.getItemOffsets(outRect, view, parent, state);
    int itemPosition = parent.getChildPosition(view);
    if (mHeaderPositionCalculator.hasNewHeader(itemPosition)) {
      View header = getHeaderView(parent, itemPosition);
      setItemOffsetsForHeader(outRect, header, mOrientationProvider.getOrientation(parent));
    }
  }

  /**
   * Sets the offsets for the first item in a section to make room for the header view
   *
   * @param itemOffsets rectangle to define offsets for the item
   * @param header      view used to calculate offset for the item
   * @param orientation used to calculate offset for the item
   */
  private void setItemOffsetsForHeader(Rect itemOffsets, View header, int orientation) {
    Rect headerMargins = mDimensionCalculator.getMargins(header);
    if (orientation == LinearLayoutManager.VERTICAL) {
      itemOffsets.top = header.getHeight() + headerMargins.top + headerMargins.bottom;
    } else {
      itemOffsets.left = header.getWidth() + headerMargins.left + headerMargins.right;
    }
  }

  @Override
  public void onDrawOver(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
    super.onDrawOver(canvas, parent, state);
    mHeaderRects.clear();

    if (parent.getChildCount() <= 0 || mAdapter.getItemCount() <= 0) {
      return;
    }

    for (int i = 0; i < parent.getChildCount(); i++) {
      View itemView = parent.getChildAt(i);
      int position = parent.getChildPosition(itemView);
      if (hasStickyHeader(i, position) || mHeaderPositionCalculator.hasNewHeader(position)) {
        View header = mHeaderProvider.getHeader(parent, position);
        Rect headerOffset = mHeaderPositionCalculator.getHeaderBounds(parent, header,
            itemView, hasStickyHeader(i, position));
        mRenderer.drawHeader(parent, canvas, header, headerOffset);
        mHeaderRects.put(position, headerOffset);
      }
    }
  }

  private boolean hasStickyHeader(int listChildPosition, int indexInList) {
    if (listChildPosition > 0 || mAdapter.getHeaderId(indexInList) < 0) {
      return false;
    }

    return true;
  }

  /**
   * Gets the position of the header under the specified (x, y) coordinates.
   *
   * @param x x-coordinate
   * @param y y-coordinate
   * @return position of header, or -1 if not found
   */
  public int findHeaderPositionUnder(int x, int y) {
    for (int i = 0; i < mHeaderRects.size(); i++) {
      Rect rect = mHeaderRects.get(mHeaderRects.keyAt(i));
      if (rect.contains(x, y)) {
        return mHeaderRects.keyAt(i);
      }
    }
    return -1;
  }

  /**
   * Gets the header view for the associated position.  If it doesn't exist yet, it will be
   * created, measured, and laid out.
   *
   * @param parent
   * @param position
   * @return Header view
   */
  public View getHeaderView(RecyclerView parent, int position) {
    return mHeaderProvider.getHeader(parent, position);
  }

  /**
   * Invalidates cached headers.  This does not invalidate the recyclerview, you should do that manually after
   * calling this method.
   */
  public void invalidateHeaders() {
    mHeaderProvider.invalidate();
  }
}
