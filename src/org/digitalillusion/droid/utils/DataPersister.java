package org.digitalillusion.droid.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.digitalillusion.droid.iching.R;
import org.digitalillusion.droid.utils.lists.HistoryEntry;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.util.Log;

/**
 * Implementor of the persistence for history and options
 *
 * @author digitalillusion
 */
public class DataPersister {

	/** Path of the local history resources **/
	private static final String ICHING_HISTORY_PATH = "/Android/data/org.digitalillusion.droid.iching/files/history.bin";
	
	/** Path of the options resources **/
	private static final String ICHING_OPTIONS_PATH = "/Android/data/org.digitalillusion.droid.iching/files/options.bin";
	
    /**
	 * @return True if media is readable, false otherwise
	 */
	public static boolean isSDReadable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    return true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    return true;
		}
		return false;
	}
	
	/**
	 * @return True if media is writable, false otherwise
	 */
	public static boolean isSDWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	/**
	 * Load history from SD card
	 * 
	 * @param historyList The history list to load
	 * 
	 * @throws IOException if SD is not readable
	 * @throws FileNotFoundException if no history was saved;
	 */
	@SuppressWarnings("unchecked")
	public static void loadHistory(ArrayList<HistoryEntry> historyList) throws IOException, FileNotFoundException {
		if (historyList.size() == 0) {
			if (!isSDReadable()) {
				throw new IOException();
			}
			File path = Environment.getExternalStorageDirectory();
			File historyFile = new File(path.getAbsolutePath() + ICHING_HISTORY_PATH) ;
			if (historyFile.exists()) {
				FileInputStream fis = new FileInputStream(historyFile);
				ObjectInputStream stream = new ObjectInputStream(fis); 
				try {
					ArrayList<HistoryEntry> persistedList = (ArrayList<HistoryEntry>) stream.readObject();
					historyList.addAll(persistedList);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					Log.e("IChingActivity.loadHistory()", e.getMessage());
				}
	    		if(historyList.size() == 0) {
	    			throw new FileNotFoundException();
	    		}
			} else {
				throw new FileNotFoundException();
			}
		}
	}
	
	/**
	 * Save history to SD card
	 * 
	 * @param historyList The history list to save
	 * @param activity The caller activity, needed to display popups (eventually)
	 * 
	 * @throws IOException if SD is not writable
	 */
	public static void saveHistory(final ArrayList<HistoryEntry> historyList, final Activity activity) {
		try {
			if (!isSDWritable()) {
				throw new IOException();
			}
			File path = Environment.getExternalStorageDirectory();
			File historyFile = new File(path.getAbsolutePath() + ICHING_HISTORY_PATH);
			if(!historyFile.exists()) {
				String absPath = historyFile.getAbsolutePath();
				File historyDir = new File(absPath.substring(0, absPath.lastIndexOf(File.separator)));
				historyDir.mkdirs();
				
				historyFile.createNewFile();
			}
			
			FileOutputStream fos = new FileOutputStream(historyFile);
			ObjectOutputStream stream = new ObjectOutputStream(fos); 
			stream.writeObject(historyList);
		} catch (IOException e) {
			AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
			alertDialog.setMessage(Utils.s(R.string.history_unsaveable));
			alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(R.string.retry), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					saveHistory(historyList, activity);
				} 
			});
			alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {} 
			}); 
		}
	}

	/**
	 * Load options from SD card
	 * 
	 * @param optionsMap The options map to load
	 * 
	 * @throws IOException if SD is not readable
	 * @throws FileNotFoundException if no options were saved;
	 */
	@SuppressWarnings("unchecked")
	public static void loadOptions(HashMap<String, Serializable> optionsMap) throws IOException, FileNotFoundException {
		if (optionsMap.size() == 0) {
			if (!isSDReadable()) {
				throw new IOException();
			}
			File path = Environment.getExternalStorageDirectory();
			File optionsFile = new File(path.getAbsolutePath() + ICHING_OPTIONS_PATH) ;
			if (optionsFile.exists()) {
				FileInputStream fis = new FileInputStream(optionsFile);
				ObjectInputStream stream = new ObjectInputStream(fis); 
				try {
					HashMap<String, Serializable> persistedMap = (HashMap<String, Serializable>) stream.readObject();
					for (Entry<String, Serializable> entry : persistedMap.entrySet()) {
						optionsMap.put(entry.getKey(), entry.getValue());
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					Log.e("IChingActivity.loadOptions()", e.getMessage());
				}
	    		if(optionsMap.size() == 0) {
	    			throw new FileNotFoundException();
	    		}
			} else {
				throw new FileNotFoundException();
			}
		}
	}
	
	/**
	 * Save options to SD card
	 * 
	 * @param optionsMap The options map to save
	 * @param activity The caller activity, needed to display popups (eventually)
	 * 
	 * @throws IOException if SD is not writable
	 */
	public static void saveOptions(final HashMap<String, Serializable> optionsMap, final Activity activity) {
		try {
			if (!isSDWritable()) {
				throw new IOException();
			}
			File path = Environment.getExternalStorageDirectory();
			File optionsFile = new File(path.getAbsolutePath() + ICHING_OPTIONS_PATH);
			if(!optionsFile.exists()) {
				String absPath = optionsFile.getAbsolutePath();
				File optionsDir = new File(absPath.substring(0, absPath.lastIndexOf(File.separator)));
				optionsDir.mkdirs();
				
				optionsFile.createNewFile();
			}
			
			FileOutputStream fos = new FileOutputStream(optionsFile);
			ObjectOutputStream stream = new ObjectOutputStream(fos); 
			stream.writeObject(optionsMap);
		} catch (IOException e) {
			AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
			alertDialog.setMessage(Utils.s(R.string.options_unsaveable));
			alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(R.string.retry), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					saveOptions(optionsMap, activity);
				} 
			});
			alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {} 
			}); 
		}
	}

	
}
