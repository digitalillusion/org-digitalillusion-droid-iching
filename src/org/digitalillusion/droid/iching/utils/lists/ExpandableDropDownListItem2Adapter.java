package org.digitalillusion.droid.iching.utils.lists;

import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import org.digitalillusion.droid.iching.R;

import java.util.List;

/**
 * Helper class to draw a ListItem on an expandable list that behaves like a drop down menu
 *
 * @author digitalillusion
 */
public abstract class ExpandableDropDownListItem2Adapter<T> extends BaseExpandableListAdapter {

  private LayoutInflater inflater;
  private List<T> list;
  private Context context;
  private ExpandableListView expandableListView;
  private float textSizeSmall;
  private float textSizeMedium;

  public ExpandableDropDownListItem2Adapter(Activity activity, ExpandableListView expandableListView, List<T> list) {
    this.inflater = (LayoutInflater) activity
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    this.list = list;
    this.context = activity.getApplicationContext();
    this.expandableListView = expandableListView;

    this.textSizeSmall = activity.getResources().getDimensionPixelSize(R.dimen.text_size_small);
    this.textSizeMedium = activity.getResources().getDimensionPixelSize(R.dimen.text_size_medium);
  }

  public abstract void childViewInit(TextView childView);

  public Object getChild(int groupPosition, int childPosition) {
    return list.get(childPosition);
  }

  public long getChildId(int groupPosition, int childPosition) {
    return childPosition;
  }

  public View getChildView(int groupPosition, int childPosition,
                           boolean isLastChild, View convertView, ViewGroup parent) {
    TextView row;
    if (convertView == null) {
      row = (TextView) inflater.inflate(
          android.R.layout.simple_list_item_1, null);
      childViewInit(row);
    } else {
      row = (TextView) convertView;
    }

    if (childPosition == 0) {
      row.setTextAppearance(context,
          android.R.style.TextAppearance_Small);
      row.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeSmall);
      row.setPadding((int) (2 * textSizeMedium), 0, 0, 0);
    } else {
      row.setTextAppearance(context,
          android.R.style.TextAppearance_Medium);
      row.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeMedium);
      row.setPadding((int) (2 * textSizeSmall), 0, 0, 0);
    }

    @SuppressWarnings("unchecked")
    T entry = (T) getItem(childPosition);
    row.setText(getText1(groupPosition, childPosition, entry));
    return row;
  }

  public int getChildrenCount(int groupPosition) {
    return list.size();
  }

  public int getCount() {
    if (list != null) {
      return list.size();
    } else {
      return 0;
    }
  }

  public Object getGroup(int groupPosition) {
    return null;
  }

  public int getGroupCount() {
    return 1;
  }

  public long getGroupId(int groupPosition) {
    return 0;
  }

  public View getGroupView(int groupPosition, boolean isExpanded,
                           View convertView, ViewGroup parent) {
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
      row.setPadding((int) (2 * textSizeSmall), 0, 0, 0);
    } else {
      row = (TwoLineListItem) convertView;
    }


    int childPosition = Math.max(0, expandableListView.getSelectedItemPosition());
    @SuppressWarnings("unchecked")
    T entry = (T) getItem(childPosition);

    row.getText1().setText(getText1(groupPosition, -1, entry));
    row.getText2().setText(getText2(groupPosition, -1, entry));

    return row;
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

  public int getItemViewType(int position) {
    return 0;
  }

  public List<T> getList() {
    return list;
  }

  public void setList(List<T> list) {
    this.list = list;
  }

  public abstract String getText1(int groupPosition, int childPosition, T entry);

  public abstract String getText2(int groupPosition, int childPosition, T entry);

  public View getView(int position, View convertView, ViewGroup parent) {
    TwoLineListItem row;
    if (convertView == null) {
      row = (TwoLineListItem) inflater.inflate(
          android.R.layout.simple_list_item_2, null);
      row.getText1().setTextAppearance(context,
          android.R.style.TextAppearance_Medium);
      row.getText1().setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeMedium);
      row.setPadding((int) (2 * textSizeSmall), 0, 0, 0);
    } else {
      row = (TwoLineListItem) convertView;
    }

    @SuppressWarnings("unchecked")
    T entry = (T) getItem(position);

    row.getText1().setText(getText1(position, position, entry));
    row.getText2().setText(getText2(position, position, entry));

    return row;
  }

  public int getViewTypeCount() {
    return 1;
  }

  public boolean hasStableIds() {
    return true;
  }

  public boolean isChildSelectable(int groupPosition, int childPosition) {
    return true;
  }

  public boolean isEnabled(int position) {
    return true;
  }
}
