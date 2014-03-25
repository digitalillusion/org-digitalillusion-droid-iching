package org.digitalillusion.droid.iching.utils;


/**
 * Commonly used constants
 *
 * @author digitalillusion
 */
public class Consts {
	/** The number of hexagrams in the I Ching **/
	public static final int HEX_COUNT = 64;
	/** The number of lines in an hexagram **/
	public static final int HEX_LINES_COUNT = 6;
	
	/** Old Yin coin value **/
	public static final int ICHING_OLD_YIN = 6;
	/** Young Yang coin value **/
	public static final int ICHING_YOUNG_YANG = 7;
	/** Young Yin coin value **/
	public static final int ICHING_YOUNG_YIN = 8;
	/** Old Yang coin value **/
	public static final int ICHING_OLD_YANG = 9;
	
	/** Haptic feedback settings **/
	public static final int HAPTIC_FEEDBACK_OFF = 0;
	public static final int HAPTIC_FEEDBACK_ON_THROW_COINS = 1;
	
	/** Manual changing lines evaluator */ 
	public static final int EVALUATOR_MANUAL = 0;
	/** Master Yin changing lines evaluator */
	public static final int EVALUATOR_MASTERYIN = 1;
	/** Najing changing lines evaluator */
	public static final int EVALUATOR_NAJING = 2;
		
	/** Language codes **/
	public static final String LANGUAGE_EN = "en";
	
	/** Dictionary settings **/
	public static final String DICTIONARY_ALTERVISTA = "altervista";
	public static final String DICTIONARY_CUSTOM = "custom";
	
	/** Storage settings **/
	public static final String STORAGE_INTERNAL = "internal";
	public static final String STORAGE_SDCARD = "sdcard";
	
	/** Connection mode settings **/
	public static final String CONNECTION_MODE_ONLINE = "online";
	public static final String CONNECTION_MODE_OFFLINE = "offline";
	
}
