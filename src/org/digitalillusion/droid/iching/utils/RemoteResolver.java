package org.digitalillusion.droid.iching.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.digitalillusion.droid.iching.IChingActivityRenderer;
import org.digitalillusion.droid.iching.R;
import org.digitalillusion.droid.iching.utils.SettingsManager.SETTINGS_MAP;
import org.digitalillusion.droid.iching.utils.sql.HexSection;
import org.digitalillusion.droid.iching.utils.sql.HexSectionDataSource;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources.NotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.text.Html;
import android.text.Spanned;
import android.widget.TextView;

/**
 * Utility class to render remotely retrieved strings into TextViews.
 * Once retrieved, those strings are placed in a local database.
 * For even better performance, as long as the activity lives they 
 * are cached in memory
 *
 * @author digitalillusion
 */
public class RemoteResolver {
	
	/** Description section identifier **/
	public static final String ICHING_REMOTE_SECTION_DESC 	= "desc";
	/** Judgment section identifier **/
	public static final String ICHING_REMOTE_SECTION_JUDGE = "judge";
	/** Image section identifier **/
	public static final String ICHING_REMOTE_SECTION_IMAGE = "image";
	/** Changing lines section identifier **/
	public static final String ICHING_REMOTE_SECTION_LINE 	= "line";
	
	/** Memory cache of remote strings **/
	private static HashMap<String, Spanned> remoteStringCache = new HashMap<String, Spanned>();
	
	/** A flag to avoid repeated "Retry network connection?" popups */
	private static boolean askRetry = true;

	/** A pointer to the last remote request **/
	private static AsyncTask<?,?,?> worker;
	
	/** The dictionaries definitions */
	@SuppressWarnings("serial")
	private static HashMap<String, String> dictionaries = new HashMap<String, String>() {{
		put(Consts.DICTIONARY_ALTERVISTA, "http://androidiching.altervista.org/");
	}};
	
	/**
	 * To be called to be sure that the "Retry network connection?" popup will be shown
	 */
	public static void prepareRetryPopup() {
		askRetry = true;
	}
	
	/**
	 * @param result The string received as result from the remote call
	 * @return A spanned conversion of the input string
	 */
	public static Spanned getSpannedFromRemoteString(String result) {
		Spanned spanned = null;
		if (result != null && result.indexOf(Utils.HEX_SECTION_QUOTE_DELIMITER) > 0) {
			result = result.replaceAll(Utils.NEWLINE, "<br/>");
			spanned = Html.fromHtml(
				"<small><em>" + result.substring(0, result.indexOf(Utils.HEX_SECTION_QUOTE_DELIMITER)) + "</em><br/>" + 
				result.substring(result.indexOf(Utils.HEX_SECTION_QUOTE_DELIMITER) + 2) + "</small>"
	           );
		} else {
			spanned = Html.fromHtml("<small>" + Html.fromHtml(result) + "</small>");
		}
		return spanned;
	}
	
