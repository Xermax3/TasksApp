package ca.qc.johnabbott.cs5a6.tasks.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ca.qc.johnabbott.cs5a6.tasks.model.sqlite.Column;
import ca.qc.johnabbott.cs5a6.tasks.model.sqlite.DatabaseException;
import ca.qc.johnabbott.cs5a6.tasks.model.sqlite.Table;

public class TaskTable extends Table<Task> {

    public static final String TABLE_NAME = "task";
    public static final String COLUMN_UUID = "uuid";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_PRIORITY = "priority";
//    public static final String COLUMN_PROJECT = "project";
//    public static final String COLUMN_ANNOTATIONS= "annotations";
//    public static final String COLUMN_TAGS = "tags";
    public static final String COLUMN_ENTRY = "entry";
    public static final String COLUMN_MODIFIED = "modified";
    public static final String COLUMN_DUE = "due";
//    public static final String COLUMN_START = "start";
//    public static final String COLUMN_END = "end";
//    public static final String COLUMN_WAIT = "wait";
//    public static final String COLUMN_SCHEDULED = "scheduled";
//    public static final String COLUMN_RECUR = "recur";
//    public static final String COLUMN_MASK = "mask";
//    public static final String COLUMN_IMASK = "imask";
//    public static final String COLUMN_UNTIL = "until";
//    public static final String COLUMN_PARENT = "parent";
//    public static final String COLUMN_DEPENDS = "depends";
    public static final String COLUMN_URGENCY = "urgency";


    /**
     * Create a database table
     *
     * @param dbh  the handler that connects to the sqlite database.
     */
    public TaskTable(SQLiteOpenHelper dbh) {
        super(dbh, TABLE_NAME);
        addColumn(new Column(COLUMN_UUID, Column.Type.BLOB));
        addColumn(new Column(COLUMN_DESCRIPTION, Column.Type.TEXT));
        addColumn(new Column(COLUMN_STATUS, Column.Type.TEXT));
        addColumn(new Column(COLUMN_PRIORITY, Column.Type.TEXT));
        addColumn(new Column(COLUMN_ENTRY, Column.Type.TEXT));
        addColumn(new Column(COLUMN_MODIFIED, Column.Type.TEXT));
        addColumn(new Column(COLUMN_DUE, Column.Type.TEXT));
        addColumn(new Column(COLUMN_URGENCY, Column.Type.REAL));
    }

    @Override
    protected ContentValues toContentValues(Task element) throws DatabaseException {
        ContentValues values = new ContentValues();
        values.put(COLUMN_UUID, TypeConvertUtils.uuidAsBytes(element.getUuid()));
        values.put(COLUMN_DESCRIPTION, element.getDescription());
        values.put(COLUMN_STATUS, element.getStatus().toString());
        values.put(COLUMN_PRIORITY, element.getPriority().toString());
        values.put(COLUMN_ENTRY, TypeConvertUtils.dateAsString(element.getEntry()));
        values.put(COLUMN_MODIFIED, TypeConvertUtils.dateAsString(element.getModified()));
        values.put(COLUMN_DUE, TypeConvertUtils.dateAsString(element.getDue()));
        values.put(COLUMN_URGENCY, element.getUrgency());
        return values;
    }

    @Override
    protected Task fromCursor(Cursor cursor) throws DatabaseException {
        Task task = new Task()
                .setId(cursor.getLong(0))
                .setUuid(TypeConvertUtils.bytesAsUuid(cursor.getBlob(1)))
                .setDescription(cursor.getString(2))
                .setStatus(Status.valueOf(cursor.getString(3)))
                .setPriority(Priority.valueOf(cursor.getString(4)))
                .setEntry(TypeConvertUtils.stringAsDate(cursor.getString(5)))
                .setModified(TypeConvertUtils.stringAsDate(cursor.getString(6)))
                .setDue(TypeConvertUtils.stringAsDate(cursor.getString(7)))
                .setUrgency(cursor.getDouble(8));

        return task;
    }

    @Override
    public boolean hasInitialData() {
        return true;
    }

    @Override
    public void initialize(SQLiteDatabase database) throws DatabaseException {
        for (Task element : TaskData.getData()) {

            // Id of inserted element, -1 if error(?).
            long insertId = -1;

            // insert into DB
            try {
                ContentValues values = toContentValues(element);
                insertId = database.insertOrThrow(super.getName(), null, values);
            }
            catch (SQLException e) {
                //database.close();
                throw new DatabaseException(e);
            }

            element.setId(insertId);
        }
    }
}
