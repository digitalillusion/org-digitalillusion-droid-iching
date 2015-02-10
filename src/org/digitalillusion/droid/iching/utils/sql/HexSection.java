package org.digitalillusion.droid.iching.utils.sql;

import org.digitalillusion.droid.iching.utils.Utils;

/**
 * Model of an hexagram section definition text
 *
 * @author digitalillusion
 */
public class HexSection {
  /**
   * The hexagram index
   */
  private String hex;
  /**
   * The dictionary
   */
  private String dictionary;
  /**
   * The localization code
   */
  private String lang;
  /**
   * The section or line name
   */
  private String section;
  /**
   * The actual definition
   */
  private String def;

  public HexSection() {
    this.hex = Utils.EMPTY_STRING;
    this.dictionary = Utils.EMPTY_STRING;
    this.section = Utils.EMPTY_STRING;
    this.lang = Utils.EMPTY_STRING;
    this.def = Utils.EMPTY_STRING;
  }

  public HexSection(String hex, String dictionary, String lang, String section, String def) {
    this.hex = hex;
    this.dictionary = dictionary;
    this.section = section;
    this.lang = lang;
    this.def = def;
  }

  public String getDef() {
    return def;
  }

  public void setDef(String def) {
    this.def = def;
  }

  /**
   * @return The quote
   */
  public String getDefQuote() {
    if (def == null || def.equals(Utils.EMPTY_STRING) || def.indexOf(Utils.HEX_SECTION_QUOTE_DELIMITER) < 0) {
      return Utils.EMPTY_STRING;
    }
    while (Utils.NEWLINE.charAt(0) == def.charAt(0)) {
      def = def.substring(1);
    }
    return def.substring(0, def.indexOf(Utils.HEX_SECTION_QUOTE_DELIMITER));
  }

  /**
   * @return The reading
   */
  public String getDefReading() {
    if (def == null || def.equals(Utils.EMPTY_STRING)) {
      return Utils.EMPTY_STRING;
    } else if (def.indexOf(Utils.HEX_SECTION_QUOTE_DELIMITER) < 0) {
      return def;
    }
    def = def.substring(def.indexOf(Utils.HEX_SECTION_QUOTE_DELIMITER) + Utils.HEX_SECTION_QUOTE_DELIMITER.length());
    while (Utils.NEWLINE.charAt(0) == def.charAt(0)) {
      def = def.substring(1);
    }
    return def;
  }

  public String getDictionary() {
    return dictionary;
  }

  public void setDictionary(String dictionary) {
    this.dictionary = dictionary;
  }

  public String getHex() {
    return hex;
  }

  public void setHex(String hex) {
    this.hex = hex;
  }

  public String getLang() {
    return lang;
  }

  public void setLang(String lang) {
    this.lang = lang;
  }

  public String getSection() {
    return section;
  }

  public void setSection(String section) {
    this.section = section;
  }
}
