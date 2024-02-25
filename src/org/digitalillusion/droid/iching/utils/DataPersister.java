package org.digitalillusion.droid.iching.utils;

import static androidx.core.content.ContextCompat.startActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;

import android.util.Log;

import androidx.core.content.FileProvider;

import org.digitalillusion.droid.iching.BuildConfig;
import org.digitalillusion.droid.iching.IChingActivityRenderer;
import org.digitalillusion.droid.iching.R;
import org.digitalillusion.droid.iching.utils.SettingsManager.SETTINGS_MAP;
import org.digitalillusion.droid.iching.utils.lists.HistoryEntry;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Implementor of the persistence for history and options
 *
 * @author digitalillusion
 */
public class DataPersister {

  /**
   * Default history name
   */
  public static final String ICHING_HISTORY_PATH_FILENAME_DEFAULT = Utils.s(R.string.history_default);


  /**
   * Path of the local history resources *
   */
  private static final String ICHING_SDCARD_FILES_PATH = "/Android/data/" + BuildConfig.APPLICATION_ID + "/files";

  private static final String ICHING_HISTORY_PATH_FILENAME_PREFIX = "history";

  private static final String ICHING_HISTORY_PATH_FILENAME_SEPARATOR = "-";

  private static final String ICHING_HISTORY_PATH_FILENAME_EXT = ".bin";

  private static final String CRYPTO_ALG = "AES";

  private static final String CRYPTO_DIGEST = "SHA-1";

  private static final String PSWD_ENCODING = "UTF-8";


  /**
   * Path of the options resources *
   */
  private static final String ICHING_OPTIONS_FILENAME = "options.bin";

  /**
   * Path of the storage, either internal or sdcard *
   */
  private static File storagePath;

  private static String historyName;
  private static byte[] historyPassword;

  private static String revertHistoryName;
  private static byte[] revertHistoryPassword;

  /**
   * Used to throw exception only once on fail load history *
   */
  private static boolean failLoadHistory = false;
  /**
   * Used to throw exception only once on fail load options *
   */
  private static boolean failLoadOptions = false;

  /**
   * The default history filename
   */
  private static String defaultHistoryFilename;

  /**
   * @return The list of history names that the user has created, plus the default one
   */
  public static List<String> getHistoryNames() {
    ArrayList<String> historyNames = new ArrayList<>();
    historyNames.add(ICHING_HISTORY_PATH_FILENAME_DEFAULT);

    List<String> customHistoryNames = new ArrayList<>();
    if (storagePath != null) {
      File[] files = storagePath.listFiles();

      for (int i = 0; files != null && i < files.length; i++) {
        File file = files[i];
        if (file.isFile() && file.getName().startsWith(ICHING_HISTORY_PATH_FILENAME_PREFIX)) {
          String name = file.getName().replace(ICHING_HISTORY_PATH_FILENAME_PREFIX, Utils.EMPTY_STRING);
          name = name.substring(0, name.lastIndexOf('.'));
          if (!name.isEmpty() && !name.equals(ICHING_HISTORY_PATH_FILENAME_SEPARATOR)) {
            String nameEntry = name.substring(ICHING_HISTORY_PATH_FILENAME_SEPARATOR.length());
            historyNames.add(nameEntry);
            customHistoryNames.add(nameEntry);
          }
        }
      }
    }

    // Set selected history first
    historyNames.remove(getSelectedHistoryName());
    historyNames.add(0, historyName);

    // Find default history filename
    for (String historyName : historyNames) {
      if (!customHistoryNames.contains(historyName)) {
        defaultHistoryFilename = historyName;
        break;
      }
    }

    return historyNames;
  }

