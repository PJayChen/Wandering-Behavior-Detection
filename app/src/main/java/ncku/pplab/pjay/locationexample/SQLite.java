package ncku.pplab.pjay.locationexample;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.widget.Toast;


/**
 * Created by pjay on 2015/1/3.
 */
public class SQLite extends SQLiteOpenHelper{

    private static final String DATABASE_NAME = "position_fix.db";
    private  static final int DATABASE_VERSION = 1;
    private SQLiteDatabase db;
    private Context m_context;

    public SQLite(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = this.getWritableDatabase();
        m_context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String DATABASE_CREATE_TABLE =
                "create table fix ("
                + "_ID INTEGER PRIMARY KEY,"
                + "time TEXT,"
                + "latitude TEXT,"
                + "longitude TEXT"
                + ");";
        db.execSQL(DATABASE_CREATE_TABLE);

        //Toast.makeText(m_context, "Database Created!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS fix");
        onCreate(db);
    }

    //Get all data
    public Cursor getAll(){
     return db.rawQuery("SELECT * FROM fix", null);
    }

    //Get one data
    public Cursor get(long rowId)throws SQLException{
        Cursor cursor = db.query(true,
                "fix",
                new String[]{"_ID", "time", "latitude", "longitude"},
                "_ID=" + rowId,
                null,
                null,
                null,
                null,
                null
                );

        if(cursor != null){
            cursor.moveToFirst();
        }
        return cursor;
    }

    //新增一筆記錄，成功回傳rowID，失敗回傳-1
    public long create(String time, String latitude, String longitude) {
        ContentValues args = new ContentValues();
        args.put("time", time);
        args.put("latitude", latitude);
        args.put("longitude", longitude);

       // Toast.makeText(m_context, "Add" + name + " " + value, Toast.LENGTH_SHORT).show();
        return db.insert("fix", null, args);
    }

//    //刪除記錄，回傳成功刪除筆數
//    public int delete(long rowId) {
//        return db.delete("fix",	//資料表名稱
//                "_ID=" + rowId,			//WHERE
//                null				//WHERE的參數
//        );
//    }

//    //修改記錄，回傳成功修改筆數
//    public int update(long rowId, String value) {
//        ContentValues args = new ContentValues();
//        args.put("value", value);
//
//        return db.update("fix",	//資料表名稱
//                args,				//VALUE
//                "_ID=" + rowId,			//WHERE
//                null				//WHERE的參數
//        );
//    }
}
