package com.xueni97.guardian;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * 微信自动拨号无障碍服务
 * 参考开源项目 davidche1116/wechat_video_call 的状态机方案：
 * 回主页 -> 点搜索 -> 输入备注名 -> 点联系人 -> 点"+" -> 点视频/语音通话 -> 确认弹窗
 *
 * 微信控件 ID 随版本变化，因此查找顺序统一为：
 * 文本/描述匹配 -> viewId 快速路径 -> 手势坐标点击兜底
 */
public class WeChatCallService extends AccessibilityService {

    // 状态机
    private static final int STATE_IDLE = 0;      // 空闲
    private static final int STATE_MAIN = 1;      // 确保回到微信主页
    private static final int STATE_SEARCH = 2;    // 点搜索入口
    private static final int STATE_INPUT = 3;     // 输入搜索关键词
    private static final int STATE_CONTACT = 4;   // 点击搜索到的联系人
    private static final int STATE_MORE = 5;      // 点聊天页 "+"
    private static final int STATE_MENU = 6;      // 点面板"视频通话/语音通话"
    private static final int STATE_CONFIRM = 7;   // 确认弹窗（视频通话需二次确认）

    private static int state = STATE_IDLE;
    private static String keyword = "";
    private static boolean isVideo = true;
    private static long taskStartTime = 0;
    private static long stateStartTime = 0;

    private static WeChatCallService instance;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onInterrupt() {
        state = STATE_IDLE;
    }

    @Override
    public void onDestroy() {
        instance = null;
        state = STATE_IDLE;
        super.onDestroy();
    }

    // 服务是否已连接（无障碍是否可用）
    public static boolean isReady() {
        return instance != null;
    }

    // 发起自动拨号任务：searchKeyword 为微信备注名/昵称，video=true 视频通话
    public static boolean startCall(String searchKeyword, boolean video) {
        if (instance == null) return false;
        keyword = searchKeyword;
        isVideo = video;
        state = STATE_MAIN;
        taskStartTime = System.currentTimeMillis();
        stateStartTime = System.currentTimeMillis();
        return true;
    }

    public static void resetTask() {
        state = STATE_IDLE;
        keyword = "";
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (state == STATE_IDLE || instance == null || event == null) return;

        // 只处理微信窗口事件
        CharSequence pkg = event.getPackageName();
        if (pkg == null || !pkg.toString().contains("tencent.mm")) return;

        // 60 秒总超时保护
        if (System.currentTimeMillis() - taskStartTime > 60000) {
            resetTask();
            return;
        }

        String activity = event.getClassName() == null ? "" : event.getClassName().toString();

        try {
            switch (state) {
                case STATE_MAIN:    handleMain(activity); break;
                case STATE_SEARCH:  handleSearch(); break;
                case STATE_INPUT:   handleInput(); break;
                case STATE_CONTACT: handleContact(); break;
                case STATE_MORE:    handleMore(activity); break;
                case STATE_MENU:    handleMenu(); break;
                case STATE_CONFIRM: handleConfirm(); break;
            }
        } catch (Exception ignored) {
            // 自动化容错，不允许崩溃
        }
    }

