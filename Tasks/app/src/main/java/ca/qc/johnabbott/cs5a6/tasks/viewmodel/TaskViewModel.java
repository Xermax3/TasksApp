package ca.qc.johnabbott.cs5a6.tasks.viewmodel;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ca.qc.johnabbott.cs5a6.tasks.model.Status;
import ca.qc.johnabbott.cs5a6.tasks.model.Task;
import ca.qc.johnabbott.cs5a6.tasks.model.TaskDatabaseHandler;
import ca.qc.johnabbott.cs5a6.tasks.model.sqlite.DatabaseException;
import ca.qc.johnabbott.cs5a6.tasks.ui.TasksActivity;

public class TaskViewModel extends ObservableModel<TaskViewModel> {

    private TasksActivity tasksActivity;
    private TaskDatabaseHandler dbHandler;

    private List<Task> allTasks;
    private List<Task> currentTasks;

    // Specific list of tasks separated into categories to make sorting easier.
    private final List<Task> pendingTasks;
    private final List<Task> completedTasks;
    private final List<Task> completedTasksNoDue;

    private String currentFilter = null;

    private Date oldestDue = null;
    private final Date now = new Date();

    private Task taskToEdit;
    private Task lastTaskCreated;
    private Task lastTaskEditedBeforeChanges;
    private Task lastTaskEditedAfterChanges;

    @Override
    protected TaskViewModel get() {
        return this;
    }

    public TaskViewModel() {
        pendingTasks = new ArrayList<>();
        completedTasks = new ArrayList<>();
        completedTasksNoDue = new ArrayList<>();
    }

    public void setDbHandler(Context context) throws DatabaseException {
        tasksActivity = (TasksActivity)context;
        dbHandler = new TaskDatabaseHandler(tasksActivity);
        allTasks = dbHandler.getTaskTable().readAll();
        currentTasks = allTasks;

        // Get the oldest date in the task list.
        for (Task task : allTasks) {
            if (task.getDue() == null || task.getStatus() != Status.COMPLETED)
                continue;
            if (oldestDue == null || task.getDue().before(oldestDue))
                oldestDue = task.getDue();
        }

        // Sort the tasks by inserting them one by one.
        for (Task task : allTasks) {
            addTaskWithoutDB(task);
        }
        updateMainList();
    }

    public List<Task> getTasks() {
        return currentTasks;
    }

    public Task getTask(int pos) {
        return currentTasks.get(pos);
    }

    public int size() {
        return currentTasks.size();
    }

    public Date getOldestDueDate() {
        return oldestDue;
    }

    public Date getNow() {
        return now;
    }

    public Task getTaskToEdit() {
        return taskToEdit;
    }

    public void setTaskToEdit(Task taskToEdit) {
        this.taskToEdit = taskToEdit;
    }

    // Inserts a new task into the list.
    public void addTask(Task task) throws DatabaseException {
        addTaskWithoutDB(task);

        dbHandler.getTaskTable().create(task);
    }

    private void addTaskWithoutDB(Task task) {
        lastTaskCreated = task;
        task.setUrgency(task.calculateUrgency(oldestDue, now));
        insertIntoTaskList(task);

        tasksActivity.createNotificationThread(task);
    }

    // Updates the position of a single task according to the sort that applies to it.
    public void relocateTask(Task task) throws DatabaseException {
        removeTaskByStatus(task, task.getStatus());
        insertIntoTaskList(task);

        dbHandler.getTaskTable().update(task);
    }

    // Same as relocateTask(), but used for tasks that had their status changed (which affects their position).
    public void relocateTaskNewStatus(Task task, Status origStatus) throws DatabaseException {
        removeTaskByStatus(task, origStatus);
        insertIntoTaskList(task);

        dbHandler.getTaskTable().update(task);
    }

    // Behaves like relocateTask(), but replaces the task object with an entirely new one.
    public void editTask(Task oldTask, Task newTask) throws DatabaseException {
        lastTaskEditedBeforeChanges = oldTask;
        lastTaskEditedAfterChanges = newTask;
        removeTask(oldTask);
        addTask(newTask);
        setTaskToEdit(null);

        dbHandler.getTaskTable().update(newTask);
    }

