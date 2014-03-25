package org.digitalillusion.droid.iching.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.digitalillusion.droid.iching.R;
import org.digitalillusion.droid.iching.utils.SettingsManager.SETTINGS_MAP;
import org.digitalillusion.droid.iching.utils.lists.HistoryEntry;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.util.Log;

/**
 * Implementor of the persistence for history and options
 *
 * @author digitalillusion
 */
public class DataPersister {

	/** Default history name */
	public static final String ICHING_HISTORY_PATH_FILENAME_DEFAULT = Utils.s(R.string.history_default);
	
	/** Path of the local history resources **/
	private static final String ICHING_SDCARD_FILES_PATH = "/Android/data/org.digitalillusion.droid.iching/files/";
	
	private static final String ICHING_HISTORY_PATH_FILENAME_PREFIX = "history";
	
	private static final String ICHING_HISTORY_PATH_FILENAME_SEPARATOR = "-";
	
	private static final String ICHING_HISTORY_PATH_FILENAME_EXT = ".bin";
	
	private static final String CRYPTO_ALG = "AES";
	
	private static final String CRYPTO_DIGEST = "SHA-1";
	
	private static final String PSWD_ENCODING = "UTF-8";
	
	
	/** Path of the options resources **/
	private static final String ICHING_OPTIONS_FILENAME = "options.bin";
	
	/** Path of the storage, either internal or sdcard **/
	private static File storagePath;

	private static String historyName;
	private static byte[] historyPassword;
	
	private static String revertHistoryName;
	private static byte[] revertHistoryPassword;
	
    /**
	 * @return The list of history names that the user has created, plus the default one
	 */
	public static List<String> getHistoryNames() {
		ArrayList<String> historyNames = new ArrayList<String>();
		historyNames.add(ICHING_HISTORY_PATH_FILENAME_DEFAULT);

		File[] files = storagePath.listFiles();

		for (int i = 0; files != null && i < files.length; i++) {
			File file = files[i];
		    if (file.isFile() && file.getName().startsWith(ICHING_HISTORY_PATH_FILENAME_PREFIX)) {
		        String name = file.getName().replace(ICHING_HISTORY_PATH_FILENAME_PREFIX, Utils.EMPTY_STRING);
		        name = name.substring(0, name.lastIndexOf('.'));
		        if (!name.isEmpty() && !name.equals(ICHING_HISTORY_PATH_FILENAME_SEPARATOR)) {
		        	historyNames.add(name.substring(ICHING_HISTORY_PATH_FILENAME_SEPARATOR.length()));
		        }
		    }
		}
		
		// Set selected history first
		historyNames.remove(getSelectedHistoryName());
		historyNames.add(0, historyName);
		
		return historyNames;
	}
	
	/**
	 * @return The name of the history currently used for persistence
	 */
	public static String getSelectedHistoryName() {
		if (DataPersister.historyName == null) {
			setSelectedHistory(ICHING_HISTORY_PATH_FILENAME_DEFAULT, Utils.EMPTY_STRING, true);
		}
		return historyName;
	}
	
	/**
	 * Load history from SD card
	 * 
	 * @param historyList The history list to load
	 * @param attempt The number of attempt done to load the history
	 * 
	 * @throws IOException if SD is not readable
	 * @throws FileNotFoundException if no history was saved;
	 * @throws InvalidKeyException if the password is wrong 
	 * @throws GeneralSecurityException if cryptography failed
	 */
	@SuppressWarnings("unchecked")
	public static void loadHistory(final List<HistoryEntry> historyList) throws IOException, FileNotFoundException, InvalidKeyException, GeneralSecurityException {
		historyList.clear();
		if (!isSDReadable()) {
			throw new IOException();
		}
		File historyFile = new File(storagePath.getAbsolutePath() + getHistoryPath()) ;
		if (historyFile.exists()) {
			
			FileInputStream fis = new FileInputStream(historyFile);
			byte[] encryptedData = Utils.getBytes(fis);
			
			byte[] decryptedData = null;
			// Default history cannot by password protected
			if (!historyName.equals(ICHING_HISTORY_PATH_FILENAME_DEFAULT) && historyPassword.length > 0) {
				Cipher c = Cipher.getInstance(CRYPTO_ALG);
				SecretKeySpec k =
					  new SecretKeySpec(historyPassword, CRYPTO_ALG);
				c.init(Cipher.DECRYPT_MODE, k);
				decryptedData = c.doFinal(encryptedData);
			} else {
				decryptedData = encryptedData;
			}
			
			try {
				ObjectInputStream stream = new BackwardCompatibleInputStream(new ByteArrayInputStream(decryptedData));
				try {
					ArrayList<HistoryEntry> persistedList = (ArrayList<HistoryEntry>) stream.readObject();
					// The first entry might be dummy, used only not to save an empty history file upon creation
					if (!persistedList.isEmpty() && Utils.isDummyHistoryEntry(persistedList.get(0))) {
						persistedList.remove(0);
					}
					historyList.addAll(persistedList);
				} catch (Exception e) {
					if (historyName.equals(ICHING_HISTORY_PATH_FILENAME_DEFAULT)) {
						// If default history is corrupted, factory reset
						e.printStackTrace();
						Log.e("IChingActivity.loadHistory()", e.getMessage());
						historyFile.delete();
						throw new IOException(e.getMessage());
					}
				}
			} catch (IOException e) {
				// If it cannot be converted, it is encripted with a different password
				throw new InvalidKeyException();
			}
		} else {
			throw new FileNotFoundException();
		}
	}
	
