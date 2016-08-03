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

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single table driven {@link android.content.ContentProvider}.
 */
public abstract class SingleTable implements BaseColumns {
	public final String mAuthority;
	public final String mTableName;

	public final Uri mContentUri;

	private final HashMap<String, String> mProjectionMap;
	private final UriMatcher mUriMatcher;

	private final int COLUMN_NONE = 1;
	private final int COLUMN_ID = 2;
	private final int COLUMN_SINGLE = 3;

	protected SingleTable(@NonNull String authority, @NonNull String tableName) {
		mAuthority = authority;
		mTableName = tableName;
		mContentUri = Uri.parse("content://" + authority + "/" + tableName + "/");

		mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		mUriMatcher.addURI(authority, tableName, COLUMN_NONE);
		mUriMatcher.addURI(authority, tableName + "/#", COLUMN_ID);
		mUriMatcher.addURI(authority, tableName + "/*", COLUMN_SINGLE);

		mProjectionMap = new HashMap<>();
		initProjectionMap(mProjectionMap);
	}

	/**
	 * get table name in use.
	 * @return the table name
	 */
	public final @NonNull String getTableName() {
		return mTableName;
	}

	/**
	 * get base content uri of this provider.
	 * @return the content uri
	 */
	public final @NonNull Uri getContentUri() {
		return mContentUri;
	}

	/**
	 * Called when {@link SQLiteOpenHelper#onCreate} is called.
	 * @param db The database.
	 */
	public abstract void onCreateDatabase(SQLiteDatabase db);

	/**
	 * Called when {@link SQLiteOpenHelper#onUpgrade} is called.
	 * @param db The database.
	 * @param oldVersion The old database version.
	 * @param newVersion The new database version.
	 */
	public abstract void onUpgradeDatabase(SQLiteDatabase db, int oldVersion, int newVersion);

	/**
	 * initial projectionMap used in {@link SQLiteQueryBuilder#setProjectionMap(Map)}
	 * @param projectionMap
	 */
	public abstract void initProjectionMap(HashMap<String, String> projectionMap);

	/**
	 * get default sort order used in {@link android.content.ContentProvider#query} if sort order is not specified
	 * @return default sort order
	 */
	public abstract String getDefaultSortOrder();

	/**
	 * check the validity of {@link ContentValues} used in {@link android.content.ContentProvider#insert(Uri, ContentValues)}
	 * @param values the {@link ContentValues} to be inserted.
	 * @return {@link true} if {@link values} is valid, {@link false} if invalid.
	 * @throws IllegalArgumentException {@link throw} if {@link values} is invalid.
	 */
	public abstract boolean checkInsertContentValues(@NonNull ContentValues values) throws IllegalArgumentException;

	/**
	 * see {@link android.content.ContentProvider#getType(Uri)}
	 */
	public String getType(Uri uri) {
		StringBuilder stringBuilder = new StringBuilder("vnd.android.cursor.");
		switch (mUriMatcher.match(uri)) {
			case COLUMN_ID:
				stringBuilder.append("item/vnd.");
				break;
			default:
				stringBuilder.append("dir/vnd.");
				break;
		}
		stringBuilder.append(mAuthority)
			.append(".")
			.append(getTableName());
		return stringBuilder.toString();
	}

	/**
	 * get column name from {@link Uri}
	 * @param uri the URI to query.
	 * @return the column name or {@link null} if not found.
	 */
	final String extractColumnName(Uri uri) {
		List<String> paths = uri.getPathSegments();
		if (paths.size() == 3) {
			return paths.get(1);
		}
		return null;
	}

	/**
	 * see {@link android.content.ContentProvider#query(Uri, String[], String, String[], String)}
	 * @param dbHelper the related database used
	 */
	public Cursor query(SQLiteOpenHelper dbHelper, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(getTableName());
		qb.setProjectionMap(mProjectionMap);

		switch (mUriMatcher.match(uri)) {
			case COLUMN_NONE:
				break;
			case COLUMN_ID:
				qb.appendWhere(_ID + "=?");
				selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[] {uri.getLastPathSegment()});
				break;
			case COLUMN_SINGLE:
				String column = extractColumnName(uri);
				if (null == column) {
					throw new IllegalArgumentException("no column name: " + uri);
				}
				qb.appendWhere(column + "=?");
				selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[] {uri.getLastPathSegment()});
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		if (TextUtils.isEmpty(sortOrder)) {
			sortOrder = getDefaultSortOrder();
		}

		SQLiteDatabase db = dbHelper.getReadableDatabase();
		return qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
	}

	/**
	 * see {@link android.content.ContentProvider#insert(Uri, ContentValues)}
	 * @param dbHelper the related database used
	 */
	public Uri insert(SQLiteOpenHelper dbHelper, Uri uri, ContentValues values) {
		if (COLUMN_NONE != mUriMatcher.match(uri)) {
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		if (null == values) {
			values = new ContentValues();
		}
		if (!checkInsertContentValues(values)) {
			throw new IllegalArgumentException("invalid content values");
		}

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(getTableName(), null, values);

		if (rowId <= 0) {
			throw new SQLException("Failed to insert row into " + uri);
		}

		return ContentUris.withAppendedId(getContentUri(), rowId);
	}

	/**
	 * see {@link android.content.ContentProvider#delete(Uri, String, String[])}
	 * @param dbHelper the related database used
	 */
	public int delete(SQLiteOpenHelper dbHelper, Uri uri, String selection, String[] selectionArgs) {
		switch (mUriMatcher.match(uri)) {
			case COLUMN_NONE:
				break;
			case COLUMN_ID:
				selection = DatabaseUtils.concatenateWhere(_ID + " = " + ContentUris.parseId(uri), selection);
				break;
			case COLUMN_SINGLE:
				String column = extractColumnName(uri);
				if (null == column) {
					throw new IllegalArgumentException("no column name: " + uri);
				}
				selection = DatabaseUtils.concatenateWhere(column + "=?", selection);
				selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[] {uri.getLastPathSegment()});
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		return db.delete(getTableName(), selection, selectionArgs);
	}

	/**
	 * see {@link android.content.ContentProvider#update(Uri, ContentValues, String, String[])}
	 * @param dbHelper the related database used
	 */
	public int update(SQLiteOpenHelper dbHelper, Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		switch (mUriMatcher.match(uri)) {
			case COLUMN_NONE:
				break;
			case COLUMN_ID:
				selection = DatabaseUtils.concatenateWhere(_ID + " = " + ContentUris.parseId(uri), selection);
				break;
			case COLUMN_SINGLE:
				String column = extractColumnName(uri);
				if (null == column) {
					throw new IllegalArgumentException("no column name: " + uri);
				}
				selection = DatabaseUtils.concatenateWhere(column + "=?", selection);
				selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[] {uri.getLastPathSegment()});
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		return db.update(getTableName(), values, selection, selectionArgs);
	}
}
