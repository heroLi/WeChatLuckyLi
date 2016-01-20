package com.lixiao.wechat.service;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.PowerManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by lixiao on 16/1/16.
 * 无障碍服务
 */
public class WeChatService extends AccessibilityService {

    private final String WXHB = "[微信红包]";
    private final String LOGTAG = "WeChat";
    //红包详情
    private final String WXHbDetail = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";
    //聊天UI
    private final String WXLauncherUI = "com.tencent.mm.ui.LauncherUI";
    //微信点击请红包的之后的ui
    private final String WXReceiveUI = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI";
    //领取红包的id
    private final String OtherRedViewId = "com.tencent.mm:id/ci";
    //微信红包详情返回
    private final String RedDetailBack = "com.tencent.mm:id/c2m";
    //查看大家手气
    private final String LookRedDetail = "com.tencent.mm:id/b2e";

    private final String WXOther = "领取红包";
    private final String WXOner = "查看红包";

    private static final int MAX_CACHE_TOLERANCE = 10000;

    //抢红包的方式  1 只为抢红包  到详情之后 返回到聊天泪飙
    public static int redType = 1;

    private final List<Integer> receiveHb = new ArrayList<Integer>();
    //可以抢红包
    private boolean isReceiveRed = true;
    //正在抢红包
    private boolean isReceiveing = false;

    private List<AccessibilityNodeInfo> clickRed = new ArrayList<AccessibilityNodeInfo>();

    private String lastFetchedHongbaoId = "";
    private String lastFetchedHongbaoHash = "";
    private String lastTalkRedName = "";//最后一个发红包的名字

    private long lastFetchedTime = 0;
    //当前ui页面
    private String currentUI = "";

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private KeyguardManager keyguardManager;

