package org.digitalillusion.droid.iching.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.digitalillusion.droid.iching.R;

/**
 * Replacement of the deprecated ProgressDialog: a dialog showing a message
 * and either a spinner or a progress bar
 *
 * @author digitalillusion
 */
public class ProgressPopup {

  private final AlertDialog dialog;
  private final ProgressBar progressBar;
  private final TextView tvCount;

  /**
   * A popup with a spinner
   *
   * @param activity The activity showing the popup
   * @param message  The message to display
   */
  public ProgressPopup(Activity activity, CharSequence message) {
    this(activity, null, message, 0);
  }

  /**
   * A popup with a progress bar
   *
   * @param activity The activity showing the popup
   * @param title    The popup title
   * @param message  The message to display
   * @param max      The progress at completion
   */
  public ProgressPopup(Activity activity, CharSequence title, CharSequence message, int max) {
    View view = LayoutInflater.from(activity).inflate(
        max > 0 ? R.layout.progress_horizontal : R.layout.progress_spinner, null);
    ((TextView) view.findViewById(R.id.tvProgressMessage)).setText(message);
    progressBar = view.findViewById(R.id.pbProgress);
    tvCount = view.findViewById(R.id.tvProgressCount);
    if (max > 0) {
      progressBar.setMax(max);
    }
    dialog = new AlertDialog.Builder(activity)
        .setTitle(title)
        .setView(view)
        .create();
  }

  public void dismiss() {
    dialog.dismiss();
  }

  public int getMax() {
    return progressBar.getMax();
  }

  public int getProgress() {
    return progressBar.getProgress();
  }

  public boolean isShowing() {
    return dialog.isShowing();
  }

  public void setCanceledOnTouchOutside(boolean cancel) {
    dialog.setCanceledOnTouchOutside(cancel);
  }

  public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
    dialog.setOnDismissListener(listener);
  }

  /**
   * Can be called from any thread
   *
   * @param progress The current progress
   */
  public void setProgress(final int progress) {
    progressBar.setProgress(progress);
    if (tvCount != null) {
      tvCount.post(() -> tvCount.setText(progress + "/" + progressBar.getMax()));
    }
  }

  public void show() {
    dialog.show();
  }
}
