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

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * make sure the {@link SQLiteDatabase} created for {@link TableProvider} is as expected.
 */
public class TableProviderDatabaseHelper extends SQLiteOpenHelper {
	interface Callback {
		/**
		 * Called when {@link SQLiteOpenHelper#onCreate} is called.
		 * @param db The database.
		 */
		void onDatabaseCreate(SQLiteDatabase db);

		/**
		 * Called when {@link SQLiteOpenHelper#onUpgrade} is called.
		 * @param db The database.
		 * @param oldVersion The old database version.
		 * @param newVersion The new database version.
		 */
		void onDatabaseUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
	}

	Callback mCallback;

	public TableProviderDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, Callback callback) {
		this(context, name, factory, version, null, callback);
	}

	public TableProviderDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler, Callback callback) {
		super(context, name, factory, version, errorHandler);
		mCallback = callback;
	}

	public void onCreate(SQLiteDatabase db) {
		if (null != mCallback) {
			mCallback.onDatabaseCreate(db);
		}
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (null != mCallback) {
			mCallback.onDatabaseUpgrade(db, oldVersion, newVersion);
		}
	}
}
