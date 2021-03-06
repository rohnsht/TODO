package com.ace.legend.todo;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.software.shell.fab.ActionButton;

import java.util.Calendar;


public class TodoDetail extends ActionBarActivity {

    private Toolbar toolBar;
    AlarmManager alarmManager;
    EditText et_title, et_detail;
    TextView tv_remainder;
    Button btn_save;
    DatabaseHandler db;
    String title, detail, date, time;
    boolean togglePref;
    PendingIntent pi;
    SharedPreferences preferences;
    DatePicker datePicker;
    TimePicker timePicker;
    int reqCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.todo_detail);

        et_title = (EditText) findViewById(R.id.et_title);
        et_detail = (EditText) findViewById(R.id.et_detail);
        btn_save = (Button) findViewById(R.id.btn_save);

        initializeView();
        setFab();

        NotificationManager notificationManager = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        db = new DatabaseHandler(this);
        preferences = getPreferences(MODE_PRIVATE);

        Intent i = getIntent();
        title = i.getStringExtra("title");
        detail = i.getStringExtra("detail");
        date = i.getStringExtra("date");
        time = i.getStringExtra("time");
        reqCode = i.getIntExtra("RequestCode", 0);


        et_title.setText(title);
        et_detail.setText(detail);

        btn_save.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                String etitle = et_title.getText().toString();
                String edetail = et_detail.getText().toString();
                int count = db.updateTodo(title, detail, etitle, edetail);
                if (count == 1) {
                    Toast.makeText(getApplicationContext(), "ToDo updated",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(TodoDetail.this, AlarmReceiver.class);
        intent.putExtra("Title", title);
        intent.putExtra("Detail", detail);
        intent.putExtra("Date", date);
        intent.putExtra("Time", time);
        intent.putExtra("RequestCode", reqCode);
        pi = PendingIntent.getBroadcast(this, reqCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        RelativeLayout rl_remainder = (RelativeLayout) findViewById(R.id.rl_remainder);
        tv_remainder = (TextView) findViewById(R.id.tv_remainder);
        if (date != null && time != null) {
            tv_remainder.setText(date + " " + time);
        }
        rl_remainder.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                final Dialog d = new Dialog(TodoDetail.this);
                d.setContentView(R.layout.todo_dialog);
                d.setTitle("Set Remainder");
                d.setCancelable(true);

                datePicker = (DatePicker) d.findViewById(R.id.datePicker);
                timePicker = (TimePicker) d.findViewById(R.id.timePicker);
                Button btn_submit = (Button) d.findViewById(R.id.btn_submit);
                Button btn_cancel = (Button) d.findViewById(R.id.btn_cancel);

                btn_submit.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.MONTH, datePicker.getMonth());
                        calendar.set(Calendar.DAY_OF_MONTH,
                                datePicker.getDayOfMonth());
                        calendar.set(Calendar.YEAR, datePicker.getYear());
                        calendar.set(Calendar.HOUR_OF_DAY,
                                timePicker.getCurrentHour());
                        calendar.set(Calendar.MINUTE,
                                timePicker.getCurrentMinute());

                        alarmManager.set(AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(), pi);

                        String date = datePicker.getDayOfMonth() + "/"
                                + datePicker.getMonth() + "/"
                                + datePicker.getYear();
                        String time = timePicker.getCurrentHour() + ":"
                                + timePicker.getCurrentMinute();

                        db.addRemainder(new ToDo(title, detail, date, time));
                        tv_remainder.setText(date + " " + time);

                        d.dismiss();
                    }
                });

                btn_cancel.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        d.dismiss();
                    }
                });

                d.show();

            }
        });

    }

    private void initializeView() {
        toolBar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolBar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setFab() {
        ActionButton actionButton = (ActionButton) findViewById(R.id.action_button);
        actionButton.setButtonColor(getResources().getColor(R.color.accentColor));
        actionButton.setButtonColorPressed(getResources().getColor(R.color.accentColorDark));
        actionButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_assignment_turned_in_white_24dp));
        actionButton.setImageResource(R.drawable.ic_assignment_turned_in_white_24dp);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int num = db.shiftTodo(new ToDo(title, detail));
                if (num == 1) {
                    alarmManager.cancel(pi);
                    Toast.makeText(TodoDetail.this, "ToDo completed.", Toast.LENGTH_LONG)
                            .show();
                    finish();
                }
            }
        });
    }

    public void handleClick(View v){
        int id = v.getId();
        switch(id){
            case R.id.ibtn_newTodo:
                Intent intent = new Intent(this, AddTodo.class);
                startActivity(intent);
                break;
            case R.id.ibtn_deleteTodo:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Delete ToDo");
                builder.setMessage("Do you want to delete the task?");
                builder.setCancelable(true);
                builder.setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int pos) {
                                int count = db.deleteTodo(new ToDo(title, detail));
                                if (count == 1) {
                                    alarmManager.cancel(pi);
                                    Toast.makeText(TodoDetail.this, "ToDo deleted.",
                                            Toast.LENGTH_LONG).show();
                                    finish();
                                }
                            }
                        });
                builder.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();

                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
                break;
            case R.id.ibtn_updateTodo:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_todo_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_discard:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Delete ToDo");
                builder.setMessage("Do you want to delete the task?");
                builder.setCancelable(true);
                builder.setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int pos) {
                                int count = db.deleteTodo(new ToDo(title, detail));
                                if (count == 1) {
                                    alarmManager.cancel(pi);
                                    Toast.makeText(TodoDetail.this, "ToDo deleted.",
                                            Toast.LENGTH_LONG).show();
                                    finish();
                                }
                            }
                        });
                builder.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();

                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();

                return true;


            case R.id.action_add:
                Intent intent = new Intent(this, AddTodo.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}