	/**
	 * Load options from SD card
	 * 
	 * @param context The base context
	 * @param optionsMap The options map to load
	 * 
	 * @throws IOException if SD is not readable
	 * @throws FileNotFoundException if no options were saved;
	 */
	@SuppressWarnings("unchecked")
	public static void loadOptions(Context context, HashMap<String, Serializable> optionsMap) throws IOException, FileNotFoundException {
		if (optionsMap.size() == 0) {
			if (!isSDReadable()) {
				throw new IOException();
			}

			if (storagePath == null) {
				// Search options file on internal storage first, then default to sd card
				storagePath = context.getFilesDir();
				File testFile = new File(storagePath.getAbsolutePath() + File.separator + ICHING_OPTIONS_FILENAME);
				if (!testFile.exists()) {
					storagePath = new File(Environment.getExternalStorageDirectory() + ICHING_SDCARD_FILES_PATH);
				}
			}
			
			File optionsFile = new File(storagePath + File.separator + ICHING_OPTIONS_FILENAME) ;
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
	 * Remove an existing history from SD card. Default history cannot be removed
     *
	 * @param activity The caller activity, needed to display popups (eventually)
	 * 
	 * @throws IOException if SD is not writable
	 */
	public static void removeHistory(final Activity activity) {
		try {
			if (!isSDWritable()) {
				throw new IOException();
			}

			File historyFile = new File(storagePath.getAbsolutePath() + getHistoryPath());
			if(historyFile.exists() && !historyName.equals(ICHING_HISTORY_PATH_FILENAME_DEFAULT)) {
				historyFile.delete();
			}		
		} catch (IOException e) {
			AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
			alertDialog.setMessage(Utils.s(R.string.history_unremovable));
			alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(R.string.retry), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					removeHistory(activity);
				} 
			});
			alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {} 
			}); 
		} finally {
			historyName = null;
			historyPassword = new byte[0];
		}
	}
	
	/**
	 * Rename the selected history. Default history cannot be renamed
     *
     * @param historyList The history list to save
	 * @param activity The caller activity, needed to display popups (eventually)
	 * @param newName the new name for the history
	 * 
	 */
	public static void renameHistory(final List<HistoryEntry> historyList, final Activity activity, final String newName) {
		byte[] backupPassword = new byte[DataPersister.historyPassword.length];
		String backupName = new String(DataPersister.historyName);
		if (!newName.equals(backupName)) {
			System.arraycopy(DataPersister.historyPassword, 0, backupPassword, 0, backupPassword.length);
			
			DataPersister.historyName = newName;
			DataPersister.saveHistory(historyList, activity);
			
			DataPersister.historyName = backupName;
			DataPersister.removeHistory(activity);
			
			DataPersister.historyName = newName;
			DataPersister.historyPassword = backupPassword;
			DataPersister.revertHistoryName = newName;
			DataPersister.revertHistoryPassword = backupPassword;
		}
	}

	/**
	 * Revert to the last saved history name and password, useful in case a switch has failed
	 * 
	 * @return True if history needed o be reverted, false otherwise
	 */
	public static boolean revertSelectedHistory() {
		boolean needRevert = !DataPersister.revertHistoryName.equals(DataPersister.historyName);
		if (needRevert) {
			DataPersister.historyName = DataPersister.revertHistoryName; 
			DataPersister.historyPassword = DataPersister.revertHistoryPassword;
		}
		return needRevert;
	}
		
	
	/**
	 * Save history to SD card
	 * 
	 * @param historyList The history list to save
	 * @param activity The caller activity, needed to display popups (eventually)
	 * 
	 * @return True if history was saved successfully, false otherwise
	 * 
	 * @throws IOException if SD is not writable
	 */
	public static boolean saveHistory(final List<HistoryEntry> historyList, final Activity activity) {
		try {
			if (!isSDWritable()) {
				throw new IOException();
			}

			File historyFile = new File(storagePath.getAbsolutePath() + getHistoryPath());
			if(!historyFile.exists()) {
				String absPath = historyFile.getAbsolutePath();
				File historyDir = new File(absPath.substring(0, absPath.lastIndexOf(File.separator)));
				historyDir.mkdirs();
				
				historyFile.createNewFile();
			}
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream stream = new ObjectOutputStream(baos); 
			stream.writeObject(historyList);
			
			byte[] encryptedData;
			if (historyPassword != null && historyPassword.length > 0) {
				Cipher c = Cipher.getInstance(CRYPTO_ALG);
				SecretKeySpec k = new SecretKeySpec(historyPassword, CRYPTO_ALG);
				c.init(Cipher.ENCRYPT_MODE, k);
				encryptedData = c.doFinal(baos.toByteArray());
			} else {
				encryptedData = baos.toByteArray();
			}

			FileOutputStream fos = new FileOutputStream(historyFile);
			fos.write(encryptedData);

			return true;
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
		} catch (GeneralSecurityException e) {
			AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
			alertDialog.setMessage(Utils.s(R.string.crypto_unavailable));
			alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {} 
			}); 
			alertDialog.show();
		}
		return false;
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

			File optionsFile = new File(storagePath.getAbsolutePath() + File.separator + ICHING_OPTIONS_FILENAME);
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
	
	/**
	 * Specify the name of the history to use for persistence
	 * 
	 * @param historyName The name of the history
	 * @param historyPassword The password of the history (can be empty string if not needed)
	 * @param reversible If true, the selection will be reversible 
	 * @throws InvalidParameterException if the format of the given password is not valid 
	 */
	public static void setSelectedHistory(String historyName, String historyPassword, boolean reversible) throws InvalidParameterException {
		// When changing history, keep backup of the latest working one
		if (!historyName.equals(DataPersister.historyName) || !reversible) {
			DataPersister.revertHistoryName = DataPersister.historyName; 
			DataPersister.revertHistoryPassword = DataPersister.historyPassword;
		}
		// Change history and password
		DataPersister.historyName = historyName;
		try {
			if (historyPassword == null || historyPassword.isEmpty()) {
				DataPersister.historyPassword = new byte[0];
			} else {
				MessageDigest sha = MessageDigest.getInstance(CRYPTO_DIGEST);
				byte[] key = sha.digest(historyPassword.getBytes(PSWD_ENCODING));
				key = Arrays.copyOf(key, 32); // use 256 bit
				
				DataPersister.historyPassword = key;
			}
		} catch (GeneralSecurityException e) {
			throw new InvalidParameterException(e.getMessage());
		} catch (UnsupportedEncodingException e) {
			throw new InvalidParameterException(e.getMessage());
		}
	}

	/**
	 * Set the DataPersister to use the internal storage
	 * 
	 * @param settings The settings manager
	 * @param context The basic context
	 *  
	 * @return True if storage is set to internal, false if an error prevented the setting
	 */
	public static boolean useStorageInternal(SettingsManager settings, Context context) {
		if (!Consts.STORAGE_INTERNAL.equals(settings.get(SETTINGS_MAP.STORAGE))) {
			File internalPath = context.getFilesDir();
			File currentDir = new File(storagePath.getAbsolutePath());
			if (changeStorage(currentDir, internalPath) && 
				currentDir.listFiles() != null && currentDir.listFiles().length > 0) {
				// Remove source files
				for (File f : currentDir.listFiles()) {
					f.delete();
				}
				storagePath = internalPath;
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Set the DataPersister to use the external SD card
	 * 
	 * @param settings The settings manager
	 * 
	 * @return True if storage is set to SD card, false if an error prevented the setting
	 */
	public static boolean useStorageSDCard(SettingsManager settings) {
		if (!Consts.STORAGE_SDCARD.equals(settings.get(SETTINGS_MAP.STORAGE))) {
			File currentDir = new File(storagePath.getAbsolutePath());
			File sdCardPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + 
					ICHING_SDCARD_FILES_PATH);
			if (changeStorage(currentDir, sdCardPath) &&
				currentDir.listFiles() != null && currentDir.listFiles().length > 0) {
				// Remove source files
				for (File f : currentDir.listFiles()) {
					f.delete();
				}
				storagePath = sdCardPath;
				return true;
			}
		}
		return false;
	}
	
	private static boolean changeStorage(File srcDir, File destDir) {
		File newStoragePath = new File(destDir.getAbsolutePath());
		newStoragePath.mkdirs();
		if (srcDir.listFiles() != null && srcDir.listFiles().length > 0) {
			try {			
				for (File f : srcDir.listFiles()) {
					if (f.isFile() && f.exists()) {
						File newPath = new File(newStoragePath.getAbsolutePath() + File.separatorChar + f.getName());
						Utils.copy(f.getAbsoluteFile(), newPath);
					}
				}
				return true;
			} catch (IOException e) {
				// Delete all files copied to destination
				for (File f : newStoragePath.listFiles()) {
					if (f.isFile() && f.exists()) {
						f.delete();
					}
				}
				return false;
			}
		}
		return true;
	}
	
	private static String getHistoryPath() {
		if(historyName == null || historyName.isEmpty() || historyName.equals(ICHING_HISTORY_PATH_FILENAME_DEFAULT)) {
			return File.separator + ICHING_HISTORY_PATH_FILENAME_PREFIX + ICHING_HISTORY_PATH_FILENAME_EXT;
		} else {
			return File.separator + ICHING_HISTORY_PATH_FILENAME_PREFIX + ICHING_HISTORY_PATH_FILENAME_SEPARATOR + historyName + ICHING_HISTORY_PATH_FILENAME_EXT;
		}
	}

	/**
	 * @return True if media is readable, false otherwise
	 */
	private static boolean isSDReadable() {
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
	private static boolean isSDWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	
}
