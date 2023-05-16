package ca.qc.johnabbott.cs5a6.tasks.ui.editor;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Stack;

import ca.qc.johnabbott.cs5a6.tasks.R;
import ca.qc.johnabbott.cs5a6.tasks.databinding.FragmentTaskEditBinding;
import ca.qc.johnabbott.cs5a6.tasks.model.Priority;
import ca.qc.johnabbott.cs5a6.tasks.model.Task;
import ca.qc.johnabbott.cs5a6.tasks.ui.TasksActivity;
import ca.qc.johnabbott.cs5a6.tasks.ui.util.DatePickerDialogFragment;
import ca.qc.johnabbott.cs5a6.tasks.ui.util.TimePickerDialogFragment;
import ca.qc.johnabbott.cs5a6.tasks.viewmodel.TaskViewModel;

public class TaskEditFragment extends Fragment {

    private FragmentTaskEditBinding binding;
    private TasksActivity tasksActivity;
    private TaskViewModel taskViewModel;

    private final Stack<Task> taskHistory = new Stack<>();
    private final Stack<ChangeType> changeHistory = new Stack<>();

    private Date selectedDate;
    private Priority selectedPriority;
    private boolean undoingText = false;

    private enum ChangeType {
        SET_DESCRIPTION,
        SET_PRIORITY,
        SET_DUE_DATE,
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTaskEditBinding.inflate(inflater, container, false);
        tasksActivity = (TasksActivity)getActivity();
        taskViewModel = tasksActivity.getTaskViewModel();
        tasksActivity.setTaskEditFragment(this);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set an initial task to put at the bottom of the task stack
        Task initialTask = taskViewModel.getTaskToEdit();

        if (initialTask == null) {
            initialTask = new Task();
            initialTask.setEntry(new Date());
            selectedPriority = Priority.NONE;
            binding.priorityRadioGroup.setVisibility(View.GONE);
            binding.dueDateLinearLayout.setVisibility(View.GONE);
        } else {
            binding.priorityRadioGroup.setVisibility(View.VISIBLE);
            binding.dueDateLinearLayout.setVisibility(View.VISIBLE);
            selectedDate = initialTask.getDue();
            selectedPriority = initialTask.getPriority();
            editDescription(initialTask);
            editPriority(initialTask);
            editDueDate(initialTask);
        }

        taskHistory.push(initialTask);

        // Open priority menu (automatically set to HIGH)
        binding.priorityImageButton.setOnClickListener(priorityImageButtonView -> {
            binding.priorityImageButton.setEnabled(false);
            selectedPriority = Priority.HIGH;
            logEdit(ChangeType.SET_PRIORITY);
            binding.priorityRadioGroup.clearCheck();
            binding.priorityHighRadioButton.setChecked(true);
            binding.priorityRadioGroup.setVisibility(View.VISIBLE);
        });

        // Open due date menu
        binding.dueDateImageButton.setOnClickListener(dueDateImageButtonView -> {
            setDueDate();
        });

        // Close priority menu
        binding.priorityCancelImageButton.setOnClickListener(priorityCancelImageButtonView -> {
            selectedPriority = Priority.NONE;
            binding.priorityRadioGroup.clearCheck();
            removePriorityVisual();
            logEdit(ChangeType.SET_PRIORITY);
        });

        // Close due date menu
        binding.dueDateCancelImageButton.setOnClickListener(dueDateCancelImageButtonView -> {
            selectedDate = null;
            removeDueDateVisual();
            logEdit(ChangeType.SET_DUE_DATE);
        });

        // Edit due date
        binding.dueDateButton.setOnClickListener(dueDateButtonView -> {
            setDueDate();
        });

        // Undo action
        binding.undoImageButton.setOnClickListener(undoImageButtonView -> {

            // Only proceed if there are tasks recorded besides the initial one
            if (taskHistory.size() > 1) {
                ChangeType changeType = changeHistory.pop();
                taskHistory.pop();
                Task previousTask = taskHistory.peek();

                // Modify the appropriate value and update visually
                switch (changeType) {
                    case SET_DESCRIPTION:
                        editDescription(previousTask);
                        break;
                    case SET_PRIORITY:
                        editPriority(previousTask);
                        break;
                    case SET_DUE_DATE:
                        editDueDate(previousTask);
                        break;
                }
            } else {
                removePriorityVisual();
                removeDueDateVisual();
            }
        });

