package edu.buffalo.cse.cse486586.groupmessenger;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {
	/*
	 * Static URI constants
	 */
	static final String PROVIDER_NAME = "edu.buffalo.cse.cse486586.groupmessenger.provider";
	static final String TABLE_NAME = "map";
	static final String DATABASE_NAME = "DB";
	static final String KEY = "key";
	static final String VALUE = "value";

	static long rowID = 0;
	private SQLiteDatabase db;
	static final int DATABASE_VERSION = 1;
	static final String CREATE_DB_TABLE = 
			" CREATE TABLE " + TABLE_NAME +
			" ("+ KEY+" TEXT PRIMARY KEY, " + 
			VALUE + " TEXT NOT NULL);";
	
	public long get_RowID()
	{
		return rowID;
	}
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context){
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL(CREATE_DB_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, 
				int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " +  TABLE_NAME);
			onCreate(db);
		}
	}


	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// You do not need to implement this.
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return null;

	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		
		Uri result;
		long _ID =db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE );
		if(_ID > 0)
		{
			result = ContentUris.withAppendedId(uri, _ID);
			getContext().getContentResolver().notifyChange(uri, null);
			rowID = _ID;
			Log.v("insert", values.toString());
			return result;

		}
		throw new SQLException ("Failed to insert in the following uri: " +  uri.toString());
	}

	@Override
	public boolean onCreate() {
		// If you need to perform any one-time initialization task, please do it here.
		Context context = getContext();
		
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		
		/*
		 * create a SQLite database if it does not exists already
		 */
		db = dbHelper.getWritableDatabase();
		
		/*
		 * Dropping the old table since the test program always inserts the  same key values which violates DB constraints 
		 */
		db.execSQL("DROP TABLE IF EXISTS " +  TABLE_NAME);
		dbHelper.onCreate(db);
		return (db == null)? false:true;
	} 

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		
		Log.e("Inside query Method", "query method");
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		
		qb.setTables(TABLE_NAME);
		
		Log.e("query", selection);
		qb.appendWhere( KEY + " = " + "'" +selection+ "'");
		
		String queryString = qb.buildQuery(projection,null, null, null, sortOrder, null);
		Log.e("query String", queryString);
		Cursor cursor = qb.query(db, projection, null, null, null, null, sortOrder);
		
		
		
		 
		Log.v("query", selection);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// You do not need to implement this.
		return 0;
	}



}