    private KeyguardManager.KeyguardLock keyguardLock;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOGTAG, "oncreate");
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "WeChat");
        keyguardManager = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        keyguardLock =  keyguardManager.newKeyguardLock("unlock");
    }

    /*
        * 接收无障碍反馈事件
        * */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(LOGTAG, " onAccessibilityEvent " + event.getEventType());
        switch (event.getEventType()) {

//            点击事件
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                String className1 = event.getClassName().toString();
                Log.d(LOGTAG, className1 + "   TYPE_VIEW_CLICKED");
                break;
//            通知栏状态改变  聊天窗口时 也能收到 通知 会造成 1此过量点击
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                List<CharSequence> notifys = event.getText();
                if (notifys != null) {
                    for (CharSequence charSequence : notifys) {
                        String text = charSequence.toString();
                        Loger(text);
                        if (text.contains(WXHB) && isReceiveRed && text.lastIndexOf("]") < text.length() - 1) {
                            //收到微信红包 打开窗口
                            if(redType==0){
                                keyguardLock.disableKeyguard();
                                wakeLock.acquire();
                            }
                            if (event.getParcelableData() != null
                                    && event.getParcelableData() instanceof Notification) {
                                Loger(WXHB + "  TYPE_NOTIFICATION_STATE_CHANGED");
                                Notification notification = (Notification) event
                                        .getParcelableData();
                                PendingIntent pendingIntent = notification.contentIntent;
                                try {
                                    pendingIntent.send();
                                } catch (PendingIntent.CanceledException e) {
                                    e.printStackTrace();
                                }
                            }

                        }
                    }
                }
                break;
            //界面窗口变化
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                String className = event.getClassName().toString();
                Log.d(LOGTAG, className + " TYPE_WINDOW_STATE_CHANGED " + isReceiveRed);
                currentUI = className;
                if (className.equals(WXLauncherUI) && isReceiveRed) {
                    getWxHb(new String[]{WXOther, WXOner});
//                    listViewText(event);
                } else if (className.equals(WXReceiveUI)) {
                    openWxHb();
                } else if (className.equals(WXHbDetail)) {
                    redDetailBack();
                    isReceiveRed = true;
                }
                break;
            //界面内容变化
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                String name = event.getClassName().toString();
                Log.d(LOGTAG, name + " TYPE_WINDOW_CONTENT_CHANGED   " + isReceiveRed);
                if (name.equals("android.widget.TextView") && isReceiveRed && currentUI.equals(WXLauncherUI)) {
                    getWxHb(new String[]{WXOther, WXOner});
                }
                break;
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                Log.d(LOGTAG, "TYPE_WINDOWS_CHANGED");
                break;
            case AccessibilityEvent.TYPE_VIEW_SELECTED:
                Log.d(LOGTAG, event.getEventType() + "TYPE_VIEW_SELECTED");
                break;
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                Log.d(LOGTAG, event.getEventType() + "TYPE_VIEW_FOCUSED");
                break;
            case AccessibilityEvent.TYPE_VIEW_SCROLLED://listview 滚动
                String viewName = event.getClassName().toString();
                if (viewName.equals("android.widget.TextView") && currentUI.equals(WXLauncherUI)) {
                    isReceiveRed = true;

                }
                Log.d(LOGTAG, event.getEventType() + "TYPE_VIEW_SCROLLED" + viewName);
                break;
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:
                break;

        }

    }

    @Override
    public void onInterrupt() {

    }

    private void listViewText(AccessibilityEvent event) {
        AccessibilityNodeInfo accessibilityNodeInfo = event.getSource();
        listAccessibilityNodeInfo(accessibilityNodeInfo);

    }

    private void listAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        if (accessibilityNodeInfo != null) {
            for (int i = 0; i < accessibilityNodeInfo.getChildCount(); i++) {
                AccessibilityNodeInfo nodeInfo = accessibilityNodeInfo.getChild(i);
                if (nodeInfo.getChildCount() > 0) {
                    listAccessibilityNodeInfo(nodeInfo);
                } else {
                    if (nodeInfo.getText() != null) {
                        Loger(nodeInfo.getText().toString());
                    }
                    Loger(nodeInfo.toString());
                }

            }
        }
    }

    /**
     * 只打开最后一个红包
     */
    private void getWxHb(String[] red) {

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            List<AccessibilityNodeInfo> nodeInfos = getWxRedNodeInfo(rootNode, red[0]);
            if (nodeInfos.size() > 0) {
                AccessibilityNodeInfo lastClick = nodeInfos.get(nodeInfos.size() - 1);
                if (lastClick.getParent() != null && lastClick.getParent().getClassName().equals("android.widget.LinearLayout")) {
                    String id = getHongbaoText(lastClick);
                    String redName = getWxHbName(rootNode);
//                    String redName = getWxHbName(rootNode); //获取
                    if (isRedClickBoolean(redName, id)) {
                        isReceiveRed = false;
                        AccessibilityNodeInfo parent = lastClick.getParent();
                        if (parent != null) {
                            Loger("Red    ACTION_CLICK");
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                }
            }
            //抢自己的红包
//            List<AccessibilityNodeInfo> nodeInfosOvenr = getWxRedNodeInfo(rootNode, red[1]);
//            if (nodeInfosOvenr.size() > 0) {
//                AccessibilityNodeInfo lastClick = nodeInfosOvenr.get(nodeInfosOvenr.size() - 1);
//                if (lastClick.getParent() != null && lastClick.getParent().getClassName().equals("android.widget.LinearLayout")) {
//                    String id = getHongbaoText(lastClick);
//                    String redName = "李潇";
//                    if (isRedClickBoolean(redName, id)) {
//                        AccessibilityNodeInfo parent = lastClick.getParent();
//                        if (parent != null) {
//                            Loger("Red    ACTION_CLICK");
//                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                        }
//                    }
//                }
//            }

        }
    }

    /*最后一个红包的名字*/
    private String getWxHbName(AccessibilityNodeInfo rootNode) {
        try {

            List<AccessibilityNodeInfo> list = rootNode.findAccessibilityNodeInfosByViewId(OtherRedViewId);
            AccessibilityNodeInfo info = list.get(list.size() - 1);
            return info.getChild(0).getText().toString();
        } catch (Exception c) {
            return lastTalkRedName;
        }
    }


    /**
     * 判断红包是否可以点击  拍重 ，无效红包
     */
    private boolean isRedClick(AccessibilityNodeInfo nodeInfo) {
        long now = System.currentTimeMillis();
        AccessibilityNodeInfo talkParent = nodeInfo.getParent();
        String talkName = "-1";
        if (talkParent != null && talkParent.getChildCount() > 0 && talkParent.getChild(0).getText() != null) {
            talkName = talkParent.getChild(0).getText().toString();
        }
        String id = getHongbaoText(nodeInfo);
        Loger("getHongbaoText   " + id);
        Loger("talkName   " + talkName);
        if ((now - lastFetchedTime) < MAX_CACHE_TOLERANCE && id.equals(lastFetchedHongbaoId) && talkName.equals(lastTalkRedName)) {
            return false;
        }
        lastFetchedTime = now;
        lastFetchedHongbaoId = id;
        lastTalkRedName = talkName;
        return true;
    }

    /**
     * 判断红包是否可以点击  拍重 ，无效红包
     */
    private boolean isRedClickBoolean(String talkName, String redTitle) {
        long now = System.currentTimeMillis();
        Loger("getHongbaoText   " + redTitle);
        Loger("talkName   " + talkName);
        Loger("time   " + ((now - lastFetchedTime) < MAX_CACHE_TOLERANCE));
        Loger("title   " + (redTitle.equals(lastFetchedHongbaoId)));
        Loger("name   " + (talkName.equals(lastTalkRedName)));
//        if ((now - lastFetchedTime) < MAX_CACHE_TOLERANCE && redTitle.equals(lastFetchedHongbaoId) && talkName.equals(lastTalkRedName)) {
//            return false;
//        }
        if ((now - lastFetchedTime) < MAX_CACHE_TOLERANCE && redTitle.equals(lastFetchedHongbaoId)) {
            return false;
        }
        lastFetchedTime = now;
        lastFetchedHongbaoId = redTitle;
        lastTalkRedName = talkName;

        return true;
    }


    /**
     * 将节点对象的id和红包上的内容合并
     * 用于表示一个唯一的红包
     *
     * @param node 任意对象
     * @return 红包标识字符串
     */

    private String getHongbaoText(AccessibilityNodeInfo node) {
        /* 获取红包上的文本 */
        String content;
        try {
            AccessibilityNodeInfo i = node.getParent().getChild(0);
            content = i.getText().toString();
        } catch (NullPointerException npe) {
            return "-1";
        }

        return content;
    }

    /**
     * 判断是否返回,减少点击次数
     * 现在的策略是当红包文本和缓存不一致时,戳
     * 文本一致且间隔大于MAX_CACHE_TOLERANCE时,戳
     *
     * @param id       红包id
     * @param duration 红包到达与缓存的间隔
     * @return 是否应该返回
     */
    private boolean shouldReturn(String id, long duration) {
        // ID为空
        if (id == null) return true;

        // 名称和缓存不一致
        if (duration < MAX_CACHE_TOLERANCE && id.equals(lastFetchedHongbaoId)) return true;

        return false;
    }

    private void Loger(String text) {
        Log.d(LOGTAG, text);
    }

    /*
    om.tencent.mm:id/ya  领取红包的布局
    * com.tencent.mm:id/h4   聊天名字
    * com.tencent.mm:id/h5  聊天内容
    * */
    private List<AccessibilityNodeInfo> getAccessibilityListById(AccessibilityNodeInfo nodeInfo, String id) {
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(id);

        return list;

    }

    //领取红包  遍历红包整个布局 拿到 红包的名字 暂不可用
    private void openOtherWxRed() {
        //获取试图
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ya");//可点击的红包viewlist
            if (list.size() > 0) {
                AccessibilityNodeInfo lastClick = list.get(list.size() - 1);
                if (lastClick.getChildCount() > 1) {
                    String talkNameString = "-1";
                    if (lastClick.getChild(0).getText() != null) {
                        talkNameString = lastClick.getChild(0).getText().toString();
                    }
                    String redTitleString = "-1";
                    redTitleString = listRedTitle(lastClick.getChild(1));
                    if (isRedClickBoolean(talkNameString, redTitleString)) {
                        lastClick.getChild(1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
            }
        }
    }

    private String listRedTitle(AccessibilityNodeInfo nodeInfo) {

        if (nodeInfo.getChildCount() > 0) {
            listRedTitle(nodeInfo.getChild(0));
        } else {
            if (nodeInfo.getText() != null) {
                return nodeInfo.getText().toString();
            }
        }
        return "-1";
    }


    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private List<AccessibilityNodeInfo> getWxRedNodeInfo(AccessibilityNodeInfo nodeInfo, String nodes) {
        List<AccessibilityNodeInfo> nodeInfos2 = nodeInfo
                .findAccessibilityNodeInfosByText(nodes);
        return nodeInfos2;
    }

    /**
     * com.tencent.mm:id/b2c  开红包
     */
    private void openWxHb() {
        openWXRedById();
        //获取试图
//        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
//        if (nodeInfo != null) {
//            List<AccessibilityNodeInfo> list = nodeInfo
//                    .findAccessibilityNodeInfosByText("拆红包");
//            for (AccessibilityNodeInfo n : list) {
//                Log.d(LOGTAG, "拆红包");
//                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//            }
//        }

    }

    private void openWXRedById() {
        //获取试图
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo
                    .findAccessibilityNodeInfosByViewId("com.tencent.mm:id/b2c");
            for (AccessibilityNodeInfo n : list) {
                Log.d(LOGTAG, "拆红包");
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                isReceiveRed = true;

                return;
            }
        }
        isReceiveRed = true;
        backWx();
    }


    // 红包详情返回
    private void redDetailBack() {

        //获取试图
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(RedDetailBack);
            for (AccessibilityNodeInfo n : list) {
                Log.d(LOGTAG, "redDetailBack");
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
        if(redType==0){
            if(wakeLock!=null&&wakeLock.isHeld()){
                wakeLock.release();
            }
        }

    }

    //红包布局id
    private void openWxRed() {
        //获取试图
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/yc");//可点击的红包view
            if (list.size() > 0) {
                AccessibilityNodeInfo lastClick = list.get(list.size() - 1);
                if (isRedClick(lastClick)) {
                    lastClick.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        }
    }

    //没抢到点击查看详情
    private void clickHbDetail() {
        //获取试图
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(LookRedDetail);
            for (AccessibilityNodeInfo n : list) {
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    //    抢完红包之后 返回
    private void backWx() {
//            Intent mHomeIntent = new Intent(Intent.ACTION_MAIN);
//            mHomeIntent.addCategory(Intent.CATEGORY_HOME);
//            mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
//                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
//        mContext.startActivity(mHomeIntent);
        //模拟系统返回
        this.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }
}
