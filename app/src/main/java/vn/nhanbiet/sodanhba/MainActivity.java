package vn.nhanbiet.sodanhba;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.SharedPreferences;

public class MainActivity extends Activity {
    private TextView status;
    private Button toggle;
    private SharedPreferences prefs;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32,40,32,32); root.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView title = new TextView(this); title.setText("SỐ TÊN DANH BẠ"); title.setTextSize(25); title.setGravity(Gravity.CENTER); root.addView(title);
        TextView info = new TextView(this); info.setText("Tìm chuỗi 3–4 chữ số trong phần chữ mà ứng dụng khác cho phép đọc, rồi hiện tất cả tên và số điện thoại trong danh bạ có chứa chuỗi đó (tối đa 12 kết quả). Không phát âm thanh.\n\nBản thử nghiệm chưa nhận diện chữ nằm hoàn toàn trong ảnh."); info.setTextSize(16); info.setPadding(0,28,0,24); root.addView(info);
        toggle = new Button(this); root.addView(toggle); updateToggleText();
        toggle.setOnClickListener(v -> { boolean active = !prefs.getBoolean("active", true); prefs.edit().putBoolean("active", active).apply(); NumberAccessibilityService.setActive(active); updateToggleText(); updateStatus(); });
        Button contacts = new Button(this); contacts.setText("1. Cấp quyền đọc danh bạ"); root.addView(contacts);
        contacts.setOnClickListener(v -> { if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 10); else updateStatus(); });
        Button accessibility = new Button(this); accessibility.setText("2. Mở cài đặt Trợ năng"); root.addView(accessibility);
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        status = new TextView(this); status.setTextSize(15); status.setPadding(0,20,0,0); root.addView(status);
        Button refresh = new Button(this); refresh.setText("Kiểm tra quyền"); root.addView(refresh); refresh.setOnClickListener(v -> updateStatus());
        setContentView(root); updateStatus();
    }
    @Override public void onRequestPermissionsResult(int r, String[] p, int[] g) { super.onRequestPermissionsResult(r,p,g); updateStatus(); }
    private void updateToggleText() { if (toggle != null) toggle.setText(prefs.getBoolean("active", true) ? "TẮT nhận diện" : "BẬT nhận diện"); }
    private void updateStatus() { if (status == null) return; boolean c=checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED; status.setText("Quyền danh bạ: "+(c?"đã cấp":"chưa cấp")+"\nNhận diện: "+(prefs.getBoolean("active", true)?"đang bật":"đang tắt")+"\nBật dịch vụ trong Cài đặt > Trợ năng > Số tên danh bạ."); }
}
