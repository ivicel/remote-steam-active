package info.ivicel.steam.slowdowng;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by sedny on 26/08/2017.
 */

public class ServerDatabase extends SQLiteOpenHelper {
    
    public ServerDatabase(Context context, String name, SQLiteDatabase.CursorFactory factory,
            int version) {
        super(context, name, factory, version);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql =
                "CREATE TABLE servers(" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT," +
                "address TEXT)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // do nothing
    }
}
