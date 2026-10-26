# Student Records – Aplikasi Android (Java + XML + SQLite)

Aplikasi pengelolaan data mahasiswa untuk tugas **Mobile Application Development – Case 2**
(Student Name, Student ID, Enrolled or not). Data disimpan secara lokal di SQLite.

> Bagian kode di dokumen ini disalin otomatis dari file proyek yang sebenarnya,
> jadi isinya sama persis dengan kode yang dijalankan.

---

## 1. Desain aplikasi dan struktur tabel SQLite

### Desain
Aplikasi hanya punya **satu layar** (`MainActivity`):

- Form sesuai gambar tugas: input **Enter Student Name**, input **Enter Student ID**, dan checkbox **Has Enrolled**.
- Satu baris tombol: **ADD**, **EDIT**, **DELETE**, **VIEW**.
- Di bawahnya ada daftar (`ListView`) berisi data yang tersimpan.
- Di posisi landscape (HP dimiringkan), form ada di kiri dan daftar ada di kanan.

Ada tiga class Java:

| Class | Tugas |
|---|---|
| `StudentDbHelper` (turunan `SQLiteOpenHelper`) | Membuat database dan berisi operasi tambah, baca, ubah, hapus |
| `Student` | Objek sederhana untuk satu data mahasiswa |
| `MainActivity` | Menghubungkan tampilan (form, tombol, daftar) dengan database |

### Tabel `students` (database `students.db`)

| Kolom | Tipe | Keterangan |
|---|---|---|
| `_id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | Kunci internal. EDIT dan DELETE memakai kolom ini, sehingga Student ID tetap bisa diubah |
| `name` | `TEXT NOT NULL` | Student Name (wajib diisi) |
| `student_id` | `TEXT NOT NULL UNIQUE` | Student ID. Disimpan sebagai **TEXT** agar angka nol di depan tetap ada (`00123` tidak berubah menjadi `123`). `UNIQUE` mencegah ID ganda di tingkat database |
| `enrolled` | `INTEGER NOT NULL DEFAULT 0 CHECK (enrolled IN (0, 1))` | Checkbox: `1` = dicentang, `0` = tidak dicentang |

SQL yang dijalankan saat database pertama kali dibuat:

```sql
CREATE TABLE students (
    _id        INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT NOT NULL,
    student_id TEXT NOT NULL UNIQUE,
    enrolled   INTEGER NOT NULL DEFAULT 0 CHECK (enrolled IN (0, 1))
);
```

---

## 2. Struktur folder

Hanya file yang relevan yang ditampilkan. File lain adalah bawaan Android Studio dan tidak diubah.

```
MidExamGuwendMandoya26/
├── gradle/
│   └── libs.versions.toml                  (tidak diubah – versi library)
└── app/
    ├── build.gradle.kts                    (tidak diubah – dependency)
    └── src/main/
        ├── AndroidManifest.xml             (tidak diubah)
        ├── java/com/example/midexam_guwendmandoya26/
        │   ├── MainActivity.java           (DIUBAH)
        │   ├── StudentDbHelper.java        (BARU)
        │   └── Student.java                (BARU)
        └── res/
            ├── layout/
            │   └── activity_main.xml       (DIUBAH – tampilan portrait)
            ├── layout-land/
            │   └── activity_main.xml       (BARU – tampilan landscape)
            └── values/
                └── strings.xml             (DIUBAH – semua teks dan pesan)
```

---

## 3. Kode lengkap

### 3.1 `app/src/main/java/com/example/midexam_guwendmandoya26/Student.java` (BARU)
```java
package com.example.midexam_guwendmandoya26;

/**
 * One row of the "students" table.
 */
public class Student {

    private final long rowId;          // internal SQLite primary key (_id)
    private final String name;
    private final String studentId;    // stored as TEXT so leading zeros are kept
    private final boolean enrolled;

    public Student(long rowId, String name, String studentId, boolean enrolled) {
        this.rowId = rowId;
        this.name = name;
        this.studentId = studentId;
        this.enrolled = enrolled;
    }

    public long getRowId() {
        return rowId;
    }

    public String getName() {
        return name;
    }

    public String getStudentId() {
        return studentId;
    }

    public boolean isEnrolled() {
        return enrolled;
    }

