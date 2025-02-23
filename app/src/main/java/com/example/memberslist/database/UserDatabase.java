package com.example.memberslist.database;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {User.class}, version = 7, exportSchema = false)
public abstract class UserDatabase extends RoomDatabase {
    private static volatile UserDatabase INSTANCE;
    public abstract UserDao userDao();
    static final Migration MIGRATION = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase supportSQLiteDatabase) {
            //Use below line if any columns are added to the table.
            //supportSQLiteDatabase.execSQL("ALTER TABLE user_database ADD COLUMN new_column_name TEXT DEFAULT ''");
        }
    };
    public static UserDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (UserDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            UserDatabase.class,
                            "user_database")
                            .addMigrations(MIGRATION)
                            //The below line should be commented if no data to be deleted
                            //.fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
