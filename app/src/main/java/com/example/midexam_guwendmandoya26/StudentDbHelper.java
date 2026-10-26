package com.example.midexam_guwendmandoya26;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates the SQLite database and provides the ADD / VIEW / EDIT / DELETE operations.
 */
public class StudentDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "students.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_STUDENTS = "students";
    public static final String COL_ROW_ID = "_id";
    public static final String COL_NAME = "name";
    public static final String COL_STUDENT_ID = "student_id";
    public static final String COL_ENROLLED = "enrolled";

    private static final String SQL_CREATE_STUDENTS =
            "CREATE TABLE " + TABLE_STUDENTS + " ("
                    + COL_ROW_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_NAME + " TEXT NOT NULL, "
                    + COL_STUDENT_ID + " TEXT NOT NULL UNIQUE, "
                    + COL_ENROLLED + " INTEGER NOT NULL DEFAULT 0 CHECK (" + COL_ENROLLED + " IN (0, 1))"
                    + ")";

    public StudentDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_STUDENTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Only version 1 exists. A future version would migrate data here.
    }

    /**
     * Returns true if another student already uses this Student ID.
     *
     * @param excludeRowId row to ignore (the student being edited), or -1 to check all rows
     */
    public boolean studentIdExists(String studentId, long excludeRowId) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_STUDENTS,
                new String[]{COL_ROW_ID},
                COL_STUDENT_ID + " = ? AND " + COL_ROW_ID + " != ?",
                new String[]{studentId, String.valueOf(excludeRowId)},
                null, null, null)) {
            return cursor.moveToFirst();
        }
    }

    /** ADD: inserts a student. Returns the new row id, or -1 if the insert failed. */
    public long addStudent(String name, String studentId, boolean enrolled) {
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);
        values.put(COL_STUDENT_ID, studentId);
        values.put(COL_ENROLLED, enrolled ? 1 : 0);
        return getWritableDatabase().insert(TABLE_STUDENTS, null, values);
    }

    /** VIEW: returns all students ordered by name. */
    public List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_STUDENTS, null, null, null, null, null,
                COL_NAME + " COLLATE NOCASE ASC")) {
            int rowIdIndex = cursor.getColumnIndexOrThrow(COL_ROW_ID);
            int nameIndex = cursor.getColumnIndexOrThrow(COL_NAME);
            int studentIdIndex = cursor.getColumnIndexOrThrow(COL_STUDENT_ID);
            int enrolledIndex = cursor.getColumnIndexOrThrow(COL_ENROLLED);
            while (cursor.moveToNext()) {
                students.add(new Student(
                        cursor.getLong(rowIdIndex),
                        cursor.getString(nameIndex),
                        cursor.getString(studentIdIndex),
                        cursor.getInt(enrolledIndex) == 1));
            }
        }
        return students;
    }

    /** EDIT: updates the student with the given row id. Returns the number of rows changed. */
    public int updateStudent(long rowId, String name, String studentId, boolean enrolled) {
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);
        values.put(COL_STUDENT_ID, studentId);
        values.put(COL_ENROLLED, enrolled ? 1 : 0);
        return getWritableDatabase().update(TABLE_STUDENTS, values,
                COL_ROW_ID + " = ?", new String[]{String.valueOf(rowId)});
    }

    /** DELETE: removes the student with the given row id. Returns the number of rows deleted. */
    public int deleteStudent(long rowId) {
        return getWritableDatabase().delete(TABLE_STUDENTS,
                COL_ROW_ID + " = ?", new String[]{String.valueOf(rowId)});
    }
}
