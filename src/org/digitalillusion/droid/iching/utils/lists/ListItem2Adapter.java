package org.digitalillusion.droid.iching.utils.lists;

import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TwoLineListItem;

import org.digitalillusion.droid.iching.R;

import java.util.List;

/**
 * Helper class to draw a ListItem2 list
 *
 * @author digitalillusion
 */
public abstract class ListItem2Adapter<T> extends BaseAdapter {

  LayoutInflater inflater;
  List<T> list;
  Context context;
  private float textSizeMedium;
  private float textSizeSmall;

  public ListItem2Adapter(Activity activity, List<T> list) {
    this.inflater = (LayoutInflater) activity
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    this.list = list;
    this.context = activity.getApplicationContext();
    this.textSizeMedium = activity.getResources().getDimensionPixelSize(R.dimen.text_size_medium);
    this.textSizeSmall = activity.getResources().getDimensionPixelSize(R.dimen.text_size_small);
  }

  public int getCount() {
    if (list != null) {
      return list.size();
    } else {
      return 0;
    }
  }

  public Object getItem(int position) {
    if (list != null) {
      return list.get(position);
    } else {
      return null;
    }
  }

  public long getItemId(int position) {
    return position;
  }

  public abstract String getText1(T entry);

  public abstract String getText2(T entry);

  public View getView(int position, View convertView, ViewGroup parent) {
    TwoLineListItem row;
    if (convertView == null) {
      row = (TwoLineListItem) inflater.inflate(
          android.R.layout.simple_list_item_2, null);
      row.getText1().setTextAppearance(context,
          android.R.style.TextAppearance_Medium);
      row.getText1().setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeMedium);
      row.getText2().setTextAppearance(context,
          android.R.style.TextAppearance_Small);
      row.getText2().setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeSmall);
    } else {
      row = (TwoLineListItem) convertView;
    }

    @SuppressWarnings("unchecked")
    T entry = (T) getItem(position);

    row.getText1().setText(getText1(entry));
    row.getText2().setText(getText2(entry));

    return row;
  }
}