	/**
	 * Render a remote string on a TextView
	 * 
	 * @param component The component where to set the text
	 * @param retryAction The action to execute if the user wants to retry connection
	 * @param activity The caller activity, needed to display popups
	 * 
	 * @return A string from the server
	 */
	public static void renderRemoteString(final TextView component, final OnClickListener retryAction, final IChingActivityRenderer activity) {
		final String hex = activity.getCurrentHex();
		final String section = activity.getCurrentSection();
		final String key = hex + section;
		component.setText("");
		
		final HexSectionDataSource dataSource = activity.getHexSectionDataSource();
		final String dictionary = (String) activity.getSettingsManager().get(SETTINGS_MAP.DICTIONARY);
		final String lang = (String) activity.getSettingsManager().get(SETTINGS_MAP.LANGUAGE);
		final class RemoteWorker extends AsyncTask<Object, Integer, String> {
			@Override
			protected String doInBackground(Object... params) {
				InputStream is;
				try {
					// If choice of dictionary is custom and we are requesting a remote string,
					// get the default localization on the default dictionary
					String langCode = lang;
					String remoteUrl;
					if (dictionary.equals(Consts.DICTIONARY_CUSTOM)) {
						langCode = (String) activity.getSettingsManager().getDefault(SETTINGS_MAP.LANGUAGE);
						remoteUrl = dictionaries.get((String) activity.getSettingsManager().getDefault(SETTINGS_MAP.DICTIONARY));
					} else {
						remoteUrl = dictionaries.get(dictionary);
					}
					
					is = Utils.downloadUrl(
						remoteUrl + langCode + "/",
						new String[] { "h", hex },
						new String[] { "s", section }
					);
					
					return Utils.streamToString(is);
				} catch (IOException e) {
					cancel(true);
					return null;
				}
			}
			
			@Override
			protected void onPostExecute(String result) {
				super.onPostExecute(result);
				askRetry = true;
				if (result != null) {
					Spanned spanned = getSpannedFromRemoteString(result);
					
					if (hex == activity.getCurrentHex() && section == activity.getCurrentSection()) {
						// If the request is still pending, proceed with component update
						if (!component.getText().equals(result)) {		
							component.setText(spanned);
						}
					} 
					// Store the result in cache
					remoteStringCache.put(key, spanned);
					dataSource.updateHexSection(hex, dictionary, lang, section, result);
				}
			}	
		}
		
		if (!remoteStringCache.containsKey(key)) {
			try {
				HexSection hs = dataSource.getHexSection(hex, dictionary, lang, section);
				if (hs.getDef() == null || hs.getDef().equals("")) {
					throw new NotFoundException();
				} else {
					Spanned spanned = getSpannedFromRemoteString(hs.getDef());
					component.setText(spanned);
				}
			} catch (NotFoundException nfe) {
				// Cancel any pending requests before issuing a new one
				if (worker != null) {
					worker.cancel(true);
				}
				
				ConnectivityManager connMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
				try {
			        if (networkInfo != null && networkInfo.isConnected()) {
			        	worker = new RemoteWorker().execute(hex, section);
			        } else {
			        	throw new ExecutionException(new Throwable());
			        }
				} catch(Throwable t) {
					if (askRetry) {
						askRetry = false;
						AlertDialog networkExceptionDialog = new AlertDialog.Builder(activity).create();
						networkExceptionDialog.setMessage(Utils.s(R.string.remoteconn_unavailable));
						networkExceptionDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(R.string.retry), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								askRetry = true;
								retryAction.onClick(dialog, which);
							} 
						});
						networkExceptionDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.cancel), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {} 
						});
						networkExceptionDialog.show();
					}
				}
			}
		} else {
			Spanned spanned = remoteStringCache.get(key);
			component.setText(spanned);
		}
		
	}
	
	/**
	 * Reset the definitions cache for the given hexagram. They will be downloaded again
	 * upon the first request
	 * 
	 * @param hex The hexagram to purge from cache
	 * @param activity The caller activity, needed to retrieve settings
	 */
	public static void resetCache(String hex, IChingActivityRenderer activity) {
		final HexSectionDataSource dataSource = activity.getHexSectionDataSource();
		final String lang = (String) activity.getSettingsManager().get(SETTINGS_MAP.LANGUAGE);
		dataSource.deleteHexSections(hex, lang);
		
		remoteStringCache.remove(hex + ICHING_REMOTE_SECTION_DESC);
		remoteStringCache.remove(hex + ICHING_REMOTE_SECTION_JUDGE);
		remoteStringCache.remove(hex + ICHING_REMOTE_SECTION_IMAGE);
		
		for (int i = 1; i <= 6; i++) {
			remoteStringCache.remove(hex + ICHING_REMOTE_SECTION_LINE);
		}
		
	}
	
	/**
	 * Reset the definition cache for the given hexagram section. It will be downloaded again
	 * upon the first request
	 * 
	 * @param hex The hexagram in question
	 * @param section The hexagram section to purge from cache
	 * @param activity The caller activity, needed to retrieve settings
	 */
	public static void resetCache(String hex, String section, IChingActivityRenderer activity) {
		final HexSectionDataSource dataSource = activity.getHexSectionDataSource();
		final String lang = (String) activity.getSettingsManager().get(SETTINGS_MAP.LANGUAGE);
		dataSource.deleteHexSection(hex, section, lang);
		remoteStringCache.remove(hex + section);
	}
	
}