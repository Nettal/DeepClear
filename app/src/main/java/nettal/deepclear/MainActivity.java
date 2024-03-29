package nettal.deepclear;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class MainActivity extends Activity {

    public static final String FileName = "WhiteList";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ArrayList<DialogView> fullAppView = new ArrayList<>();
        Button startService = findViewById(R.id.activity_main_StartService);
        Button whiteList = findViewById(R.id.activity_main_WhiteList);
        Button about = findViewById(R.id.activity_main_About);
        startService.setOnClickListener(p1 -> checkPermissionsAndStartService());
        whiteList.setOnClickListener(view -> {
            SearchableDialog dialog = new SearchableDialog(MainActivity.this, fullAppView);
            dialog.setPositiveButton("OK", null);
            dialog.setOnDismissListener(dialog1 -> {
                HashMap<String, Boolean> hashMap = new HashMap<>();
                for (DialogView dialogView : fullAppView) {
                    hashMap.put(dialogView.getApplicationInfo().packageName, dialogView.isEnabled());
                }
                try {
                    Utilities.saveObjectToFile(MainActivity.this, hashMap, FileName);
                } catch (Exception e) {
                    Utilities.toast(Utilities.printLog(e), MainActivity.this);
                }
                checkPermissionsAndStartService();
            });
            dialog.show();
        });
        whiteList.setEnabled(false);
        whiteList.post(() -> new Thread(() -> {
            Looper.prepare();
            final List<ApplicationInfo> fullAppList = Utilities.getAllApplications(MainActivity.this);
            try {//获取到白名单
                HashMap<String, Boolean> hashMap = Utilities.loadObjectFromFile(MainActivity.this, FileName);
                for (ApplicationInfo info : fullAppList) {
                    DialogView dv = new DialogView(MainActivity.this, info, (int) (whiteList.getWidth() / 1.6), whiteList.getTextSize() / 1.7f);
                    dv.setEnabled(Boolean.TRUE.equals(hashMap.getOrDefault(info.packageName, Utilities.isSystemApp(info))));
                    fullAppView.add(dv);
                }
            } catch (Exception e) {//没获取到白名单
                for (ApplicationInfo info : fullAppList) {
                    DialogView dv = new DialogView(MainActivity.this, info, (int) (whiteList.getWidth() / 1.6), whiteList.getTextSize() / 1.7f);
                    dv.setEnabled(Utilities.isSystemApp(info) || info.packageName.equals(MainActivity.this.getPackageName()));
                    fullAppView.add(dv);
                }
            }
            runOnUiThread(() -> whiteList.setEnabled(true));
        }).start());
        about.setOnClickListener(v -> Toast.makeText(MainActivity.this, R.string.about_detail, Toast.LENGTH_LONG).show());
        about.setOnLongClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Nettal/DeepClear")));
            return true;
        });
    }

    private void checkPermissionsAndStartService() {
        if (!Utilities.ignoreBatteryOptimization(MainActivity.this)) {
            Toast.makeText(MainActivity.this, getString(R.string.non_ignoreBatteryOptimization), Toast.LENGTH_LONG).show();
            return;
        }
        if (!Command.checkSU()) {
            Toast.makeText(MainActivity.this, getString(R.string.non_SU), Toast.LENGTH_LONG).show();
            return;
        }
        stopService(new Intent(MainActivity.this, ForceStopService.class));
        startService(new Intent(MainActivity.this, ForceStopService.class));
    }
}

@SuppressLint("ViewConstructor")
final class DialogView extends LinearLayout {
    private final TextView textView;
    private final ApplicationInfo applicationInfo;

    public DialogView(Context context, ApplicationInfo info, int iconSize, float textSize) {
        super(context);
        applicationInfo = info;
        textView = new TextView(context);
        textView.setTextSize(textSize);
        textView.setText(context.getPackageManager().getApplicationLabel(info).toString().equals(info.packageName)
                ? info.packageName : context.getPackageManager().getApplicationLabel(info) + "\n" + info.packageName);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        ImageView imageView = new ImageView(context);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        imageView.setImageDrawable(context.getPackageManager().getApplicationIcon(info));
        addView(imageView, iconSize, iconSize);
        addView(textView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        this.setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
    }

    @Override
    public String toString() {
        return textView.getText().toString();
    }

    @Override
    public void setEnabled(boolean enabled) {
        textView.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    public ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }
}
