/*
 *  TableProvider - Use Table as Data Source for ContentProvider
 *  Copyright (C) 2016  Tommy Alex
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published
 *  by the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.iptux.tableprovider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.List;

/**
 * <p>{@link ContentProvider} that use URI like this:
 * <ul>
 *     <li><code>content://AUTHORITY/table1/</code>: is used to query data in <code>table1</code></li>
 *     <li><code>content://AUTHORITY/table2/number</code>: is used to query a row with primary key <code>number</code> in <code>table2</code></li>
 *     <li><code>content://AUTHORITY/table3/column/value</code>: is used to query data with <code>column</code>'s value <code>value</code> in <code>table3</code></li>
 * </ul></p>
 *
 * <p>sample code:</p>
 * <pre>class SampleProvider extends TableProvider {
 *     SampleProvider() {
 *         super();
 *
 *         // add sonme SingleTableProvider
 *         addSingleTableProvider(new XXTableProvider());
 *         addSingleTableProvider(new XXTableProvider());
 *         addSingleTableProvider(new XXTableProvider());
 *     }
 *
 *     public boolean onCreate() {
 *         // must initial mDatabaseHelper
 * 		   mDatabaseHelper = new TableProviderDatabaseHelper(getContext(), "database", null, 1, this);
 * 		   return true;
 *     }
 * }</pre>
 */
public abstract class TableProvider extends ContentProvider implements TableProviderDatabaseHelper.Callback {
	HashMap<String, SingleTableProvider> mTableMap;
	protected SQLiteOpenHelper mDatabaseHelper = null;

	protected TableProvider() {
		super();
		mTableMap = new HashMap<>();
	}

	/**
	 * add a {@link SingleTableProvider} to the {@link ContentProvider}
	 *
	 * should be called in constructor.
	 * @param tableProvider the {@link SingleTableProvider} to add
	 */
	public final void addSingleTableProvider(@NonNull SingleTableProvider tableProvider) {
		mTableMap.put(tableProvider.getTableName(), tableProvider);
	}

	/**
	 * find the {@link SingleTableProvider} related to the {@link Uri}
	 * @param uri the URI to query.
	 * @return the {@link SingleTableProvider} found
	 * @throws IllegalArgumentException if not found
	 */
	final @NonNull SingleTableProvider findSingleTableProvider(Uri uri) throws IllegalArgumentException {
		String tableName = extractTableName(uri);
		SingleTableProvider table = mTableMap.get(tableName);
		if (null == table) {
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		return table;
	}

	/**
	 * get table name from {@link Uri}
	 * @param uri the URI to query.
	 * @return the table name or {@link null} if not found.
	 */
	final String extractTableName(Uri uri) {
		List<String> paths = uri.getPathSegments();
		if (paths.size() > 0) {
			return paths.get(0);
		}
		return null;
	}

	/**
	 * see {@link TableProviderDatabaseHelper.Callback#onDatabaseCreate(SQLiteDatabase)}
	 */
	public void onDatabaseCreate(SQLiteDatabase db) {
		for (SingleTableProvider table: mTableMap.values()) {
			table.onCreateDatabase(db);
		}
	}

	/**
	 * see {@link TableProviderDatabaseHelper.Callback#onDatabaseUpgrade(SQLiteDatabase, int, int)}
	 */
	public void onDatabaseUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		for (SingleTableProvider table: mTableMap.values()) {
			table.onUpgradeDatabase(db, oldVersion, newVersion);
		}
	}

	/**
	 * see {@link android.content.ContentProvider#getType(Uri)}
	 */
	public String getType(Uri uri) {
		SingleTableProvider table = findSingleTableProvider(uri);
		return table.getType(uri);
	}

	/**
	 * see {@link android.content.ContentProvider#query(Uri, String[], String, String[], String)}
	 */
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SingleTableProvider table = findSingleTableProvider(uri);
		Cursor cursor = table.query(mDatabaseHelper, uri, projection, selection, selectionArgs, sortOrder);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	/**
	 * see {@link android.content.ContentProvider#insert(Uri, ContentValues)}
	 */
	public Uri insert(Uri uri, ContentValues values) {
		SingleTableProvider table = findSingleTableProvider(uri);
		Uri newUri = table.insert(mDatabaseHelper, uri, values);
		getContext().getContentResolver().notifyChange(newUri, null);
		return newUri;
	}

	/**
	 * see {@link android.content.ContentProvider#delete(Uri, String, String[])}
	 */
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SingleTableProvider table = findSingleTableProvider(uri);
		int count = table.delete(mDatabaseHelper, uri, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	/**
	 * see {@link android.content.ContentProvider#update(Uri, ContentValues, String, String[])}
	 */
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SingleTableProvider table = findSingleTableProvider(uri);
		int count = table.update(mDatabaseHelper, uri, values, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
}