    // Removes a task from every list that contains it.
    public void removeTask(Task task) throws DatabaseException {
        removeTaskByStatus(task, task.getStatus());
        currentTasks.remove(task);
        if (currentFilter != null)
            allTasks.remove(task);

        dbHandler.getTaskTable().delete(task);
    }

    // Filters the current task list according to a provided search text.
    public void filterTasks(String searchText) {
        ArrayList<Task> filteredTasks = new ArrayList<>();
        String searchTextLowerCase = searchText.toLowerCase();

        // Only keep tasks with descriptions containing the search text.
        for (Task task : allTasks) {
            if (task.getDescription().toLowerCase().contains(searchText)) {
                filteredTasks.add(task);
            }
        }

        currentTasks = filteredTasks;
        currentFilter = searchTextLowerCase;
    }

    // Removes the filter. All tasks are displayed.
    public void resetFilter() {
        currentTasks = allTasks;
        currentFilter = null;
    }

    // Removes the last task added to the list.
    public void undoLatestAdd() throws DatabaseException {
        removeTask(lastTaskCreated);
    }

    // Reverts the changes made on the last edited task.
    public void undoLatestEdit() throws DatabaseException {
        editTask(lastTaskEditedAfterChanges, lastTaskEditedBeforeChanges);
    }


    // Rebuilds the task lists while preserving any existing filter.
    // This method should be used after updating a specific list.
    private void updateMainList() {
        allTasks = Stream.of(pendingTasks, completedTasks, completedTasksNoDue)
                .flatMap(Collection::stream).collect(Collectors.toList());
        currentTasks = allTasks;
        if (currentFilter != null) {
            filterTasks(currentFilter);
        }
    }

    // Inserts a task into its respective status list.
    // Note: This method does not update the main list.
    private void insertIntoTaskList(Task task) {
        if (task.getStatus() != Status.COMPLETED) {
            insertInPendingList(task);
        } else if (task.getDue() != null) {
            insertInCompletedList(task);
        } else {
            completedTasksNoDue.add(0, task);
        }
        updateMainList();
    }

    // Binary insertion for pending tasks (highest urgency first).
    // Based on: https://www.geeksforgeeks.org/binary-insertion-sort/
    private void insertInPendingList(Task task) {
        if (pendingTasks.size() == 0) {
            pendingTasks.add(task);
        } else {

            int low = 0;
            int high = pendingTasks.size() - 1;

            while (low <= high) {
                int mid = low + (high - low) / 2;
                if (task == pendingTasks.get(mid)) {
                    pendingTasks.add(mid, task);
                    return;
                } else if (task.getUrgency() < pendingTasks.get(mid).getUrgency())
                    low = mid + 1;
                else
                    high = mid - 1;
            }

            pendingTasks.add(low, task);
        }
    }

    // Binary insertion for completed tasks (most recent due date first).
    // Based on: https://www.geeksforgeeks.org/binary-insertion-sort/
    private void insertInCompletedList(Task task) {
        if (completedTasks.size() == 0) {
            completedTasks.add(task);
        } else {

            int low = 0;
            int high = completedTasks.size() - 1;

            while (low <= high) {
                int mid = low + (high - low) / 2;
                if (task == completedTasks.get(mid)) {
                    completedTasks.add(mid, task);
                    return;
                } else if (task.getDue().before(completedTasks.get(mid).getDue()))
                    low = mid + 1;
                else
                    high = mid - 1;
            }

            completedTasks.add(low, task);
        }
    }

    // Removes a task only from its respective status list.
    private void removeTaskByStatus(Task task, Status status) {
        if (status != Status.COMPLETED) {
            pendingTasks.remove(task);
        } else if (task.getDue() != null) {
            completedTasks.remove(task);
        } else {
            completedTasksNoDue.remove(task);
        }
    }


}
