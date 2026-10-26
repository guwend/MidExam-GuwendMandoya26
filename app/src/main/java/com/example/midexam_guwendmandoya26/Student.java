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