  /**
   * @return The name of the default history file created
   */
  public static String getDefaultHistoryFilename() {
    return defaultHistoryFilename;
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
   * Load history
   *
   * @param historyList The history list to load
   * @throws IOException              if path is not readable
   * @throws FileNotFoundException    if no history was saved;
   * @throws InvalidKeyException      if the password is wrong
   * @throws GeneralSecurityException if cryptography failed
   */
  @SuppressWarnings("unchecked")
  public static synchronized void loadHistory(final List<HistoryEntry> historyList) throws IOException, FileNotFoundException, InvalidKeyException, GeneralSecurityException {
    if (!failLoadHistory && storagePath == null) {
      failLoadHistory = true;
      throw new IOException("Switched storage due to I/O error");
    }

    historyList.clear();
    if (storagePath != null) {
      File historyFile = new File(storagePath.getAbsolutePath() + getHistoryPath());
      if (historyFile.exists()) {

        FileInputStream fis = new FileInputStream(historyFile);
        byte[] encryptedData = Utils.getBytes(fis);

        byte[] decryptedData;
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
              Log.e("DataPersister", e.getMessage());
              historyFile.delete();
              throw new IOException(e.getMessage());
            }
          }
        } catch (IOException e) {
          // If it cannot be converted, it is encripted with a different password
          throw new InvalidKeyException();
        }
      } else if (!historyName.equals(ICHING_HISTORY_PATH_FILENAME_DEFAULT)) {
        throw new FileNotFoundException();
      }
    } else {
      throw new FileNotFoundException();
    }
  }

  /**
   * Load options
   *
   * @param context    The activity
   * @param optionsMap The options map to load
   * @throws IOException           if load fails due to an IO error
   * @throws FileNotFoundException if no options were saved;
   */
  @SuppressWarnings("unchecked")
  public static synchronized void loadOptions(IChingActivityRenderer context, HashMap<String, Serializable> optionsMap) throws IOException, FileNotFoundException {
    initStoragePath(context, optionsMap);
    if (!failLoadOptions && storagePath == null) {
      failLoadOptions = true;
      throw new IOException("Switched storage due to I/O error");
    }

    File optionsFile = new File(storagePath + File.separator + ICHING_OPTIONS_FILENAME);
    FileInputStream fis = new FileInputStream(optionsFile);
    ObjectInputStream stream = new ObjectInputStream(fis);
    try {
      HashMap<String, Serializable> persistedMap = (HashMap<String, Serializable>) stream.readObject();
      for (Entry<String, Serializable> entry : persistedMap.entrySet()) {
        // Do not override the storage setting set above
        if (!SettingsManager.SETTINGS_MAP.STORAGE.getKey().equals(entry.getKey())) {
          optionsMap.put(entry.getKey(), entry.getValue());
        }
      }
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      Log.e("DataPersister", e.getMessage());
    }
    if (optionsMap.size() == 0) {
      throw new FileNotFoundException();
    }
  }

  /**
   * Remove an existing history from SD card. Default history cannot be removed
   *
   * @param activity The caller activity, needed to display popups (eventually)
   */
  public static void removeHistory(final Activity activity) {
    try {
      if (!isSDWritable()) {
        throw new IOException();
      }

      File historyFile = new File(storagePath.getAbsolutePath() + getHistoryPath());
      if (historyFile.exists() && !historyName.equals(ICHING_HISTORY_PATH_FILENAME_DEFAULT)) {
        historyFile.delete();
      }
    } catch (IOException e) {
      AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
      alertDialog.setMessage(Utils.s(R.string.history_unremovable));
      alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(R.string.retry), (dialog, which) -> removeHistory(activity));
      alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.cancel), (dialog, which) -> {
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
   * @param activity    The caller activity, needed to display popups (eventually)
   * @param newName     the new name for the history
   */
  public static void renameHistory(final List<HistoryEntry> historyList, final IChingActivityRenderer activity, final String newName) {
    byte[] backupPassword = new byte[DataPersister.historyPassword.length];
    String backupName = DataPersister.historyName;
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
    boolean needRevert = DataPersister.revertHistoryName != null && !DataPersister.revertHistoryName.equals(DataPersister.historyName);
    if (needRevert) {
      DataPersister.historyName = DataPersister.revertHistoryName;
      DataPersister.historyPassword = DataPersister.revertHistoryPassword;
    }
    return needRevert;
  }

  /**
   * Save history
   *
   * @param historyList The history list to save
   * @param activity    The caller activity, needed to display popups (eventually)
   * @return True if history was saved successfully, false otherwise
   */
  public static synchronized boolean saveHistory(final List<HistoryEntry> historyList, final IChingActivityRenderer activity) {
    try {
      if (storagePath != null) {
        File historyFile = new File(storagePath.getAbsolutePath() + getHistoryPath());
        if (!historyFile.exists()) {
          String absPath = historyFile.getAbsolutePath();
          File historyDir = new File(absPath.substring(0, absPath.lastIndexOf(File.separator) + 1));
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
      }
      return false;
    } catch (IOException e) {
      AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
      alertDialog.setMessage(Utils.s(R.string.history_unsaveable));
      alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(R.string.retry), (dialog, which) -> saveHistory(historyList, activity));
      alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.cancel), (dialog, which) -> {
      });
      alertDialog.show();
    } catch (GeneralSecurityException e) {
      AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
      alertDialog.setMessage(Utils.s(R.string.crypto_unavailable));
      alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.cancel), (dialog, which) -> {
      });
      alertDialog.show();
    }
    return false;
  }


  /**
   * Save options
   *
   * @param activity   The caller activity, needed to display popups (eventually)
   * @param optionsMap The options map to save
   */
  public static synchronized void saveOptions(final IChingActivityRenderer activity, final HashMap<String, Serializable> optionsMap) {
    try {
      initStoragePath(activity, optionsMap);

      File optionsFile = new File(storagePath.getAbsolutePath() + File.separator + ICHING_OPTIONS_FILENAME);
      if (!optionsFile.exists()) {
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
      alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(R.string.retry), (dialog, which) -> saveOptions(activity, optionsMap));
      alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.cancel), (dialog, which) -> {
      });
    }
  }

  /**
   * Specify the name of the history to use for persistence
   *
   * @param historyName     The name of the history
   * @param historyPassword The password of the history (can be empty string if not needed)
   * @param reversible      If true, the selection will be reversible
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
   * @param activity  The activity context
   * @param settings The settings manager
   * @return True if storage is set to internal, false if an error prevented the setting
   */
  public static boolean useStorageInternal(IChingActivityRenderer activity, SettingsManager settings) {
    if (!Consts.STORAGE_INTERNAL.equals(settings.get(SETTINGS_MAP.STORAGE))) {
      activity.getSettingsManager().put(SETTINGS_MAP.STORAGE, Consts.STORAGE_INTERNAL);
      File internalPath = activity.getFilesDir();
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
   * @param activity The activity context
   * @param settings The settings manager
   * @return True if storage is set to SD card, false if an error prevented the setting
   */
  public static boolean useStorageSDCard(IChingActivityRenderer activity, SettingsManager settings) {
    if (isSDWritable() && !Consts.STORAGE_SDCARD.equals(settings.get(SETTINGS_MAP.STORAGE))) {
      activity.getSettingsManager().put(SETTINGS_MAP.STORAGE, Consts.STORAGE_SDCARD);
      File currentDir = new File(storagePath.getAbsolutePath());
      String externalDir = activity.getExternalFilesDir(null).getAbsolutePath();
      if (!externalDir.endsWith(ICHING_SDCARD_FILES_PATH)) {
        externalDir += ICHING_SDCARD_FILES_PATH;
      }
      File sdCardPath = new File(externalDir);
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
        if (newStoragePath.listFiles() != null) {
          for (File f : newStoragePath.listFiles()) {
            if (f.isFile() && f.exists()) {
              f.delete();
            }
          }
        }
        return false;
      }
    }
    return true;
  }

  /**
   * Create backup
   *
   * @param activity   The caller activity, needed to display popups (eventually)
   */
  public static synchronized void createBackup(final IChingActivityRenderer activity) {
    File currentDir = new File(storagePath.getAbsolutePath());
    int BUFFER = 256;
    try {
      BufferedInputStream origin;
      Format formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT);
      String destPath = currentDir.getAbsolutePath() + "/IChing-backup-" + formatter.format(new Date()) + ".zip";
      File destFile = new File(destPath);
      FileOutputStream dest = new FileOutputStream(destFile);
      ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
              dest));
      byte[] data = new byte[BUFFER];

      File[] files = currentDir.listFiles((dir, name) -> name.startsWith(ICHING_HISTORY_PATH_FILENAME_PREFIX));
      for (File file : files) {
        FileInputStream fi = new FileInputStream(file);
        origin = new BufferedInputStream(fi, BUFFER);

        String path = file.getAbsolutePath();
        ZipEntry entry = new ZipEntry(path.substring(path.lastIndexOf("/") + 1));
        out.putNextEntry(entry);
        int count;

        while ((count = origin.read(data, 0, BUFFER)) != -1) {
          out.write(data, 0, count);
        }
        origin.close();
      }

      out.close();

      Intent intent = new Intent(Intent.ACTION_SEND);
      intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      Uri uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID, destFile);
      intent.setType("application/zip");
      intent.putExtra(Intent.EXTRA_STREAM, uri);
      activity.startActivity(Intent.createChooser(intent, Utils.s(R.string.read_share_using)));
    } catch (Exception e) {
      AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
      alertDialog.setMessage(Utils.s(R.string.backup_error_create));
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.cancel), (dialog, which) -> {
      });
      alertDialog.show();
    }
  }

  /**
   * Restore backup
   *
   * @param activity The caller activity, needed to display popups (eventually)
   * @param uri The uri of the backup zip file to extract
   */
  public static synchronized void restoreBackup(final IChingActivityRenderer activity, Uri uri) {
    AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
    alertDialog.setMessage(Utils.s(R.string.backup_restore_confirm));
    alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, Utils.s(R.string.yes), (dialog, which) -> {
      try {

        InputStream is = activity.getContentResolver().openInputStream(uri);
        ZipInputStream zin = new ZipInputStream(is);
        ZipEntry ze;

        File currentDir = new File(storagePath.getAbsolutePath());
        for (File f : currentDir.listFiles(f -> f.isFile() && f.exists() && f.getName().startsWith(ICHING_HISTORY_PATH_FILENAME_PREFIX))) {
          f.delete();
        }
        while ((ze = zin.getNextEntry()) != null) {
          File out = new File(storagePath.getAbsolutePath() + File.separator + ze.getName());
          if (!out.getCanonicalPath().startsWith(currentDir.getCanonicalPath())) {
            throw new SecurityException();
          }
          FileOutputStream fout = new FileOutputStream(out);
          for (int c = zin.read(); c != -1; c = zin.read()) {
            fout.write(c);
          }

          zin.closeEntry();
          fout.close();
        }
        zin.close();

        setSelectedHistory(ICHING_HISTORY_PATH_FILENAME_DEFAULT, Utils.EMPTY_STRING, true);
        CharSequence text = Utils.s(
            R.string.backup_restore_done
        );
        activity.showToast(text);
      } catch (Exception e) {
        AlertDialog alertDialogErr = new AlertDialog.Builder(activity).create();
        alertDialogErr.setMessage(Utils.s(R.string.backup_error_restore));
        alertDialogErr.show();
      }
    });
    alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, Utils.s(R.string.cancel), (dialog, which) -> {});
    alertDialog.show();


  }

  private static String getHistoryPath() {
    if (historyName == null || historyName.isEmpty() || historyName.equals(ICHING_HISTORY_PATH_FILENAME_DEFAULT)) {
      return File.separator + ICHING_HISTORY_PATH_FILENAME_PREFIX + ICHING_HISTORY_PATH_FILENAME_EXT;
    } else {
      return File.separator + ICHING_HISTORY_PATH_FILENAME_PREFIX + ICHING_HISTORY_PATH_FILENAME_SEPARATOR + historyName + ICHING_HISTORY_PATH_FILENAME_EXT;
    }
  }

  private static void initStoragePath(IChingActivityRenderer context, HashMap<String, Serializable> optionsMap) {
    String storageOptionKey = SettingsManager.SETTINGS_MAP.STORAGE.getKey();
    Serializable storageOptionValue = optionsMap.get(storageOptionKey);
    if (storageOptionValue == null) {
      File optionsFile = new File(context.getFilesDir() + File.separator + ICHING_OPTIONS_FILENAME);
      if (optionsFile.exists()) {
        storageOptionValue = Consts.STORAGE_INTERNAL;
      } else if (isSDWritable()) {
        String externalDir = context.getExternalFilesDir(null).getAbsolutePath();
        if (!externalDir.endsWith(ICHING_SDCARD_FILES_PATH)) {
          externalDir += ICHING_SDCARD_FILES_PATH;
        }
        optionsFile = new File(externalDir + File.separator + ICHING_OPTIONS_FILENAME);
        if (optionsFile.exists()) {
          storageOptionValue = Consts.STORAGE_SDCARD;
        }
      } else {
        storageOptionValue = context.getSettingsManager().getDefault(SettingsManager.SETTINGS_MAP.STORAGE);
      }
    }
    if (Consts.STORAGE_INTERNAL.equals(storageOptionValue)) {
      optionsMap.put(storageOptionKey, Consts.STORAGE_INTERNAL);
      storagePath = context.getFilesDir();
    } else {
      String externalDir = context.getExternalFilesDir(null).getAbsolutePath();
      if (!externalDir.endsWith(ICHING_SDCARD_FILES_PATH)) {
        externalDir += ICHING_SDCARD_FILES_PATH;
      }
      optionsMap.put(storageOptionKey, Consts.STORAGE_SDCARD);
      storagePath = new File(externalDir);
    }
  }

  /**
   * @return True if media is writable, false otherwise
   */
  private static boolean isSDWritable() {
    String state = Environment.getExternalStorageState();
    return Environment.MEDIA_MOUNTED.equals(state);
  }


}
