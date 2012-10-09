package org.digitalillusion.droid.iching.utils.lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * An entry of the settings list. An option is a group of settings in which
 * one is currently selected as the option value.
 * 
 * @author digitalillusion
 *
 * @param <T> The type of the value of the selectable options
 */
public class SettingsEntry <T extends Serializable> {
	private String optionName;
	private T optionValue;
	private ArrayList<T> optionValues = new ArrayList<T>();
	
	/**
	 * Set this option a name
	 * 
	 * @param optionName The name of this group of settings
	 */
	public void setOptionName(String optionName) {
		this.optionName = optionName;
	}
	
	/**
	 * @return The name of this group of settings
	 */
	public String getOptionName() {
		return optionName;
	}
	
	/**
	 * @return The current setting for this option
	 */
	public T getOptionValue() {
		return optionValue;
	}
	
	/**
	 * @return The cardinal position of the current setting in the group
	 */
	public Integer getOptionIndex() {
		return optionValues.indexOf(optionValue);
	}
	
	/**
	 * @return All the settings values
	 */
	public List<T> getOptionValues() {
		return optionValues;
	}
	
	/**
	 * @return Add a setting to this option
	 */
	public void addOptionValue(T value) {
		optionValues.add(value);
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
}
