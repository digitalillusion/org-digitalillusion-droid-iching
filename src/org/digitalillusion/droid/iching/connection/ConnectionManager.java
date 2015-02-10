package org.digitalillusion.droid.iching.connection;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Resources.NotFoundException;
import android.os.AsyncTask;

import org.digitalillusion.droid.iching.IChingActivity;
import org.digitalillusion.droid.iching.IChingActivityRenderer;
import org.digitalillusion.droid.iching.R;
import org.digitalillusion.droid.iching.changinglines.ChangingLinesEvaluator;
import org.digitalillusion.droid.iching.utils.Consts;
import org.digitalillusion.droid.iching.utils.SettingsManager;
import org.digitalillusion.droid.iching.utils.SettingsManager.SETTINGS_MAP;
import org.digitalillusion.droid.iching.utils.Utils;
import org.digitalillusion.droid.iching.utils.lists.SettingsEntry;
import org.digitalillusion.droid.iching.utils.sql.HexSection;
import org.digitalillusion.droid.iching.utils.sql.HexSectionDataSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manager of the connection mode. When going from online to offline,
 * all hexagrams are downloaded for future offline reading.
 * Viceversa, all remote dictionaries are erased so that a new
 * incremental download can happen
 *
 * @author digitalillusion
 */
public class ConnectionManager {

  AsyncTask<Object, Object, Object> task;
  private ProgressDialog progressDialog;

  /**
   * Safely abort the current download
   *
   * @param activity To display a toast message
   */
  public void cleanUp(IChingActivityRenderer activity) {
    if (progressDialog != null && progressDialog.isShowing()) {
      progressDialog.dismiss();
      if (progressDialog.getProgress() < progressDialog.getMax() && task.cancel(true)) {
        activity.showToast(Utils.s(R.string.connection_on_to_off_abort));
      }
      task = null;
    }
  }

