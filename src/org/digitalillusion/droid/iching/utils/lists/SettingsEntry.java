package org.digitalillusion.droid.iching.utils.lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * An entry of the settings list. An option is a group of settings in which
 * one is currently selected as the option value.
 *
 * @param <T> The type of the value of the selectable options
 * @author digitalillusion
 */
public class SettingsEntry<T extends Serializable> {

  public static final String VIBRATION = "settings_vibration";
  public static final String DIVINATION_METHOD = "settings_divination_method";
  public static final String CHLINES_EVALUATOR = "settings_chlines_evaluator";
  public static final String LANGUAGE = "settings_lang";
  public static final String DICTIONARY = "settings_dictionary";
  public static final String STORAGE = "settings_storage";
  public static final String CONNECTION_MODE = "settings_connection_mode";

  private String optionName;
  private T optionValue;
  private ArrayList<T> optionValues = new ArrayList<T>();

  /**
   * @return Add a setting to this option
   */
  public void addOptionValue(T value) {
    optionValues.add(value);
  }

  /**
   * @return The cardinal position of the current setting in the group
   */
  public Integer getOptionIndex() {
    return optionValues.indexOf(optionValue);
  }

  /**
   * @return The name of this group of settings
   */
  public String getOptionName() {
    return optionName;
  }

  /**
   * Set this option a name
   *
   * @param optionName The name of this group of settings
   */
  public void setOptionName(String optionName) {
    this.optionName = optionName;
  }

  /**
   * @return The current setting for this option
   */
  public T getOptionValue() {
    return optionValue;
  }

  /**
   * Set this option value. The value must be within the settings of this option
   *
   * @param value The new value for this option
   * @throws IllegalArgumentException if the value is not within the possible settings
   */
  public void setOptionValue(T value) throws IllegalArgumentException {
    if (optionValues.contains(value)) {
      this.optionValue = value;
    } else {
      throw new IllegalArgumentException(value + " is not a valid options for " + optionName);
    }
  }

  /**
   * @return All the settings values
   */
  public List<T> getOptionValues() {
    return optionValues;
  }
}
