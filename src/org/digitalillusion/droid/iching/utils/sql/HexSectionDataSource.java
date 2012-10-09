package org.digitalillusion.droid.iching.utils.sql;

import org.digitalillusion.droid.iching.utils.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * DAO to access an hexagram section definition
 * 
 * @author digitalillusion
 */
public class HexSectionDataSource {

	// Database fields
	private SQLiteDatabase database;
	private MySQLiteHelper dbHelper;
	private String[] allColumns = { 
		MySQLiteHelper.COLUMN_HEX,
		MySQLiteHelper.COLUMN_DICTIONARY,
		MySQLiteHelper.COLUMN_LANG,
		MySQLiteHelper.COLUMN_SECTION,
		MySQLiteHelper.COLUMN_DEF 
	};

	public HexSectionDataSource(Context context) {
		dbHelper = new MySQLiteHelper(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	/**
	 * Allows to insert or update the definition of an hexagram section
	 * 
	 * @param hex The hexagram parameter
	 * @param dictionary The dictionary parameter
	 * @param lang The language parameter
	 * @param section The section parameter
	 * @param def The new definition
	 */
	public void updateHexSection(String hex, String dictionary, String lang, String section, String def) {
		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.COLUMN_HEX, hex);
		values.put(MySQLiteHelper.COLUMN_DICTIONARY, dictionary);
		values.put(MySQLiteHelper.COLUMN_LANG, lang);
		values.put(MySQLiteHelper.COLUMN_SECTION, section);
		values.put(MySQLiteHelper.COLUMN_DEF, def);

		try {
			String whereClause = MySQLiteHelper.COLUMN_HEX + "=? and " +
				MySQLiteHelper.COLUMN_DICTIONARY + "=? and " +
				MySQLiteHelper.COLUMN_LANG + "=? and " +
				MySQLiteHelper.COLUMN_SECTION + "=?";
			String[] whereArgs = new String[] { hex, dictionary, lang, section };

			getHexSection(hex, dictionary, lang, section);
			database.update(
					MySQLiteHelper.TABLE_DEFINITIONS, 
					values, 
					whereClause, 
					whereArgs
			);
		} catch (NotFoundException e) {
			database.insert(
					MySQLiteHelper.TABLE_DEFINITIONS, 
					null,
					values
			);
		}
	}
	
	/**
	 * Allows to delete all the definitions of an hexagram
	 * 
	 * @param hex The hexagram parameter
	 * @param lang The language parameter
	 */
	public void deleteHexSections(String hex, String lang) {
		database.delete(
			MySQLiteHelper.TABLE_DEFINITIONS,
			MySQLiteHelper.COLUMN_HEX + "=? and " +
			MySQLiteHelper.COLUMN_LANG + "=?", 
			new String[] { hex, lang } 
		);
	}
	
	/**
	 * Allows to delete a section definition for an hexagram
	 * 
	 * @param hex The hexagram parameter
	 * @param section The section parameter
	 * @param lang The language parameter
	 */
	public void deleteHexSection(String hex, String section, String lang) {
		database.delete(
			MySQLiteHelper.TABLE_DEFINITIONS,
			MySQLiteHelper.COLUMN_HEX + "=? and " +
			MySQLiteHelper.COLUMN_SECTION + "=? and " +
			MySQLiteHelper.COLUMN_LANG + "=?", 
			new String[] { hex, section, lang } 
		);
	}

	/**
	 * @param hex The hexagram parameter
	 * @param dictionary The dictionary parameter
	 * @param lang The language parameter
	 * @param section The section parameter
	 * @return The result of the search corresponding to the given parameters, or null 
	 * if no match is found
	 */
	public HexSection getHexSection(String hex, String dictionary, String lang, String section) throws NotFoundException {
		String querySelect = MySQLiteHelper.COLUMN_HEX + "=? and " +
			MySQLiteHelper.COLUMN_DICTIONARY + "=? and " +
			MySQLiteHelper.COLUMN_LANG + "=? and " +
			MySQLiteHelper.COLUMN_SECTION + "=?";
		String[] queryParams = new String[] { hex, dictionary, lang, section };  

		Cursor cursor = database.query(
				MySQLiteHelper.TABLE_DEFINITIONS,
				allColumns, 
				querySelect,
				queryParams,
				null, null, null
		);

		cursor.moveToFirst();
		try {
			if (!cursor.isAfterLast()) {
				HexSection hs = new HexSection(
					cursor.getString(0),
					cursor.getString(1),
					cursor.getString(2),
					cursor.getString(3),
					cursor.getString(4)
				);
				cursor.moveToNext();

				return hs;
			} else {
				throw new NotFoundException(
						"HexSectionDataSource.getHexSection() returned no results for query: " +
						querySelect + " using parameters (" +
						Utils.implode(queryParams, ",") + ")"
				);
			}
		} finally {
			// Make sure to close the cursor
			cursor.close();
		}
	}
} 