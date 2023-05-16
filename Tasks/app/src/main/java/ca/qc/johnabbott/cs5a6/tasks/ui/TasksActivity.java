package ca.qc.johnabbott.cs5a6.tasks.ui;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.snackbar.Snackbar;

import java.util.Date;

import ca.qc.johnabbott.cs5a6.tasks.R;
import ca.qc.johnabbott.cs5a6.tasks.databinding.ActivityTasksBinding;
import ca.qc.johnabbott.cs5a6.tasks.model.Priority;
import ca.qc.johnabbott.cs5a6.tasks.model.Status;
import ca.qc.johnabbott.cs5a6.tasks.model.Task;
import ca.qc.johnabbott.cs5a6.tasks.model.sqlite.DatabaseException;
import ca.qc.johnabbott.cs5a6.tasks.ui.editor.TaskEditFragment;
import ca.qc.johnabbott.cs5a6.tasks.viewmodel.TaskViewModel;

public class TasksActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityTasksBinding binding;

    public TaskEditFragment taskEditFragment;
    private final TaskViewModel taskViewModel;

    public static final String TASKS_NOTIFICATION_CHANNEL = "tasks-notification-channel";
    public static final int TEST_NOTIFICATION_WAIT = 30000;

    public TasksActivity() {
        taskViewModel = new TaskViewModel();
    }

    public void setTaskEditFragment(TaskEditFragment taskEditFragment) {
        this.taskEditFragment = taskEditFragment;
    }

    public TaskViewModel getTaskViewModel() {
        return taskViewModel;
    }

    public void makeSnackBarForTaskUpdate(String msg, boolean taskCreated) {
        makeSnackBar(msg)
            .setAction("UNDO", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        if (taskCreated) {
                            taskViewModel.undoLatestAdd();
                        } else {
                            taskViewModel.undoLatestEdit();
                        }
                        taskViewModel.notifyChange();
                    } catch (Exception ex) {
                        makeSnackBar("An error occurred. Undo did not apply.");
                    }
                }
            })
            .show();
    }

    public Snackbar makeSnackBar(String msg) {
        return Snackbar.make(findViewById(R.id.floatingActionButton), msg, Snackbar.LENGTH_LONG);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String name =  "Tasks";
        String description = "Notifications when tasks become overdue.";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(TASKS_NOTIFICATION_CHANNEL, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        binding = ActivityTasksBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        // Navigate to TaskEditFragment to add a new task.
        binding.floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.floatingActionButton.hide();
                NavController controller = Navigation.findNavController(TasksActivity.this, R.id.nav_host_fragment_content_main);
                controller.navigate(R.id.action_taskListFragment_to_taskEditFragment);
            }
        });

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                return !moveToPreviousFragment();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public boolean moveToPreviousFragment() {
        binding.floatingActionButton.show();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return navController.popBackStack();
    }

    public void addTestNotificationTask(int delay) {
        Date now = new Date();
        Date soon = new Date(now.getTime() + delay);
        Task sample = new Task()
                .setDescription("Test task " + soon.toString())
                .setEntry(now)
                .setModified(now)
                .setStatus(Status.PENDING)
                .setDue(soon)
                .setPriority(Priority.HIGH);
        try {
            taskViewModel.addTask(sample);
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
    }

    public void createNotificationThread(Task task) {
        // Only make notifications for tasks that are in the future.
        if (task.getDue() != null && task.getDue().after(new Date())) {

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Create an Intent for the activity you want to start
                    Intent resultIntent = new Intent(TasksActivity.this, TasksActivity.class);
                    // Create the TaskStackBuilder and add the intent, which inflates the back stack
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(TasksActivity.this);
                    stackBuilder.addNextIntentWithParentStack(resultIntent);

                    // Get the PendingIntent containing the entire back stack
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                    long due = task.getDue().getTime();
                    NotificationCompat.Builder builder =
                            new NotificationCompat.Builder(TasksActivity.this, TasksActivity.TASKS_NOTIFICATION_CHANNEL)
                                    .setSmallIcon(R.drawable.ic_baseline_notifications_active_24)
                                    .setContentTitle("Your task is overdue!")
                                    .setContentText(task.getDescription())
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setContentIntent(resultPendingIntent)
                                    .setAutoCancel(true)
                                    .setWhen(due)
                                    .setShowWhen(true);

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(TasksActivity.this);
                    try {
                        Thread.sleep(due - new Date().getTime());
                        notificationManager.notify(task.getId().intValue(), builder.build());
                    } catch (InterruptedException e) {
                        System.err.println("Notification malfunctioned.");
                    }
                }
            });
            thread.start();

        }
    }
}