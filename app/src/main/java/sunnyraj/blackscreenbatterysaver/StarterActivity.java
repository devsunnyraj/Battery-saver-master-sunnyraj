package sunnyraj.blackscreenbatterysaver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

public class StarterActivity extends AppCompatActivity {

    public final static String ACTION_PREVENT_QUICKSTART = "com.busu.blackscreenbatterysaver.ACTION_PREVENT_QUICK";

    private Preferences mPrefs;

    private Button mBtnStartStop, mBtnTutorial;
    private TextView mStatus;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (BatterySavingService.EVENT_STATUS_CHANGED.equals(intent.getAction())) {
                    serviceStatusChanged(
                            (BatterySavingService.State) intent.getSerializableExtra(BatterySavingService.BROADCAST_CURRENT_STATE));
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        mPrefs = new Preferences(this);
        if (hasToCancelActivityAndStartService(savedInstanceState != null)) {
            startTheService();
            return;
        }

        setContentView(R.layout.starter);

        mBtnStartStop = findViewById(R.id.sBtnStartStop);
        mBtnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BatterySavingService.state == BatterySavingService.State.ACTIVE) {
                    startTheService(BatterySavingService.ACTION_STOP, false);
                } else {
                    checkDrawOverlayPermission();
                }
            }
        });

        mBtnTutorial = findViewById(R.id.sBtnTutorial);
        mBtnTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTheService(BatterySavingService.ACTION_TUTORIAL, true);
            }
        });

        mStatus = findViewById(R.id.sStatus);

        //final TextView rate = findViewById(R.id.sTxtRate);
        //rate.setPaintFlags(rate.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);


        serviceStatusChanged(BatterySavingService.state);
    }

    private boolean hasToCancelActivityAndStartService(boolean isAfterConfigChange) {
        if (BatterySavingService.state == BatterySavingService.State.ACTIVE || !canDrawOverlay(this)) {
            //start activity if service is already running: users want to configure smth
            return false;
        }

        Intent startIntent = getIntent();
        if (startIntent == null) {
            //this should never happen
            startIntent = new Intent(this, StarterActivity.class);
            startIntent.setAction(ACTION_PREVENT_QUICKSTART);
        }
        //extract action from intent so that the intent can be updated
        String startAction = startIntent.getAction();

        //once the activity started, prevent not starting after rotation
        startIntent.setAction(ACTION_PREVENT_QUICKSTART);
        setIntent(startIntent);

        if (!ACTION_PREVENT_QUICKSTART.equals(startAction) && !isAfterConfigChange) {
            return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter(BatterySavingService.EVENT_STATUS_CHANGED);
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
    }

    public final static int REQUEST_CODE = 1;

    public void checkDrawOverlayPermission() {
        if (!canDrawOverlay(this)) {
            new AlertDialog.Builder(this).setCancelable(true).
                    setMessage(R.string.overlay_enabling_dialog).
                    setPositiveButton(R.string.overlay_proceed, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, REQUEST_CODE);
                        }
                    }).
                    setNegativeButton(R.string.overlay_cancel, null).
                    create().show();

        } else {
            startTheService();
        }
    }


    public static boolean canDrawOverlay(Context context) {
        return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (canDrawOverlay(this)) {
                startTheService();
            } else {
                Snackbar.make(mBtnStartStop, R.string.pleaseEnableOverlay, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void startTheService() {
        startTheService(BatterySavingService.ACTION_START, true);
    }

    /**
     * Start the service only on action start and send commands for the other actions only if the service is already started
     *
     * @param action
     * @param hasToCloseActivity
     */
    private void startTheService(String action, boolean hasToCloseActivity) {
        if (action == BatterySavingService.ACTION_START || BatterySavingService.state == BatterySavingService.State.ACTIVE) {
            startService(new Intent(StarterActivity.this, BatterySavingService.class).setAction(action));
            if (hasToCloseActivity) {
                finish();
            }
        }
    }

    private void serviceStatusChanged(BatterySavingService.State currentState) {
        final boolean isStarted = (BatterySavingService.State.ACTIVE == currentState);
        //
        mBtnStartStop.setText(isStarted ? R.string.btn_stop : R.string.btn_start);
        mStatus.setText(isStarted ? R.string.status_started : R.string.status_stopped);
        mStatus.setTextColor(isStarted ? Color.GREEN : Color.RED);
        //
        if (isStarted) {
            mBtnTutorial.setVisibility(View.VISIBLE);
        } else {
            mBtnTutorial.setVisibility(View.GONE);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}