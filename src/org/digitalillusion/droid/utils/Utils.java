package org.digitalillusion.droid.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

public class Utils {

	private static Context context;

	public static final String NEWLINE = System.getProperty("line.separator");

	public static void setContext(Context context) {
		Utils.context = context;
	}

	public static String s(int id) {
		try {
			return context.getResources().getString(id);
		} catch (NotFoundException e) {
			Log.w("Utils.s()", e.getMessage());
			return "";
		}
	}

	public static String s(int id, Object[] subst) {
		return context.getResources().getString(id, subst);
	}

	/**
	 * Finds the resource ID for the current application's resources.
	 * @param Rclass Resource class to find resource in. 
	 * Example: R.string.class, R.layout.class, R.drawable.class
	 * @param name Name of the resource to search for.
	 * @return The id of the resource or -1 if not found.
	 */
	public static int getResourceByName(Class<?> Rclass, String name) {
		int id = -1;
		try {
			if (Rclass != null) {
				final Field field = Rclass.getField(name);
				if (field != null)
					id = field.getInt(null);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return id;
	}

	public static InputStream downloadUrl(String url, String[]... params) throws IOException {
		HttpURLConnection con = null;
		InputStream is = null;

		List<BasicNameValuePair> pairs = new ArrayList<BasicNameValuePair>();
		for (String[] p : params) {
			pairs.add(new BasicNameValuePair(p[0], p[1]));
		}

		String queryString = URLEncodedUtils.format(pairs, "utf-8");
		url += (!url.endsWith("?") ? "?" : "") + queryString;

		con = (HttpURLConnection) new URL(url).openConnection();
		con.setReadTimeout(3000);
		con.setConnectTimeout(10000);
		con.setRequestMethod("GET");
		con.setDoInput(true);

		con.connect();
		is = con.getInputStream();
		return is;
	}

	public static String streamToString(InputStream stream) throws IOException {
		return new String(getBytes(stream), "UTF-8").replace("\\n",  NEWLINE);
	}

	public static byte[] getBytes(InputStream is) throws IOException {

		int len;
		int size = 1024;
		byte[] buf;

		if (is instanceof ByteArrayInputStream) {
			size = is.available();
			buf = new byte[size];
			len = is.read(buf, 0, size);
		} else {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			buf = new byte[size];
			while ((len = is.read(buf, 0, size)) != -1) {
				bos.write(buf, 0, len);
			}
			buf = bos.toByteArray();
		}
		return buf;
	}

}
