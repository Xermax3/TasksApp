package ca.qc.johnabbott.cs5a6.tasks.ui.list;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import ca.qc.johnabbott.cs5a6.tasks.R;
import ca.qc.johnabbott.cs5a6.tasks.model.Status;
import ca.qc.johnabbott.cs5a6.tasks.model.Task;
import ca.qc.johnabbott.cs5a6.tasks.databinding.ListItemTaskBinding;
import ca.qc.johnabbott.cs5a6.tasks.ui.TasksActivity;
import ca.qc.johnabbott.cs5a6.tasks.viewmodel.TaskViewModel;

import java.text.SimpleDateFormat;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Task}.
 */
public class TaskListRecyclerViewAdapter extends RecyclerView.Adapter<TaskListRecyclerViewAdapter.ViewHolder> {

    private final TasksActivity tasksActivity;
    private final TaskViewModel taskViewModel;

    public TaskListRecyclerViewAdapter(TasksActivity tasksActivity) {
        this.tasksActivity = tasksActivity;
        this.taskViewModel = tasksActivity.getTaskViewModel();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(ListItemTaskBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.bind(taskViewModel.getTask(position));
    }

    @Override
    public int getItemCount() {
        return taskViewModel.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final ListItemTaskBinding binding;

        public ViewHolder(ListItemTaskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Task task) {

            // Display task description and due date.
            binding.descriptionTextView.setText(task.getDescription());
            if (task.getDue() == null) {
                binding.dateTextView.setText("");
            } else {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm");
                binding.dateTextView.setText(formatter.format(task.getDue()));

                // Make the date text red if the task is overdue (and still pending).
                if ((task.getStatus() == Status.PENDING) && (task.getDue().before(taskViewModel.getNow()))) {
                    binding.dateTextView.setTextColor(Color.rgb(255, 0, 0));
                } else {
                    binding.dateTextView.setTextColor(Color.rgb(0, 0, 0));
                }
            }

            // Set checkbox and background according to priority and status.
            if (task.getStatus() == Status.COMPLETED) {
                binding.completedCheckBox.setChecked(true);
                binding.itemConstraintLayout.getBackground().setTint(Color.rgb(100, 100,90));
            } else {
                binding.completedCheckBox.setChecked(false);
                switch (task.getPriority()) {
                    case HIGH:
                        binding.itemConstraintLayout.getBackground().setTint(Color.rgb(255, 0,100));
                        break;
                    case MEDIUM:
                        binding.itemConstraintLayout.getBackground().setTint(Color.rgb(255, 150,0));
                        break;
                    case LOW:
                        binding.itemConstraintLayout.getBackground().setTint(Color.rgb(255, 255,0));
                        break;
                    case NONE:
                        binding.itemConstraintLayout.getBackground().setTint(Color.rgb(240, 240,230));
                        break;
                }
            }

            // Make bottom sheet appear when clicking a task.
            binding.itemConstraintLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TaskListBottomSheet taskListBottomSheet = new TaskListBottomSheet(view.getContext(), task, tasksActivity, taskViewModel);
                    taskListBottomSheet.show();
                }
            });

            // Navigate to TaskEditFragment to edit the task.
            binding.itemConstraintLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    taskViewModel.setTaskToEdit(task);
                    ((FloatingActionButton)tasksActivity.findViewById(R.id.floatingActionButton)).hide();
                    NavController controller = Navigation.findNavController(tasksActivity, R.id.nav_host_fragment_content_main);
                    controller.navigate(R.id.action_taskListFragment_to_taskEditFragment);
                    return false;
                }
            });

            // Change task status when clicking the completedCheckbox.
            binding.completedCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        Status origStatus = task.getStatus();
                        task.setStatus(binding.completedCheckBox.isChecked() ? Status.COMPLETED : Status.PENDING);
                        task.setUrgency(task.calculateUrgency(taskViewModel.getOldestDueDate(), taskViewModel.getNow()));
                        taskViewModel.relocateTaskNewStatus(task, origStatus);
                        taskViewModel.notifyChange();
                    } catch(Exception ex) {
                        tasksActivity.makeSnackBar("An error occurred. Status change was not applied.");
                    }
                }
            });
        }
    }

}