        // Log text change
        binding.descriptionEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!undoingText) {
                    logEdit(ChangeType.SET_DESCRIPTION);
                }
            }
        });

        // Log priority change
        binding.priorityHighRadioButton.setOnClickListener(priorityHighRadioButtonView -> {
            if (selectedPriority != Priority.HIGH) {
                selectedPriority = Priority.HIGH;
                logEdit(ChangeType.SET_PRIORITY);
            }
        });
        binding.priorityMediumRadioButton.setOnClickListener(priorityMediumRadioButtonView -> {
            if (selectedPriority != Priority.MEDIUM) {
                selectedPriority = Priority.MEDIUM;
                logEdit(ChangeType.SET_PRIORITY);
            }
        });
        binding.priorityLowRadioButton.setOnClickListener(priorityLowRadioButtonView -> {
            if (selectedPriority != Priority.LOW) {
                selectedPriority = Priority.LOW;
                logEdit(ChangeType.SET_PRIORITY);
            }
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                tasksActivity.moveToPreviousFragment();
            }
        });
    }

    // Open the date picker and time picker dialogs
    private void setDueDate() {

        // If no date was picked previously, set it to the next day at 8:00 AM.
        if (selectedDate == null) {
            Calendar startDateTime = Calendar.getInstance();
            startDateTime.setTime(new Date());
            startDateTime.add(Calendar.DATE, 1);
            startDateTime.set(Calendar.HOUR, 8);
            startDateTime.set(Calendar.MINUTE, 0);
            selectedDate = startDateTime.getTime();
        }

        DatePickerDialogFragment dialogFragment = DatePickerDialogFragment.create(selectedDate, (dateView, year, month, dayOfMonth) -> {

            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);

            // Only proceed if the date selected is in the future
            if (calendar.getTime().before(new Date())) {

                AlertDialog dialog = new AlertDialog.Builder(this.getContext())
                        .setIcon(R.drawable.ic_baseline_warning_24)
                        .setTitle("Invalid due date.")
                        .setMessage("Please select a date in the future.")
                        .setPositiveButton("OK", null)
                        .create();
                dialog.show();

            } else {

                TimePickerDialogFragment timeFragment = TimePickerDialogFragment.create(selectedDate, (timeView, hour, minute) -> {

                    binding.dueDateImageButton.setEnabled(false);

                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                    selectedDate = calendar.getTime();
                    logEdit(ChangeType.SET_DUE_DATE);

                    updateDateButtonText();
                    binding.dueDateLinearLayout.setVisibility(View.VISIBLE);

                });
                timeFragment.show(getParentFragmentManager(), "timePicker");

            }

        });
        dialogFragment.show(getParentFragmentManager(), "datePicker");
    }

    private void updateDateButtonText() {
        SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMMM dd 'at' hh:mm aa");
        binding.dueDateButton.setText(formatter.format(selectedDate));
    }

    private void removePriorityVisual() {
        binding.priorityRadioGroup.setVisibility(View.GONE);
        binding.priorityImageButton.setEnabled(true);
    }

    private void removeDueDateVisual() {
        binding.dueDateLinearLayout.setVisibility(View.GONE);
        binding.dueDateImageButton.setEnabled(true);
    }

    private void editDescription(Task task) {
        undoingText = true;
        binding.descriptionEditText.setText(task.getDescription());
        undoingText = false;
    }

    private void editPriority(Task task) {
        binding.priorityRadioGroup.clearCheck();
        switch (task.getPriority()) {
            case HIGH:
                binding.priorityHighRadioButton.setChecked(true);
                break;
            case MEDIUM:
                binding.priorityMediumRadioButton.setChecked(true);
                break;
            case LOW:
                binding.priorityLowRadioButton.setChecked(true);
                break;
            case NONE:
                removePriorityVisual();
                break;
        }
    }

    private void editDueDate(Task task) {
        if (task.getDue() == null) {
            removeDueDateVisual();
        } else {
            updateDateButtonText();
        }
    }

    private void logEdit(ChangeType changeType) {

        // Get the same values as the previous recorded task
        Task currentTask = taskHistory.peek().copy();
        currentTask.setModified(new Date());

        // Modify the appropriate value
        switch (changeType) {
            case SET_DESCRIPTION:
                currentTask.setDescription(binding.descriptionEditText.getText().toString());
                break;
            case SET_PRIORITY:
                currentTask.setPriority(selectedPriority);
                break;
            case SET_DUE_DATE:
                currentTask.setDue(selectedDate);
                break;
        }

        // Record the modification
        changeHistory.push(changeType);
        taskHistory.push(currentTask);
    }

    public Task getTask() {
        return taskHistory.peek();
    }

    // Returns true if the description is not empty.
    private boolean verifyDescription() {
        boolean isEmpty = binding.descriptionEditText.getText().toString().isEmpty();
        if (isEmpty) {
            AlertDialog dialog = new AlertDialog.Builder(this.getContext())
                    .setIcon(R.drawable.ic_baseline_warning_24)
                    .setTitle("Description is empty")
                    .setMessage("Your task was not saved.")
                    .setPositiveButton("OK", null)
                    .create();
            dialog.show();
        }
        return !isEmpty;
    }

    @Override
    public void onDestroyView() {
        if (verifyDescription()) {
            try {
                if (taskViewModel.getTaskToEdit() == null) {
                    // Create a new task.
                    taskViewModel.addTask(getTask());
                    tasksActivity.makeSnackBarForTaskUpdate("Task created.", true);
                } else {
                    // Edit the task.
                    taskViewModel.editTask(taskViewModel.getTaskToEdit(), getTask());
                    tasksActivity.makeSnackBarForTaskUpdate("Task updated.", false);
                }
                taskViewModel.notifyChange();
            } catch(Exception ex) {
                tasksActivity.makeSnackBar("An error occurred. Task was not saved.");
            }
        }

        tasksActivity.setTaskEditFragment(null);

        super.onDestroyView();
        binding = null;
    }

}