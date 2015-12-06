package org.digitalillusion.droid.iching.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import org.digitalillusion.droid.iching.IChingActivity;
import org.digitalillusion.droid.iching.utils.lists.SettingsEntry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


/**
 * Façade for the settings map
 *
 * @author digitalillusion
 */
public class SettingsManager {

  /**
   * Map of possible value for each setting
   */
  public static HashMap<SETTINGS_MAP, Serializable[]> SETTINGS_VALUES_MAP = new HashMap<SETTINGS_MAP, Serializable[]>() {{
    put(SETTINGS_MAP.HAPTIC_FEEDBACK, new Integer[]{
        Consts.HAPTIC_FEEDBACK_OFF,
        Consts.HAPTIC_FEEDBACK_ON_THROW_COINS
    });
    put(SETTINGS_MAP.DIVINATION_METHOD, new Integer[]{
        Consts.DIVINATION_METHOD_COINS_AUTO,
        Consts.DIVINATION_METHOD_COINS_MANUAL
    });
    put(SETTINGS_MAP.CHANGING_LINES_EVALUATOR, new Integer[]{
        Consts.EVALUATOR_MANUAL,
        Consts.EVALUATOR_MASTERYIN,
        Consts.EVALUATOR_NAJING
    });
    put(SETTINGS_MAP.LANGUAGE, new String[]{
        Consts.LANGUAGE_EN,
        Consts.LANGUAGE_FR,
        Consts.LANGUAGE_PT
    });
    put(SETTINGS_MAP.DICTIONARY, new String[]{
        Consts.DICTIONARY_ALTERVISTA,
        Consts.DICTIONARY_CUSTOM
    });
    put(SETTINGS_MAP.STORAGE, new String[]{
        Consts.STORAGE_SDCARD,
        Consts.STORAGE_INTERNAL
    });
    put(SETTINGS_MAP.CONNECTION_MODE, new String[]{
        Consts.CONNECTION_MODE_ONLINE,
        Consts.CONNECTION_MODE_OFFLINE
    });
  }};
  /**
   * Internal storage of the settings *
   */
  private HashMap<String, Serializable> settingsMap = new HashMap<String, Serializable>();
  /**
   * The activity context
   */
  private Context context;

  public SettingsManager(Context context) {
    this.context = context;
  }

  /**
   * Create a new option, aka a group of settings
   *
   * @param <T>          The type of the settings
   * @param optionsList  The list of options to which this option has to be add
   * @param name         A name for the option
   * @param values       The possible settings for this option
   * @param defaultValue The default setting for this option
   */
  @SuppressWarnings("unchecked")
  public <T extends Serializable> void createOption(List<SettingsEntry<?>> optionsList, String name, T[] values, SETTINGS_MAP defaultValue) {
    SettingsEntry<T> newOption = new SettingsEntry<T>();
    for (T val : values) {
      newOption.addOptionValue(val);
    }
    newOption.setOptionName(name);
    newOption.setOptionValue((T) get(defaultValue));
    optionsList.add(newOption);
  }

  /**
   * @see {@link HashMap#get(Object) }
   */
  public Serializable get(SETTINGS_MAP setting) {
    Serializable entry = settingsMap.get(setting.getKey());
    if (entry == null) {
      entry = getDefault(setting);
    }
    return entry;
  }

  /**
   * Return a context-dependent default setting
   *
   * @param setting The setting to retrieve
   * @return The default value of the requested setting
   */
  public Serializable getDefault(SETTINGS_MAP setting) throws InvalidParameterException {
    Serializable defaultValue = null;
    if (setting.equals(SETTINGS_MAP.LANGUAGE)) {
      defaultValue = context.getResources().getConfiguration().locale.getLanguage();
    }

    if (defaultValue != null && Arrays.asList(SETTINGS_VALUES_MAP.get(setting)).contains(defaultValue)) {
      return defaultValue;
    } else {
      return getStaticDefault(setting);
    }
  }

  /**
   * @return Application locale
   */
  public Locale getLocale() {
    String lang = (String) get(SETTINGS_MAP.LANGUAGE);
    return new Locale(lang);
  }

  /**
   * @see {@link DataPersister#loadOptions(android.content.Context, java.util.HashMap)}
   */
  public void load() throws FileNotFoundException, IOException {
    DataPersister.loadOptions(context, settingsMap);
    setLocale(getLocale());
  }

  /**
   * @see {@link HashMap#put(Object, Object)} }
   */
  public Serializable put(SETTINGS_MAP setting, Serializable object) {
    return settingsMap.put(setting.getKey(), object);
  }

  /**
   * Load the default settings
   *
   * @param uninitializedOnly specify if to load default values only for settings
   *                          that has not been yet initialized
   */
  public void resetDefaults(boolean uninitializedOnly) {
    for (SETTINGS_MAP setting : SETTINGS_MAP.values()) {
      if (!uninitializedOnly || !settingsMap.containsKey(setting.getKey())) {
        settingsMap.put(setting.getKey(), getDefault(setting));
      }
    }
  }

  /**
   * @param activity The caller activity, needed to display popups (eventually)
   * @see {@link DataPersister#saveOptions(HashMap, android.app.Activity) }
   */
  public void save(IChingActivity activity) {
    DataPersister.saveOptions(settingsMap, activity);
  }

  /**
   * Set application locale
   *
   * @param locale
   * @return True if locale was updated, false otherwise
   */
  public boolean setLocale(Locale locale) {
    Resources resources = context.getResources();
    Configuration config = resources.getConfiguration();
    if (!config.locale.equals(locale)) {
      Locale.setDefault(locale);
      config.locale = locale;
      resources.updateConfiguration(config, null);
      put(SETTINGS_MAP.LANGUAGE, locale.getLanguage());
      return true;
    }
    return false;
  }

  private Serializable getStaticDefault(SETTINGS_MAP setting) throws InvalidParameterException {
    if (setting.equals(SETTINGS_MAP.HAPTIC_FEEDBACK)) {
      return Consts.HAPTIC_FEEDBACK_ON_THROW_COINS;
    } else if (setting.equals(SETTINGS_MAP.DIVINATION_METHOD)) {
      return Consts.DIVINATION_METHOD_COINS_AUTO;
    } else if (setting.equals(SETTINGS_MAP.CHANGING_LINES_EVALUATOR)) {
      return Consts.EVALUATOR_MASTERYIN;
    } else if (setting.equals(SETTINGS_MAP.LANGUAGE)) {
      return Consts.LANGUAGE_EN;
    } else if (setting.equals(SETTINGS_MAP.DICTIONARY)) {
      return Consts.DICTIONARY_ALTERVISTA;
    } else if (setting.equals(SETTINGS_MAP.STORAGE)) {
      return Consts.STORAGE_SDCARD;
    } else if (setting.equals(SETTINGS_MAP.CONNECTION_MODE)) {
      return Consts.CONNECTION_MODE_ONLINE;
    }
    throw new InvalidParameterException(setting.getKey() + " does not specify an option.");
  }

  /**
   * The enumeration of available settings
   */
  public enum SETTINGS_MAP {
    HAPTIC_FEEDBACK("hapticFeedback"),
    DIVINATION_METHOD("divinationMethod"),
    CHANGING_LINES_EVALUATOR("changingLinesEvaluator"),
    LANGUAGE("language"),
    DICTIONARY("dictionary"),
    STORAGE("storage"),
    CONNECTION_MODE("connectionMode");


    private String key;

    SETTINGS_MAP(String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }
  }
}