    /** Text shown for this student in the ListView. */
    @Override
    public String toString() {
        return name + "\nID: " + studentId + "   |   Enrolled: " + (enrolled ? "Yes" : "No");
    }
}
```

### 3.2 `app/src/main/java/com/example/midexam_guwendmandoya26/StudentDbHelper.java` (BARU)
```java
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
```

### 3.3 `app/src/main/java/com/example/midexam_guwendmandoya26/MainActivity.java` (DIUBAH)
```java
package com.example.midexam_guwendmandoya26;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final long NO_SELECTION = -1;
    private static final String STATE_SELECTED_ROW_ID = "selected_row_id";
    private static final String STATE_LIST_SHOWN = "list_shown";

    private EditText editStudentName;
    private EditText editStudentId;
    private CheckBox checkEnrolled;
    private TextView textListHeader;
    private ListView listStudents;

    private StudentDbHelper dbHelper;
    private final List<Student> students = new ArrayList<>();
    private ArrayAdapter<Student> adapter;

    /** Row id of the student currently selected in the list, or NO_SELECTION. */
    private long selectedRowId = NO_SELECTION;
    /** True once VIEW has been pressed, so the list is kept up to date after changes. */
    private boolean listShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new StudentDbHelper(this);

        editStudentName = findViewById(R.id.editStudentName);
        editStudentId = findViewById(R.id.editStudentId);
        checkEnrolled = findViewById(R.id.checkEnrolled);
        textListHeader = findViewById(R.id.textListHeader);
        listStudents = findViewById(R.id.listStudents);
        Button btnAdd = findViewById(R.id.btnAdd);
        Button btnEdit = findViewById(R.id.btnEdit);
        Button btnDelete = findViewById(R.id.btnDelete);
        Button btnView = findViewById(R.id.btnView);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_activated_1, students);
        listStudents.setAdapter(adapter);
        listStudents.setOnItemClickListener((parent, view, position, id) ->
                selectStudent(students.get(position)));

        btnAdd.setOnClickListener(v -> addStudent());
        btnEdit.setOnClickListener(v -> editStudent());
        btnDelete.setOnClickListener(v -> confirmDeleteStudent());
        btnView.setOnClickListener(v -> viewStudents());

        // After a screen rotation, restore the selection and the list.
        // (The text fields and checkbox restore themselves.)
        if (savedInstanceState != null) {
            selectedRowId = savedInstanceState.getLong(STATE_SELECTED_ROW_ID, NO_SELECTION);
            listShown = savedInstanceState.getBoolean(STATE_LIST_SHOWN, false);
            refreshListIfShown();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SELECTED_ROW_ID, selectedRowId);
        outState.putBoolean(STATE_LIST_SHOWN, listShown);
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }

    // ---------- ADD ----------

    private void addStudent() {
        if (hasMissingInputs()) {
            return;
        }
        String name = editStudentName.getText().toString().trim();
        String studentId = editStudentId.getText().toString().trim();
        boolean enrolled = checkEnrolled.isChecked();

        if (dbHelper.studentIdExists(studentId, NO_SELECTION)) {
            showDuplicateIdError(studentId);
            return;
        }

        long newRowId = dbHelper.addStudent(name, studentId, enrolled);
        if (newRowId == -1) {
            showMessage(getString(R.string.error_save_failed));
            return;
        }
        showMessage(getString(R.string.msg_added, name));
        clearForm();
        refreshListIfShown();
    }

    // ---------- VIEW ----------

    private void viewStudents() {
        listShown = true;
        loadStudents();
    }

    private void loadStudents() {
        students.clear();
        students.addAll(dbHelper.getAllStudents());
        adapter.notifyDataSetChanged();
        listStudents.clearChoices();

        // Keep the highlight on the selected student if it is still in the list.
        for (int i = 0; i < students.size(); i++) {
            if (students.get(i).getRowId() == selectedRowId) {
                listStudents.setItemChecked(i, true);
                break;
            }
        }

        if (students.isEmpty()) {
            textListHeader.setText(R.string.list_header_empty);
        } else {
            textListHeader.setText(getString(R.string.list_header_count, students.size()));
        }
    }

    private void refreshListIfShown() {
        if (listShown) {
            loadStudents();
        }
    }

    /** Called when a record in the list is tapped: copies it into the form. */
    private void selectStudent(Student student) {
        selectedRowId = student.getRowId();
        editStudentName.setText(student.getName());
        editStudentId.setText(student.getStudentId());
        checkEnrolled.setChecked(student.isEnrolled());
        editStudentName.setError(null);
        editStudentId.setError(null);
        showMessage(getString(R.string.msg_selected, student.getName()));
    }

    // ---------- EDIT ----------

    private void editStudent() {
        if (selectedRowId == NO_SELECTION) {
            showMessage(getString(R.string.error_select_first));
            return;
        }
        if (hasMissingInputs()) {
            return;
        }
        String name = editStudentName.getText().toString().trim();
        String studentId = editStudentId.getText().toString().trim();
        boolean enrolled = checkEnrolled.isChecked();

        // The ID may stay the same, but must not collide with a different student.
        if (dbHelper.studentIdExists(studentId, selectedRowId)) {
            showDuplicateIdError(studentId);
            return;
        }

        int rowsUpdated = dbHelper.updateStudent(selectedRowId, name, studentId, enrolled);
        if (rowsUpdated == 0) {
            showMessage(getString(R.string.error_record_missing));
        } else {
            showMessage(getString(R.string.msg_updated, name));
        }
        clearForm();
        refreshListIfShown();
    }

    // ---------- DELETE ----------

    private void confirmDeleteStudent() {
        if (selectedRowId == NO_SELECTION) {
            showMessage(getString(R.string.error_select_first));
            return;
        }
        Student selected = findSelectedStudent();
        String name = selected != null ? selected.getName() : editStudentName.getText().toString().trim();
        String studentId = selected != null ? selected.getStudentId() : editStudentId.getText().toString().trim();

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_message, name, studentId))
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete_confirm, (dialog, which) -> deleteStudent(name))
                .show();
    }

    private void deleteStudent(String name) {
        int rowsDeleted = dbHelper.deleteStudent(selectedRowId);
        if (rowsDeleted == 0) {
            showMessage(getString(R.string.error_record_missing));
        } else {
            showMessage(getString(R.string.msg_deleted, name));
        }
        clearForm();
        refreshListIfShown();
    }

    // ---------- Helpers ----------

    /** Shows the duplicate Student ID error on the field and as a message. */
    private void showDuplicateIdError(String studentId) {
        String message = getString(R.string.error_student_id_taken, studentId);
        editStudentId.setError(message);
        showMessage(message);
    }

    /**
     * Checks the required fields. Returns true (and shows errors) if any is empty,
     * false if the form can be saved.
     */
    private boolean hasMissingInputs() {
        boolean nameEmpty = TextUtils.isEmpty(editStudentName.getText().toString().trim());
        boolean idEmpty = TextUtils.isEmpty(editStudentId.getText().toString().trim());
        editStudentName.setError(null);
        editStudentId.setError(null);

        if (idEmpty) {
            editStudentId.setError(getString(R.string.error_id_required));
            editStudentId.requestFocus();
        }
        if (nameEmpty) {
            editStudentName.setError(getString(R.string.error_name_required));
            editStudentName.requestFocus();
        }

        if (nameEmpty && idEmpty) {
            showMessage(getString(R.string.error_name_and_id_required));
        } else if (nameEmpty) {
            showMessage(getString(R.string.error_name_required));
        } else if (idEmpty) {
            showMessage(getString(R.string.error_id_required));
        }
        return nameEmpty || idEmpty;
    }

    private Student findSelectedStudent() {
        for (Student student : students) {
            if (student.getRowId() == selectedRowId) {
                return student;
            }
        }
        return null;
    }

    private void clearForm() {
        editStudentName.setText("");
        editStudentId.setText("");
        checkEnrolled.setChecked(false);
        editStudentName.setError(null);
        editStudentId.setError(null);
        selectedRowId = NO_SELECTION;
        listStudents.clearChoices();
        adapter.notifyDataSetChanged();
        editStudentName.requestFocus();
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
```

### 3.4 `app/src/main/res/layout/activity_main.xml` (DIUBAH – portrait)
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    tools:context=".MainActivity">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginBottom="8dp"
            android:text="@string/title_student_records"
            android:textAppearance="?attr/textAppearanceTitleLarge" />

        <EditText
            android:id="@+id/editStudentName"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:autofillHints="name"
            android:hint="@string/hint_student_name"
            android:inputType="textPersonName|textCapWords"
            android:maxLines="1"
            android:minHeight="48dp" />

        <EditText
            android:id="@+id/editStudentId"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:hint="@string/hint_student_id"
            android:importantForAutofill="no"
            android:inputType="text"
            android:maxLines="1"
            android:minHeight="48dp" />

        <CheckBox
            android:id="@+id/checkEnrolled"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:text="@string/label_has_enrolled" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:orientation="horizontal">

            <Button
                android:id="@+id/btnAdd"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginEnd="4dp"
                android:layout_weight="1"
                android:paddingStart="0dp"
                android:paddingEnd="0dp"
                android:text="@string/btn_add" />

            <Button
                android:id="@+id/btnEdit"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginStart="4dp"
                android:layout_marginEnd="4dp"
                android:layout_weight="1"
                android:paddingStart="0dp"
                android:paddingEnd="0dp"
                android:text="@string/btn_edit" />

            <Button
                android:id="@+id/btnDelete"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginStart="4dp"
                android:layout_marginEnd="4dp"
                android:layout_weight="1"
                android:paddingStart="0dp"
                android:paddingEnd="0dp"
                android:text="@string/btn_delete" />

            <Button
                android:id="@+id/btnView"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginStart="4dp"
                android:layout_weight="1"
                android:paddingStart="0dp"
                android:paddingEnd="0dp"
                android:text="@string/btn_view" />
        </LinearLayout>

        <TextView
            android:id="@+id/textListHeader"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:text="@string/list_hint_press_view"
            android:textAppearance="?attr/textAppearanceBodyMedium" />

        <ListView
            android:id="@+id/listStudents"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_marginTop="8dp"
            android:layout_weight="1"
            android:choiceMode="singleChoice" />
    </LinearLayout>

</LinearLayout>
```

### 3.5 `app/src/main/res/layout-land/activity_main.xml` (BARU – landscape)
```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Landscape version: form on the left, list on the right.
     Uses the same view IDs as layout/activity_main.xml, so MainActivity works unchanged. -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="horizontal"
    tools:context=".MainActivity">

    <ScrollView
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginBottom="8dp"
                android:text="@string/title_student_records"
                android:textAppearance="?attr/textAppearanceTitleLarge" />

            <EditText
                android:id="@+id/editStudentName"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:autofillHints="name"
                android:hint="@string/hint_student_name"
                android:inputType="textPersonName|textCapWords"
                android:maxLines="1"
                android:minHeight="48dp" />

            <EditText
                android:id="@+id/editStudentId"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:hint="@string/hint_student_id"
                android:importantForAutofill="no"
                android:inputType="text"
                android:maxLines="1"
                android:minHeight="48dp" />

            <CheckBox
                android:id="@+id/checkEnrolled"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:text="@string/label_has_enrolled" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:orientation="horizontal">

                <Button
                    android:id="@+id/btnAdd"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_marginEnd="4dp"
                    android:layout_weight="1"
                    android:paddingStart="0dp"
                    android:paddingEnd="0dp"
                    android:text="@string/btn_add" />

                <Button
                    android:id="@+id/btnEdit"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="4dp"
                    android:layout_marginEnd="4dp"
                    android:layout_weight="1"
                    android:paddingStart="0dp"
                    android:paddingEnd="0dp"
                    android:text="@string/btn_edit" />

                <Button
                    android:id="@+id/btnDelete"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="4dp"
                    android:layout_marginEnd="4dp"
                    android:layout_weight="1"
                    android:paddingStart="0dp"
                    android:paddingEnd="0dp"
                    android:text="@string/btn_delete" />

                <Button
                    android:id="@+id/btnView"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="4dp"
                    android:layout_weight="1"
                    android:paddingStart="0dp"
                    android:paddingEnd="0dp"
                    android:text="@string/btn_view" />
            </LinearLayout>
        </LinearLayout>
    </ScrollView>

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:id="@+id/textListHeader"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/list_hint_press_view"
            android:textAppearance="?attr/textAppearanceBodyMedium" />

        <ListView
            android:id="@+id/listStudents"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_marginTop="8dp"
            android:layout_weight="1"
            android:choiceMode="singleChoice" />
    </LinearLayout>

</LinearLayout>
```

### 3.6 `app/src/main/res/values/strings.xml` (DIUBAH)
```xml
<resources>
    <string name="app_name">MidExam-GuwendMandoya26</string>

    <string name="title_student_records">Student Records</string>
    <string name="hint_student_name">Enter Student Name</string>
    <string name="hint_student_id">Enter Student ID</string>
    <string name="label_has_enrolled">Has Enrolled</string>

    <string name="btn_add">ADD</string>
    <string name="btn_edit">EDIT</string>
    <string name="btn_delete">DELETE</string>
    <string name="btn_view">VIEW</string>

    <string name="list_hint_press_view">Press VIEW to show saved records.</string>
    <string name="list_header_count">Saved records: %1$d (tap one to select)</string>
    <string name="list_header_empty">No records saved yet.</string>

    <string name="error_name_required">Student Name is required</string>
    <string name="error_id_required">Student ID is required</string>
    <string name="error_name_and_id_required">Student Name and Student ID are required</string>
    <string name="error_student_id_taken">This Student ID is already used: %1$s</string>
    <string name="error_select_first">Select a student from the list first (press VIEW, then tap a record)</string>
    <string name="error_save_failed">Could not save the record</string>
    <string name="error_record_missing">The selected record no longer exists</string>

    <string name="msg_added">Student “%1$s” added</string>
    <string name="msg_updated">Student “%1$s” updated</string>
    <string name="msg_deleted">Student “%1$s” deleted</string>
    <string name="msg_selected">Selected “%1$s”. You can now EDIT or DELETE.</string>

    <string name="dialog_delete_title">Delete student?</string>
    <string name="dialog_delete_message">Delete %1$s (ID: %2$s)? This cannot be undone.</string>
    <string name="dialog_delete_confirm">Delete</string>
    <string name="dialog_cancel">Cancel</string>
</resources>
```

### 3.7 Konfigurasi dan dependency (tidak perlu diubah)
SQLite sudah tersedia di Android, jadi **tidak ada library tambahan**. Dialog konfirmasi DELETE
memakai `MaterialAlertDialogBuilder` dari library `material`, yang sudah ada sejak proyek dibuat.
File di bawah ini ditampilkan agar lengkap.

`app/build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.midexam_guwendmandoya26"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.midexam_guwendmandoya26"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
```

`gradle/libs.versions.toml`
```toml
[versions]
agp = "9.3.3"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
appcompat = "1.8.0"
material = "1.14.0"
activityKtx = "1.13.0"
constraintlayout = "2.2.2"

[libraries]
junit = { group = "junit", name = "junit", version.ref = "junit" }
ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
activity-ktx = { group = "androidx.activity", name = "activity-ktx", version.ref = "activityKtx" }
constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }

```

`app/src/main/AndroidManifest.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.MidExamGuwendMandoya26">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

---

## 4. Cara membuat proyek, menaruh kode, dan menjalankan

### 4.1 Membuat proyek (jika mulai dari nol)
1. Buka Android Studio → **New Project** → pilih **Empty Views Activity** → **Next**.
2. Isi:
   - **Name:** `MidExam-GuwendMandoya26`
   - **Package name:** `com.example.midexam_guwendmandoya26`
   - **Language:** `Java`
   - **Minimum SDK:** `API 24`
   - **Build configuration language:** `Kotlin DSL`
3. Klik **Finish** dan tunggu Gradle selesai sync.

### 4.2 Menaruh kode
1. **Java:** di panel Project, klik kanan folder `java/com.example.midexam_guwendmandoya26` → **New → Java Class**.
   Buat `Student` dan `StudentDbHelper`, lalu tempel kode dari bagian 3.1 dan 3.2.
   Ganti seluruh isi `MainActivity.java` dengan kode bagian 3.3.
2. **Layout portrait:** ganti seluruh isi `res/layout/activity_main.xml` dengan kode bagian 3.4.
3. **Layout landscape:** klik kanan folder `res` → **New → Android Resource Directory** →
   Directory name `layout-land` → **OK**. Klik kanan folder `layout-land` →
   **New → Layout Resource File** → File name `activity_main` → **OK**, lalu ganti isinya dengan kode bagian 3.5.
4. **Teks:** ganti seluruh isi `res/values/strings.xml` dengan kode bagian 3.6.
5. Klik **File → Sync Project with Gradle Files**.

### 4.3 Menjalankan
**Di emulator**
1. Buka **Device Manager** (ikon HP di sisi kanan) → jalankan emulator, misalnya **Medium Phone**.
2. Pilih emulator itu di daftar perangkat di toolbar → klik **Run ▶** (Shift+F10).

**Di HP fisik**
1. Di HP: **Settings → About phone** → ketuk **Build number** 7 kali untuk mengaktifkan Developer options.
2. **Settings → System → Developer options** → aktifkan **USB debugging**.
3. Sambungkan HP dengan kabel USB → pilih **Allow** pada pertanyaan "Allow USB debugging?".
4. Pilih HP di daftar perangkat di Android Studio → klik **Run ▶**.

**Melihat isi database saat demo (opsional):** saat aplikasi berjalan, buka
**View → Tool Windows → App Inspection → Database Inspector** → pilih `students.db` → tabel `students`.

---

## 5. Alur ADD, VIEW, EDIT, DELETE dan hubungan UI dengan SQLite

### ADD
1. Pengguna mengisi nama, ID, dan (opsional) mencentang **Has Enrolled**, lalu menekan **ADD**.
2. `hasMissingInputs()` memeriksa nama dan ID (spasi di awal/akhir dibuang dengan `trim()`).
   Jika ada yang kosong, field tersebut diberi tanda error merah dan muncul Toast. Proses berhenti.
3. `dbHelper.studentIdExists(id, -1)` menjalankan `SELECT` untuk mengecek apakah ID sudah dipakai.
   Jika sudah, muncul pesan *"This Student ID is already used: …"*.
4. `dbHelper.addStudent(...)` menjalankan `INSERT`. Checkbox disimpan sebagai `1` atau `0`.
5. Muncul Toast *"Student "…" added"*, form dikosongkan, dan daftar diperbarui (jika sudah pernah menekan VIEW).

### VIEW
1. Pengguna menekan **VIEW**.
2. `dbHelper.getAllStudents()` menjalankan `SELECT * FROM students ORDER BY name`.
   Setiap baris diubah menjadi objek `Student`; nilai `enrolled` `1/0` diubah menjadi `true/false`.
3. Daftar menampilkan setiap mahasiswa: nama, lalu `ID: … | Enrolled: Yes/No`.
   Judul daftar menampilkan jumlah data, atau *"No records saved yet."* jika kosong.
4. Setelah VIEW ditekan sekali, daftar otomatis diperbarui setiap kali ada ADD, EDIT, atau DELETE.

### Memilih data
Mengetuk satu baris di daftar akan mengisi form dengan nama, ID, dan status checkbox data tersebut,
lalu menyimpan `_id` baris itu di variabel `selectedRowId`. Baris yang dipilih diberi warna sorotan.

### EDIT
1. Pilih data di daftar, ubah form, lalu tekan **EDIT**.
2. Jika belum ada data yang dipilih → pesan *"Select a student from the list first…"*.
3. Field wajib diperiksa seperti pada ADD.
4. `studentIdExists(id, selectedRowId)` mengecek ID ganda **tanpa** menghitung data yang sedang diedit,
   jadi ID boleh tetap sama tetapi tidak boleh sama dengan mahasiswa lain.
5. `dbHelper.updateStudent(...)` menjalankan `UPDATE students SET ... WHERE _id = ?`.
6. Muncul Toast *"Student "…" updated"*, form dikosongkan, daftar diperbarui.

### DELETE
1. Pilih data di daftar, lalu tekan **DELETE**.
2. Jika belum ada data yang dipilih → pesan *"Select a student from the list first…"*.
3. Muncul dialog *"Delete student? Delete <nama> (ID: <id>)? This cannot be undone."*
   - **Cancel:** tidak terjadi apa-apa.
   - **Delete:** `dbHelper.deleteStudent(...)` menjalankan `DELETE FROM students WHERE _id = ?`,
     lalu muncul Toast *"Student "…" deleted"*, form dikosongkan, daftar diperbarui.

### Data tetap tersimpan
SQLite menyimpan data di file `students.db` di penyimpanan internal aplikasi. File ini tetap ada
setelah aplikasi ditutup atau HP dinyalakan ulang. Data baru hilang jika aplikasi di-uninstall
atau data aplikasi dihapus dari Settings.

### Saat layar diputar
Android membuat ulang layar saat HP diputar. Isi form dan checkbox dipulihkan otomatis oleh Android.
Data yang sedang dipilih dan status daftar disimpan di `onSaveInstanceState()` lalu dipulihkan di
`onCreate()`, jadi EDIT dan DELETE tetap bisa dipakai setelah layar diputar.

---

## 6. Skenario pengujian

Kolom **Status**:
- ✅ **Sudah diuji** – sudah dijalankan di emulator *Medium Phone* (Android 17, API 37) dan hasilnya sesuai.
- ⬜ **Perlu Anda uji** – belum diuji, silakan coba sendiri.

| No | Skenario | Langkah | Hasil yang diharapkan | Status |
|---|---|---|---|---|
| 1 | Input kosong | Kosongkan kedua field → ADD | Kedua field diberi tanda error, Toast *"Student Name and Student ID are required"*, tidak ada data tersimpan | ✅ |
| 2 | Hanya nama kosong | Isi ID saja → ADD | Hanya field nama yang error, Toast *"Student Name is required"* | ✅ |
| 3 | Hanya ID kosong | Isi nama saja → ADD | Hanya field ID yang error, Toast *"Student ID is required"* | ✅ |
| 4 | ADD (dicentang) | `Budi Santoso`, `00123`, centang → ADD | Toast *"Student "Budi Santoso" added"*, form kosong | ✅ |
| 5 | ADD (tidak dicentang) | `Siti Aminah`, `00456` → ADD | Data tersimpan | ✅ |
| 6 | VIEW | Tekan VIEW | *"Saved records: 2"*, Budi *Enrolled: Yes*, Siti *Enrolled: No*, ID tetap `00123` dan `00456` | ✅ |
| 7 | ID ganda saat ADD | Nama lain, ID `00123` → ADD | Pesan *"This Student ID is already used: 00123"*, jumlah data tetap | ✅ |
| 8 | Pilih data | Ketuk Siti di daftar | Form terisi, checkbox **tidak** dicentang | ✅ |
| 9 | Ubah checkbox lewat EDIT | Siti dipilih → centang → EDIT | Toast *"… updated"*, daftar menampilkan Siti *Enrolled: Yes* | ✅ |
| 10 | Checkbox dipulihkan | Ketuk Budi | Checkbox **dicentang** | ✅ |
| 11 | ID ganda saat EDIT | Pilih Siti, ubah ID ke `00123` → EDIT | Pesan ID sudah dipakai, data tidak berubah | ✅ |
| 12 | EDIT tanpa memilih | Form kosong → EDIT | Pesan *"Select a student from the list first…"* | ✅ |
| 13 | DELETE tanpa memilih | Tidak ada yang dipilih → DELETE | Pesan *"Select a student from the list first…"* | ✅ |
| 14 | DELETE dibatalkan | Pilih data → DELETE → Cancel | Data tetap ada | ✅ |
| 15 | DELETE dikonfirmasi | Pilih data → DELETE → Delete | Toast *"… deleted"*, jumlah data berkurang | ✅ |
| 16 | Tipe data Student ID | Lihat database (Database Inspector) | `student_id` bertipe TEXT, nilai `00123` | ✅ |
| 17 | Persisten: tutup paksa | Force stop aplikasi → buka lagi → VIEW | Data masih ada | ✅ |
| 18 | Persisten: restart perangkat | Restart emulator/HP → buka aplikasi → VIEW | Data dan status checkbox masih ada | ✅ |
| 19 | Putar layar | Pilih data → putar ke landscape → centang → EDIT | Form, pilihan, dan daftar tetap ada; EDIT berhasil | ✅ |
| 20 | HP fisik | Jalankan semua skenario di HP sungguhan | Sama seperti di emulator | ⬜ |
| 21 | Dark mode dan Android 7 (API 24) | Aktifkan dark mode / pakai emulator API 24 | Tampilan terbaca dan semua fungsi berjalan | ⬜ |

Catatan: ID dibandingkan persis apa adanya, jadi `S001` dan `s001` dianggap ID yang berbeda.
Spasi di awal dan akhir dibuang, jadi `" 00123"` dianggap sama dengan `"00123"`.
