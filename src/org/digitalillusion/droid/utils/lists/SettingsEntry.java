package org.digitalillusion.droid.utils.lists;

import java.util.ArrayList;
import java.util.List;

import org.digitalillusion.droid.iching.R;
import org.digitalillusion.droid.utils.Utils;

/**
 * An entry of the settings list
 * 
 * @author digitalillusion
 *
 * @param <T> The type of the value of the selectable options
 */
public class SettingsEntry <T> {
	private String optionName;
	private T optionValue;
	private ArrayList<T> optionValues = new ArrayList<T>();
	
	public void setOptionName(String optionName) {
		this.optionName = optionName;
	}
	
	public String getOptionName() {
		return optionName;
	}
	
	public T getOptionValue() {
		return optionValue;
	}
	
	public List<T> getOptionValues() {
		return optionValues;
	}
	
	public void addOptionValue(T value) {
		optionValues.add(value);
	}
	
	public void setOptionValue(T value) {
		if (optionValues.contains(value)) {
			this.optionValue = value;
		} else {
			throw new IllegalArgumentException(value + " is not a valid options for " + optionName);
		}
	}
}
