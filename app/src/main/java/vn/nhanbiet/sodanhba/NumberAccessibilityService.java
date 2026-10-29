package vn.nhanbiet.sodanhba;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.WindowManager;
import android.view.Gravity;
import android.graphics.PixelFormat;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberAccessibilityService extends AccessibilityService {
    private static volatile NumberAccessibilityService instance;
    public static void setActive(boolean active) {
        NumberAccessibilityService s = instance;
        if (s != null && !active) s.hide();
    }
    private WindowManager wm;
    private TextView badge;
    private String last = "";
    private long contactsLoadedAt = 0;
    private final List<ContactPhone> contactPhones = new ArrayList<>();
    private final Pattern digitRun = Pattern.compile("\\d{3,}");

    private static class ContactPhone {
        final String name, phone, digits;
        ContactPhone(String n, String p) {
            name = n == null ? "(Không có tên)" : n;
            phone = p == null ? "" : p;
            digits = phone.replaceAll("[^0-9]", "");
        }
    }

    @Override public void onServiceConnected() {
        instance = this;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!getSharedPreferences("settings", MODE_PRIVATE).getBoolean("active", true)) { hide(); return; }
        if (event == null || event.getPackageName() == null || getPackageName().contentEquals(event.getPackageName())) return;
        Set<String> texts = new HashSet<>();
        if (event.getText() != null) for (CharSequence s : event.getText()) if (s != null) texts.add(s.toString());
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) { collect(root, texts, 0); root.recycle(); }

        LinkedHashSet<String> fragments = new LinkedHashSet<>();
        for (String text : texts) {
            Matcher m = digitRun.matcher(text);
            while (m.find()) {
                String digits = m.group();
                for (int len = 4; len >= 3; len--) {
                    for (int i = 0; i + len <= digits.length(); i++) fragments.add(digits.substring(i, i + len));
                }
            }
        }
        if (fragments.isEmpty()) return;
        loadContactsIfNeeded();
        for (String fragment : fragments) {
            if (fragment.length() != 4) continue;
            List<ContactPhone> matches = findMatches(fragment);
            if (!matches.isEmpty()) { show(formatMatches(fragment, matches)); return; }
        }
        for (String fragment : fragments) {
            if (fragment.length() != 3) continue;
            List<ContactPhone> matches = findMatches(fragment);
            if (!matches.isEmpty()) { show(formatMatches(fragment, matches)); return; }
        }
    }

    private void collect(AccessibilityNodeInfo n, Set<String> out, int depth) {
        if (n == null || depth > 18 || out.size() > 500) return;
        CharSequence t = n.getText(), d = n.getContentDescription();
        if (t != null) out.add(t.toString());
        if (d != null) out.add(d.toString());
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo c = n.getChild(i);
            if (c != null) { collect(c, out, depth + 1); c.recycle(); }
        }
    }

    private void loadContactsIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - contactsLoadedAt < 30000 && !contactPhones.isEmpty()) return;
        contactPhones.clear();
        if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        String[] projection = { ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER };
        try (Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, null)) {
            if (c != null) while (c.moveToNext()) {
                contactPhones.add(new ContactPhone(c.getString(0), c.getString(1)));
            }
            contactsLoadedAt = now;
        } catch (Exception ignored) { }
    }

    private List<ContactPhone> findMatches(String fragment) {
        List<ContactPhone> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ContactPhone cp : contactPhones) {
            if (cp.digits.contains(fragment)) {
                String key = cp.name + "|" + cp.digits;
                if (seen.add(key)) result.add(cp);
                if (result.size() >= 12) break;
            }
        }
        return result;
    }

    private String formatMatches(String fragment, List<ContactPhone> matches) {
        StringBuilder b = new StringBuilder("Khớp ").append(fragment).append(" — ").append(matches.size()).append(" kết quả");
        for (ContactPhone cp : matches) b.append("\n\n").append(cp.name).append("\n").append(cp.phone);
        if (matches.size() >= 12) b.append("\n\n(Đã giới hạn 12 kết quả)");
        return b.toString();
    }

    private void show(String text) {
        if (wm == null) wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (text.equals(last)) return;
        last = text;
        if (badge == null) {
            badge = new TextView(this);
            badge.setTextColor(Color.WHITE);
            badge.setTextSize(15);
            badge.setPadding(28, 18, 28, 18);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.rgb(28, 105, 180));
            bg.setCornerRadius(24);
            badge.setBackground(bg);
            badge.setOnClickListener(v -> hide());
        }
        badge.setText(text + "\n\n(chạm để ẩn)");
        int type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
            type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = 80;
        try { if (badge.getParent() == null) wm.addView(badge, lp); else wm.updateViewLayout(badge, lp); } catch (Exception ignored) { }
        badge.postDelayed(this::hide, 10000);
    }

    private void hide() {
        try { if (wm != null && badge != null && badge.getParent() != null) wm.removeView(badge); } catch (Exception ignored) { }
        last = "";
    }
    @Override public void onInterrupt() { hide(); }
    @Override public void onDestroy() { hide(); if (instance == this) instance = null; super.onDestroy(); }
}
