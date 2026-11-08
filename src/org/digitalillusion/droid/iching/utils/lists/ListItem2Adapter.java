package org.digitalillusion.droid.iching.utils.lists;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import org.digitalillusion.droid.iching.IChingActivityRenderer;
import org.digitalillusion.droid.iching.R;
import org.digitalillusion.droid.iching.utils.Utils;

import java.util.List;

/**
 * Helper class to draw a ListItem2 list
 *
 * @author digitalillusion
 */
public abstract class ListItem2Adapter<T> extends BaseAdapter {

  LayoutInflater inflater;
  List<T> list;
  IChingActivityRenderer context;
  private float textSizeMedium;
  private float textSizeSmall;

  public ListItem2Adapter(IChingActivityRenderer activity, List<T> list) {
    this.inflater = (LayoutInflater) activity
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    this.list = list;
    this.context = activity;
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
    View row = convertView;
    if (row == null) {
      row = inflater.inflate(android.R.layout.simple_list_item_2, null);
      TextView text1 = row.findViewById(android.R.id.text1);
      TextViewCompat.setTextAppearance(text1, android.R.style.TextAppearance_Medium);
      text1.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeMedium);
      TextView text2 = row.findViewById(android.R.id.text2);
      TextViewCompat.setTextAppearance(text2, android.R.style.TextAppearance_Small);
      text2.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeSmall);
    }
    TextView text1 = row.findViewById(android.R.id.text1);
    TextView text2 = row.findViewById(android.R.id.text2);

    @SuppressWarnings("unchecked")
    T entry = (T) getItem(position);

    if (Utils.isDarkMode(context.getSettingsManager())) {
      text1.setTextColor(ContextCompat.getColor(context, R.color.text_primary_dark));
      text2.setTextColor(ContextCompat.getColor(context, R.color.text_secondary_dark));
    }
    text1.setText(getText1(entry));
    text2.setText(getText2(entry));

    return row;
  }
}
