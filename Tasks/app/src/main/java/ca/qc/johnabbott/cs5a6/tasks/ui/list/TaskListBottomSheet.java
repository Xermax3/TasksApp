package ca.qc.johnabbott.cs5a6.tasks.ui.list;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import ca.qc.johnabbott.cs5a6.tasks.databinding.TaskListBottomSheetBinding;
import ca.qc.johnabbott.cs5a6.tasks.model.Priority;
import ca.qc.johnabbott.cs5a6.tasks.model.Task;
import ca.qc.johnabbott.cs5a6.tasks.ui.TasksActivity;
import ca.qc.johnabbott.cs5a6.tasks.viewmodel.TaskViewModel;

public class TaskListBottomSheet extends BottomSheetDialog {

    private TaskListBottomSheetBinding binding;
    private final Task task;
    private final TasksActivity tasksActivity;
    private final TaskViewModel taskViewModel;

    public TaskListBottomSheet(@NonNull Context context, Task task, TasksActivity tasksActivity, TaskViewModel taskViewModel) {
        super(context);
        this.task = task;
        this.tasksActivity = tasksActivity;
        this.taskViewModel = taskViewModel;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = TaskListBottomSheetBinding.inflate(getLayoutInflater());

        // Change the task's priority.
        binding.prioritizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (task.getPriority() != Priority.HIGH) {
                    try {
                        task.setPriority(Priority.HIGH);
                        task.setUrgency(task.calculateUrgency(taskViewModel.getOldestDueDate(), taskViewModel.getNow()));
                        taskViewModel.relocateTask(task);
                        taskViewModel.notifyChange();
                    } catch(Exception ex) {
                        tasksActivity.makeSnackBar("An error occurred. Prioritization was not applied.");
                    }
                }
                dismiss();
            }
        });

        // Remove the task.
        binding.removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    taskViewModel.removeTask(task);
                    taskViewModel.notifyChange();
                } catch(Exception ex) {
                    tasksActivity.makeSnackBar("An error occurred. Deletion was not applied.");
                }
                dismiss();
            }
        });

        setContentView(binding.getRoot());
    }
}
