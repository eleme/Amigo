package me.ele.demo.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

public class StudentProvider extends ContentProvider {

    private static final String TAG = StudentProvider.class.getSimpleName();

    private static final String DB_NAME = "demo.db";
    private static final String DB_STUDENT_TABLE = "tb_student";
    private static final int DB_VERSION = 1;
    private static final HashMap<String, String> articleProjectionMap;
    private static final String DB_CREATE = "create table " + DB_STUDENT_TABLE +
            " (" + Student.ID + " integer primary key, " +
            Student.NAME + " text not null, " +
            Student.GENDER + " integer)";

    static {
        articleProjectionMap = new HashMap<>();
        articleProjectionMap.put(Student.ID, Student.ID);
        articleProjectionMap.put(Student.NAME, Student.NAME);
        articleProjectionMap.put(Student.GENDER, Student.GENDER);
    }

    private DBHelper dbHelper;

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate: ");
        dbHelper = new DBHelper(getContext(), DB_NAME, null, DB_VERSION);

        insertStudent(new Student(0, "stu1", 0));
        insertStudent(new Student(1, "stu2", 0));
        insertStudent(new Student(2, "stu3", 1));
        return true;
    }

    private void insertStudent(Student student) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Student.NAME, student.name);
        values.put(Student.GENDER, student.gender);
        db.insert(DB_STUDENT_TABLE, null, values);
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Log.d(TAG, "query: " + uri);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
        sqlBuilder.setTables(DB_STUDENT_TABLE);
        sqlBuilder.setProjectionMap(articleProjectionMap);
        return sqlBuilder.query(db, projection, selection, selectionArgs, null, null,
                TextUtils.isEmpty(sortOrder) ? Student.DEFAULT_SORT_ORDER : sortOrder, null);
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("do not support insert op yet.");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    public static class Student {
        public static final String ID = "_id";
        public static final String NAME = "_name";
        public static final String GENDER = "_gender";

        public static final String DEFAULT_SORT_ORDER = "_id asc";

        public int id;
        public String name;
        public int gender; // 0=male, 1=female

        public Student(int gender, String name, int id) {
            this.gender = gender;
            this.name = name;
            this.id = id;
        }
    }

    private static class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                        int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DB_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + DB_STUDENT_TABLE);
            onCreate(db);
        }
    }
}