  /**
   * Handle switch to online mode, cleaning all downloaded definitions for the current dictionary and language
   *
   * @param activity    To display a confirmation dialog
   * @param successTask Operation to run if the user confirmed the switch
   */
  public void fromOfflineToOnline(final IChingActivity activity, final Runnable successTask) {
    SettingsManager settings = activity.getSettingsManager();
    final String dictionary = (String) settings.get(SETTINGS_MAP.DICTIONARY);
    final String lang = (String) settings.get(SETTINGS_MAP.LANGUAGE);
    if (dictionary.equals(Consts.DICTIONARY_CUSTOM)) {
      // Custom dictionary is never updated from remote
      successTask.run();
    } else if (settings.get(SETTINGS_MAP.CONNECTION_MODE).equals(Consts.CONNECTION_MODE_OFFLINE)) {
      AlertDialog confirmDialog = new AlertDialog.Builder(activity).create();
      confirmDialog.setTitle(getTitle(dictionary, lang));
      confirmDialog.setMessage(Utils.s(R.string.connection_off_to_on_confirm));
      confirmDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(R.string.yes), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
          HexSectionDataSource dataSource = activity.getHexSectionDataSource();
          for (int i = 0; i < Consts.HEX_COUNT; i++) {
            String hex = getHexFromIndex(i + 1);
            dataSource.deleteHexSections(hex, dictionary, lang);
          }
          RemoteResolver.clearCache();
          successTask.run();
        }
      });
      confirmDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.no), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
        }
      });
      confirmDialog.show();
    }
  }

  /**
   * Handle switch to offline mode, attempting a download of all the definitions for the current dictionary and language.
   *
   * @param activity    To display a toast message
   * @param successTask The operation to run once the download is finished successfully
   */
  public void fromOnlineToOffline(final IChingActivityRenderer activity, final Runnable successTask) {
    final SettingsManager settings = activity.getSettingsManager();
    final String dictionary = (String) settings.get(SETTINGS_MAP.DICTIONARY);
    final String lang = (String) settings.get(SETTINGS_MAP.LANGUAGE);
    final HexSectionDataSource dataSource = activity.getHexSectionDataSource();
    if (settings.get(SETTINGS_MAP.CONNECTION_MODE).equals(Consts.CONNECTION_MODE_ONLINE)) {
      if (task == null || task.isCancelled()) {
        task = new AsyncTask<Object, Object, Object>() {
          @Override
          protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(activity) {
              public void onBackPressed() {
                cleanUp(activity);
              }

              ;
            };
            progressDialog.setMessage(Utils.s(R.string.connection_on_to_off_download));
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMax(Consts.HEX_COUNT);
            progressDialog.setOnDismissListener(new OnDismissListener() {
              public void onDismiss(DialogInterface dialog) {
                cleanUp(activity);
              }
            });
            progressDialog.setTitle(getTitle(dictionary, lang));
            progressDialog.setProgress(1);
            progressDialog.show();
          }

          @Override
          protected Object doInBackground(Object... params) {
            List<Integer> specialCases = Arrays.asList(ChangingLinesEvaluator.ICHING_ALL_LINES_DESC);
            RemoteResolver.clearCache();
            for (int i = 0; i < Consts.HEX_COUNT && !isCancelled(); i++) {
              int hexIndex = i + 1;
              progressDialog.setProgress(hexIndex);
              String hex = getHexFromIndex(hexIndex);

              List<String> sections = new ArrayList<String>();
              sections.add(RemoteResolver.ICHING_REMOTE_SECTION_DESC);
              sections.add(RemoteResolver.ICHING_REMOTE_SECTION_JUDGE);
              sections.add(RemoteResolver.ICHING_REMOTE_SECTION_IMAGE);
              if (specialCases.contains(hexIndex)) {
                sections.add(RemoteResolver.ICHING_REMOTE_SECTION_LINE + ChangingLinesEvaluator.ICHING_APPLY_BOTH);
              }
              for (int j = 0; j < Consts.HEX_LINES_COUNT; j++) {
                sections.add(RemoteResolver.ICHING_REMOTE_SECTION_LINE + j);
              }

              for (String section : sections) {
                if (!isCancelled()) {
                  try {
                    HexSection currentHexSection = dataSource.getHexSection(hex, dictionary, lang, section);
                    if (currentHexSection.getDef().equals(Utils.EMPTY_STRING)) {
                      throw new NotFoundException();
                    }
                  } catch (NotFoundException e) {
                    String result = Utils.EMPTY_STRING;
                    do {
                      // Just retry upon failure
                      try {
                        result = RemoteResolver.downloadRemoteString(activity, hex, section, dictionary, lang);
                      } catch (IOException ioe) {
                      }
                    } while (result.equals(Utils.EMPTY_STRING) && !isCancelled());
                    if (!result.equals(Utils.EMPTY_STRING)) {
                      dataSource.updateHexSection(hex, dictionary, lang, section, result);
                    }
                  }
                }
              }
            }
            return null;
          }

          @Override
          protected void onPostExecute(Object result) {
            super.onPostExecute(result);
            cleanUp(activity);
            successTask.run();
          }
        };
        task.execute(new Object[0]);
      } else if (progressDialog != null && !progressDialog.isShowing()) {
        progressDialog.show();
      }
    }
  }

  private String getTitle(final String dictionary, final String lang) {
    return Utils.s(R.string.connection_off_to_on_confirm_title, new String[]{
        Utils.s(Utils.getResourceByName(R.string.class, SettingsEntry.DICTIONARY + Utils.UNDERSCORE + dictionary)),
        Utils.s(Utils.getResourceByName(R.string.class, SettingsEntry.LANGUAGE + Utils.UNDERSCORE + lang))
    });
  }

  private String getHexFromIndex(int hexIndex) {
    String hex = (hexIndex < 10 ? "0" : Utils.EMPTY_STRING) + hexIndex;
    return hex;
  }

}
