package info.ivicel.steam.slowdowng;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sedny on 26/08/2017.
 */

public class DatabaseUtils {
    public static Server insertServer(SQLiteDatabase db, Server server) {
        ContentValues cv = new ContentValues();
        cv.put("name", server.getName());
        cv.put("address", server.getAddress());
        if (db.insert("servers", null, cv) != -1) {
            Cursor cursor = db.query("servers", null, null, null, null, null, "id");
            if (cursor != null && cursor.moveToLast()) {
                server.setId(cursor.getString(cursor.getColumnIndex("id")));
                try {
                    cursor.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
            return server;
        }
        return null;
    }
    
    public static void removeServer(SQLiteDatabase db, Server server) {
        db.delete("servers", "id = ?", new String[] {server.getId()});
    }
    
    public static void updateServer(SQLiteDatabase db, Server server) {
        ContentValues cv = new ContentValues();
        cv.put("name", server.getName());
        cv.put("address", server.getAddress());
        db.update("servers", cv, "id = ?", new String[]{server.getId()});
    }
    
    public static List<Server> queryAllServer(SQLiteDatabase db) {
        List<Server> servers = new ArrayList<>();
        Cursor cursor = db.query("servers", null, null, null, null, null, "id");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex("id"));
                String name = cursor.getString(cursor.getColumnIndex("name"));
                String address = cursor.getString(cursor.getColumnIndex("address"));
                Server server = new Server();
                server.setId(String.valueOf(id));
                server.setName(name);
                server.setAddress(address);
                servers.add(server);
            } while (cursor.moveToNext());
            try {
                cursor.close();
            } catch (Exception e) {
                // do nothing
            }
        }
        return servers;
    }
}
