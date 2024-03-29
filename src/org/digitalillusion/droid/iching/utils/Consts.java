package org.digitalillusion.droid.iching.utils;


/**
 * Commonly used constants
 *
 * @author digitalillusion
 */
public class Consts {
  /**
   * Shared pref flag for VERSION_CODE *
   */
  public static final String SHARED_PREF_VERSION_CODE = "VERSION_CODE";
  /**
   * The number of hexagrams in the I Ching *
   */
  public static final int HEX_COUNT = 64;
  /**
   * The number of lines in an hexagram *
   */
  public static final int HEX_LINES_COUNT = 6;
  /**
   * Yin coin value *
   */
  public static final int ICHING_COIN_YIN = 2;
  /**
   * Yang coin value *
   */
  public static final int ICHING_COIN_YANG = 3;
  /**
   * Old Yin coins value *
   */
  public static final int ICHING_OLD_YIN = 6;
  /**
   * Young Yang coins value *
   */
  public static final int ICHING_YOUNG_YANG = 7;
  /**
   * Young Yin coins value *
   */
  public static final int ICHING_YOUNG_YIN = 8;
  /**
   * Old Yang coins value *
   */
  public static final int ICHING_OLD_YANG = 9;

  /**
   * Haptic feedback settings *
   */
  public static final int HAPTIC_FEEDBACK_OFF = 0;
  public static final int HAPTIC_FEEDBACK_ON_THROW_COINS = 1;

  /**
   * Divination method automatic toss coins *
   */
  public static final int DIVINATION_METHOD_COINS_AUTO = 0;

  /**
   * Divination method manual toss coins *
   */
  public static final int DIVINATION_METHOD_COINS_MANUAL = 1;

  /**
   * Divination method manual shake coins *
   */
  public static final int DIVINATION_METHOD_COINS_SHAKE = 2;

  /**
   * Manual changing lines evaluator
   */
  public static final int EVALUATOR_MANUAL = 0;
  /**
   * Master Yin changing lines evaluator
   */
  public static final int EVALUATOR_MASTERYIN = 1;
  /**
   * Najing changing lines evaluator
   */
  public static final int EVALUATOR_NAJING = 2;
  /**
   * Only Changing lines evaluator
   */
  public static final int EVALUATOR_CHANGING = 3;

  /**
   * Language codes *
   */
  public static final String LANGUAGE_EN = "en";
  public static final String LANGUAGE_ES = "es";
  public static final String LANGUAGE_FR = "fr";
  public static final String LANGUAGE_IT = "it";
  public static final String LANGUAGE_PT = "pt";

  /**
   * Dictionary settings *
   */
  public static final String DICTIONARY_ALTERVISTA = "altervista";
  public static final String DICTIONARY_CUSTOM = "custom";

  /**
   * Storage settings *
   */
  public static final String STORAGE_INTERNAL = "internal";
  public static final String STORAGE_SDCARD = "sdcard";

  /**
   * Connection mode settings *
   */
  public static final String CONNECTION_MODE_ONLINE = "online";
  public static final String CONNECTION_MODE_OFFLINE = "offline";

  /**
   * Share settings *
   */
  public static final String SHARE_PAGE = "page";
  public static final String SHARE_HEXAGRAM = "hexagram";
  public static final String SHARE_READING = "reading";

  /**
   * Screen orientation settings *
   */
  public static final String SCREEN_ORIENTATION_ROTATE = "rotate";
  public static final String SCREEN_ORIENTATION_LANDSCAPE = "landscape";
  public static final String SCREEN_ORIENTATION_PORTRAIT = "portrait";

  /**
   * Theme settings *
   */
  public static final String THEME_SYSTEM = "system";
  public static final String THEME_LIGHT = "light";
  public static final String THEME_DARK = "dark";
  public static final String THEME_HOLO = "holo";

  /**
   * Backup and restore settings *
   */
  public static final String BACKUP_AND_RESTORE_CREATE_BACKUP = "create_backup";
  public static final String BACKUP_AND_RESTORE_RESTORE_BACKUP = "restore_backup";
}
