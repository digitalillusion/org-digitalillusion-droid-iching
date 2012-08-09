package org.digitalillusion.droid.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.digitalillusion.droid.iching.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.text.Html;
import android.text.Spanned;
import android.widget.TextView;

/**
 * Utility class to render remotely retrieved strings into TextViews
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
	
	/** URL of the remote string resources **/
	private static final String ICHING_REMOTE_URL = "http://androidiching.altervista.org/en/";
	
	/** Memory cache of remote strings **/
	private static HashMap<String, Spanned> remoteStringCache = new HashMap<String, Spanned>();
	
	/** A flag to avoid repeated "retry connection?" popups */
	private static boolean askRetry = true;
	
	/** A pointer to the last remote request **/
	private static AsyncTask<?,?,?> worker;
	
	/**
	 * Render a remote string on a TextView
	 * 
	 * @param component The component where to set the text
	 * @param hex The hexagram currently being evaluated
	 * @param section One of ICHING_REMOTE_SECTION_ flags
	 * @param retryAction The action to execute if the user wants to retry connection
	 * @param activity The caller activity, needed to display popups
	 * 
	 * @return A string from the server
	 */
	public static void renderRemoteString(final TextView component, final String hex, final String section, OnClickListener retryAction, Activity activity) {
		final String key = hex + section;
		component.setText("");
		final class RemoteWorker extends AsyncTask<Object, Integer, String> {
			@Override
			protected String doInBackground(Object... params) {
				InputStream is;
				try {
					is = Utils.downloadUrl(
						ICHING_REMOTE_URL,
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
				Spanned spanned;
				if (result.indexOf("\\e") > 0) {
					result = result.replaceAll(Utils.NEWLINE, "<br/>");
					spanned = Html.fromHtml(
						"<small><em>" + result.substring(0, result.indexOf("\\e")) + "</em><br/>" + 
						result.substring(result.indexOf("\\e") + 2) + "</small>"
			           );
				} else {
					spanned = Html.fromHtml("<small>" + Html.fromHtml(result) + "</small>");
				}
				component.setText(spanned);
				remoteStringCache.put(key, spanned);
			}
			
		}
		if (!remoteStringCache.containsKey(key)) {
			// Cancel any pending requests before issuing a new one
			if (worker != null) {
				worker.cancel(true);
				while (!worker.isCancelled()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {}
				}
			}
			
			ConnectivityManager connMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			try {
		        if (networkInfo != null && networkInfo.isConnected()) {
		        	worker = new RemoteWorker().execute(hex, section);
		        	worker = null;
		        } else {
		        	throw new ExecutionException(new Throwable());
		        }
			} catch(Throwable t) {
				if (askRetry) {
					AlertDialog networkExceptionDialog = new AlertDialog.Builder(activity).create();
					networkExceptionDialog.setMessage(Utils.s(R.string.remoteconn_unavailable));
					networkExceptionDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(R.string.retry), retryAction);
					networkExceptionDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.cancel), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							askRetry = false;
						} 
					});
					networkExceptionDialog.show();
				}
			}
		} else {
			Spanned spanned = remoteStringCache.get(key);
			component.setText(spanned);
		}
		
	}
	
}