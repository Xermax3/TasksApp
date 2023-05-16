package ca.qc.johnabbott.cs5a6.tasks.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import ca.qc.johnabbott.cs5a6.tasks.model.sqlite.DatabaseException;
import ca.qc.johnabbott.cs5a6.tasks.model.sqlite.Table;

public class TaskDatabaseHandler extends SQLiteOpenHelper {


    public static final String DATABASE_FILE_NAME = "tasks.db";
    public static final int DATABASE_VERSION = 1;

    private Table<Task> taskTable;

    public TaskDatabaseHandler(@Nullable Context context) {
        super(context, DATABASE_FILE_NAME, null, DATABASE_VERSION);
        taskTable = new TaskTable(this);
    }

    public Table<Task> getTaskTable() {
        return taskTable;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        try {
            taskTable.createTable(database);
        } catch (DatabaseException e) {
            taskTable = null;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int i, int i1) {

    }
}
