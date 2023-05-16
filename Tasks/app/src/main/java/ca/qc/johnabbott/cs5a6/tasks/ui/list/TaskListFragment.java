package ca.qc.johnabbott.cs5a6.tasks.ui.list;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import ca.qc.johnabbott.cs5a6.tasks.databinding.FragmentTaskListBinding;
import ca.qc.johnabbott.cs5a6.tasks.model.sqlite.DatabaseException;
import ca.qc.johnabbott.cs5a6.tasks.ui.TasksActivity;
import ca.qc.johnabbott.cs5a6.tasks.viewmodel.ObservableModel;
import ca.qc.johnabbott.cs5a6.tasks.viewmodel.TaskViewModel;

/**
 * A fragment representing a list of Items.
 */
public class TaskListFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private TasksActivity tasksActivity;
    private TaskViewModel taskViewModel;
    private TaskListRecyclerViewAdapter adapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TaskListFragment() {
    }

    @SuppressWarnings("unused")
    public static TaskListFragment newInstance(int columnCount) {
        TaskListFragment fragment = new TaskListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tasksActivity = (TasksActivity)getActivity();
        taskViewModel = tasksActivity.getTaskViewModel();

        try {
            taskViewModel.setDbHandler(tasksActivity);
            tasksActivity.addTestNotificationTask(TasksActivity.TEST_NOTIFICATION_WAIT);
        } catch (DatabaseException e) {
            tasksActivity.makeSnackBar("An error occurred. Could not load the database.");
        }

        adapter = new TaskListRecyclerViewAdapter(tasksActivity);

        tasksActivity.getTaskViewModel().addOnUpdateListener(this, new ObservableModel.OnUpdateListener<TaskViewModel>() {
            @Override
            public void onUpdate(TaskViewModel item) {
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        FragmentTaskListBinding binding = FragmentTaskListBinding.inflate(inflater, container, false);
        Context context = getContext();

        binding.tasksRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        binding.tasksRecyclerView.setAdapter(adapter);

        // Filter tasks by their description.
        binding.filterEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (binding.filterEditText.getText().length() > 0) {
                    taskViewModel.filterTasks(s.toString());
                } else {
                    taskViewModel.resetFilter();
                }
                taskViewModel.notifyChange();
            }
        });

        // Change the number of columns.
        binding.columnSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    binding.tasksRecyclerView.setLayoutManager(new GridLayoutManager(context, 2));
                } else {
                    binding.tasksRecyclerView.setLayoutManager(new LinearLayoutManager(context));
                }
            }
        });

        return binding.getRoot();
    }
}