    // ============ 状态1：回到微信主页，点底部"微信"tab ============
    private void handleMain(String activity) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        if (activity.contains("LauncherUI")) {
            // 微信主界面：点底部"微信"tab（确保在会话列表页，搜索入口可用）
            int screenH = getResources().getDisplayMetrics().heightPixels;
            List<AccessibilityNodeInfo> tabs = root.findAccessibilityNodeInfosByText("微信");
            for (AccessibilityNodeInfo t : tabs) {
                Rect r = new Rect();
                t.getBoundsInScreen(r);
                if (r.top > screenH * 0.75) {
                    clickNode(t);
                    sleep(500);
                    enterState(STATE_SEARCH);
                    return;
                }
            }
            // 没找到 tab（可能已在首页），直接进入搜索状态
            enterState(STATE_SEARCH);
        } else if (activity.toLowerCase().contains("dialog")) {
            // 其他弹窗页面，返回关闭
            performGlobalAction(GLOBAL_ACTION_BACK);
            sleep(500);
        }
    }

    // ============ 状态2：点击主页搜索入口 ============
    private void handleSearch() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        // 1) 文本/描述"搜索"（跨版本最稳）
        List<AccessibilityNodeInfo> search = root.findAccessibilityNodeInfosByText("搜索");
        for (AccessibilityNodeInfo n : search) {
            if (n.isEditable()) continue; // 跳过搜索输入框本身
            if (clickNode(n)) {
                sleep(600);
                enterState(STATE_INPUT);
                return;
            }
        }
        // 2) viewId 快速路径（参考项目：jha）
        List<AccessibilityNodeInfo> byId = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/jha");
        if (!byId.isEmpty() && clickNode(byId.get(0))) {
            sleep(600);
            enterState(STATE_INPUT);
        }
        // 找不到则等待下次窗口事件重试（60 秒超时兜底）
    }

    // ============ 状态3：搜索框输入关键词 ============
    private void handleInput() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        AccessibilityNodeInfo edit = findEditText(root);
        if (edit == null) return;

        boolean ok = setText(edit, keyword);
        if (ok) {
            sleep(800);
            enterState(STATE_CONTACT);
        }
    }

    // ============ 状态4：点击搜索结果中的联系人 ============
    private void handleContact() {
        // 等搜索结果加载
        sleep(600);
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(keyword);
        for (AccessibilityNodeInfo n : nodes) {
            if (n.isEditable()) continue; // 跳过输入框
            CharSequence t = n.getText();
            if (t != null && t.toString().contains(keyword)) {
                if (clickNode(n)) {
                    sleep(800);
                    enterState(STATE_MORE);
                    return;
                }
            }
        }
        // 结果未加载完，等下次事件重试
    }

    // ============ 状态5：聊天页点击 "+" ============
    private void handleMore(String activity) {
        if (!activity.contains("Chat")) return; // 等待聊天界面加载
        sleep(400);
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        int screenH = getResources().getDisplayMetrics().heightPixels;

        // 1) viewId 快速路径（参考项目：bjz）
        List<AccessibilityNodeInfo> more = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bjz");
        if (!more.isEmpty() && clickNode(more.get(0))) {
            sleep(900);
            enterState(STATE_MENU);
            return;
        }
        // 2) 文本/描述 "+"、"更多功能"、"更多"
        List<AccessibilityNodeInfo> byText = root.findAccessibilityNodeInfosByText("+");
        List<AccessibilityNodeInfo> byDesc = root.findAccessibilityNodeInfosByText("更多功能");
        List<AccessibilityNodeInfo> byDesc2 = root.findAccessibilityNodeInfosByText("更多");
        List<AccessibilityNodeInfo> all = new java.util.ArrayList<>();
        all.addAll(byText);
        all.addAll(byDesc);
        all.addAll(byDesc2);
        for (AccessibilityNodeInfo n : all) {
            Rect r = new Rect();
            n.getBoundsInScreen(r);
            // "+" 在屏幕下半部分右侧
            if (r.centerY() > screenH * 0.6 && r.width() > 0) {
                if (clickNode(n) || clickByGesture(r)) {
                    sleep(900);
                    enterState(STATE_MENU);
                    return;
                }
            }
        }
    }

    // ============ 状态6：点面板"视频通话/语音通话" ============
    private void handleMenu() {
        sleep(500);
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        String target = isVideo ? "视频通话" : "语音通话";
        List<AccessibilityNodeInfo> menu = root.findAccessibilityNodeInfosByText(target);
        for (AccessibilityNodeInfo n : menu) {
            if (clickNode(n) || clickByGestureOfNode(n)) {
                sleep(900);
                enterState(STATE_CONFIRM);
                return;
            }
        }
        // 兜底：遍历文本/描述包含目标的节点
        List<AccessibilityNodeInfo> all = root.findAccessibilityNodeInfosByText("");
        for (AccessibilityNodeInfo n : all) {
            CharSequence t = n.getText();
            CharSequence d = n.getContentDescription();
            boolean hit = (t != null && t.toString().contains(target))
                    || (d != null && d.toString().contains(target));
            if (hit && (clickNode(n) || clickByGestureOfNode(n))) {
                sleep(900);
                enterState(STATE_CONFIRM);
                return;
            }
        }
        // 面板未完全弹出，等下次事件重试
    }

    // ============ 状态7：确认弹窗（视频通话需再点一次确认） ============
    private void handleConfirm() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        String target = isVideo ? "视频通话" : "语音通话";

        if (root != null) {
            List<AccessibilityNodeInfo> options = root.findAccessibilityNodeInfosByText(target);
            for (AccessibilityNodeInfo n : options) {
                if (clickNode(n)) {
                    sleep(500);
                    resetTask(); // 拨出完成
                    return;
                }
            }
        }
        // 语音通话一般无确认弹窗，直接拨出；等待 6 秒无弹窗视为完成
        if (System.currentTimeMillis() - stateStartTime > 6000) {
            resetTask();
        }
    }

    private void enterState(int s) {
        state = s;
        stateStartTime = System.currentTimeMillis();
    }

    // ============ 工具函数 ============

    // 点击节点：优先自身，否则向上找可点击父节点
    private boolean clickNode(AccessibilityNodeInfo node) {
        if (node == null) return false;
        if (node.isClickable()) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        AccessibilityNodeInfo p = node.getParent();
        while (p != null) {
            if (p.isClickable()) return p.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            p = p.getParent();
        }
        return false;
    }

    // 递归查找可编辑输入框
    private AccessibilityNodeInfo findEditText(AccessibilityNodeInfo node) {
        if (node == null) return null;
        CharSequence cls = node.getClassName();
        if (node.isEditable() && cls != null && cls.toString().contains("EditText")) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findEditText(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }

    // 输入文本：SET_TEXT 优先，失败用剪贴板粘贴
    private boolean setText(AccessibilityNodeInfo edit, String text) {
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        if (edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return true;

        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("kw", text));
        edit.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        return edit.performAction(AccessibilityNodeInfo.ACTION_PASTE);
    }

    // 手势坐标点击（面板按钮 performAction 无效时兜底）
    private boolean clickByGesture(Rect r) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        Path path = new Path();
        path.moveTo(r.exactCenterX(), r.exactCenterY());
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 30));
        return dispatchGesture(builder.build(), null, null);
    }

    private boolean clickByGestureOfNode(AccessibilityNodeInfo node) {
        if (node == null) return false;
        Rect r = new Rect();
        node.getBoundsInScreen(r);
        if (r.width() <= 0 && node.getParent() != null) {
            node.getParent().getBoundsInScreen(r);
        }
        return clickByGesture(r);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
