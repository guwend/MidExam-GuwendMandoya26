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
