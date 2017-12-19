package com.example.admin.remarkdemo;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import android.widget.Toast;

import net.sqlcipher.database.SQLiteDatabaseHook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


import net.sqlcipher.database.SQLiteDatabase;


import org.dom4j.DocumentException;
import org.json.JSONException;

import static com.example.admin.remarkdemo.Constant.*;
import static com.example.admin.remarkdemo.MyService.getContext;
import static com.example.admin.remarkdemo.CipherUtils.*;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "remarkdemo.MainActivity";

    private static final String DB_DIR_PATH = WECHAT_PATH + "MicroMsg";  // 数据库的文件夹目录
    private static final String DB_FILE_NAME = "EnMicroMsg.db";           // 数据库名

    private static final String COPY_DB_NAME = "wx_data.db";



    private AccessibilityService mService;

    ClipboardManager manager;

//    private AtomicInteger mIsFinishCreateDatabase = new AtomicInteger(0);

    private Handler mHandler;

    private TextView mMsgHintTV; //  信息提示
    private TextView mNumTV;     //  当前修改当前数量
    private TextView mSumTV;     //  需要修改总数量

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMsgHintTV = findViewById(R.id.hint);
        mNumTV = findViewById(R.id.num);
        mSumTV = findViewById(R.id.sum);



        manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);



        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                switch (msg.what){
                    case 1:
                        // ui总的数量
                        int s = msg.arg1;
                        mSumTV.setText(""+s);
                        break;

                    case 2:
                        // 当前修改的数量
                        int num = msg.arg1;
                        int sum = msg.arg2;
                        Toast.makeText(MainActivity.this,"当前已修改 "+num+"/"+sum,Toast.LENGTH_SHORT).show();
                        mNumTV.setText(""+num);
                        break;

                    case 3:
                        mMsgHintTV.setText("任务已完成");
                        break;

                    case 4:
                        String error = (String) msg.obj;
                        mMsgHintTV.setText(error);
                        break;
                }
            }
        };







        Bundle bundle = getIntent().getExtras();
        if (bundle!=null){
            String s = bundle.getString("key");
            if (s.equals("service_start")){
                Log.i("xyz","bundle传入正确");

                autoDoTask();


            }
        }


    }

//    private void waitForCreate(long time) throws MyTimeoutException {
//        long before = SystemClock.currentThreadTimeMillis();
//        do {
//            long after = SystemClock.currentThreadTimeMillis();
//            if (after - before >= time){
//                throw new MyTimeoutException("等待创建数据库超时");
//            }
//            SystemClock.sleep(200);
//
//        }while (mIsFinishCreateDatabase.get() == 0);
//    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void findEditAndInputInfo(String conRemark) throws MyTimeoutException {
        AccessibilityNodeInfo editInfo = null;
        do {
            AccessibilityNodeInfo root = getRoot();
            if (root!=null){
                editInfo = findEditText(root);
            }
            SystemClock.sleep(200);
        }while (editInfo == null);

        long aa = System.currentTimeMillis();
        do {
            long bb =  System.currentTimeMillis();
            if (bb - aa >= 13000) {
                Log.i(TAG, "findEditAndInputInfo: 输入昵称失败 ");
                throw new MyTimeoutException("输入昵称失败");
            }
            // 填入昵称
            ClipData data = ClipData.newPlainText("text",conRemark);
            if (manager == null){
                Log.i(TAG,"manager == null");
            }
            if (data == null){
                Log.i(TAG,"data == null");
            }
            if (manager!=null && data!=null){
                manager.setPrimaryClip(data);
                editInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            }
            sleepRandom();
        }while (editInfo.getText() == null);

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private AccessibilityNodeInfo findEditTextSuccess(){
        AccessibilityNodeInfo editInfo = null;
        do {
            AccessibilityNodeInfo root = getRoot();
            if (root!=null){
                editInfo = findEditText(root);
            }
            SystemClock.sleep(200);
        }while (editInfo == null);

        return editInfo;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private AccessibilityNodeInfo findEditText(AccessibilityNodeInfo root) {
        if (root == null){
            return null;
        }
        AccessibilityNodeInfo res = null;
        List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/hb");
        if (list.size()<=0){
            Log.i(TAG, "findEditText: 找不到搜索框");
        }else {
            res = list.get(0);
        }
        return res;
    }


    private void sleepRandom() {
        double ran = Math.random();
        long lon = (long) (1000 + ran *200);
        SystemClock.sleep(lon);
    }


    private void initLocalDatabase() throws IOException, JSONException, DocumentException, InterruptedException, MyTimeoutException {
        // 没有root的要先赋予权限
        execCMD("chmod -R 777 " + SP_YYBL);
        execCMD("chmod -R 777 " + WECHAT_PATH);


        // 1、获取IEMI值
        // 获取包含IMEI的文本
        String source = getIMEIText();
        String phoneIMEI  = getIMEI(source);

        Log.i(TAG, "onCreate: source == "+ source);
        Log.i(TAG, "onCreate: res == "+ phoneIMEI);

        // 2、获取当前登录微信账号的uin(存储在sp里面)/data/data/com.tencent.mm/shared_prefs/auth_info_key_prefs.xml 里面的name为_auth_uin的值
        String currentUin = initCurrentUin();

        // 3、拼接、加密并获取到密码
        String password = initDbPassword(phoneIMEI,currentUin);
        Log.i(TAG, "onCreate: password == "+ password);


//        textView.setText(password);

        // 如果在手机上登陆多个微信的话，在MicroMsg中的多个文价夹下有EnMicroMsg.db，而我们需要找出的是
        // 当前微信号所对应的文件夹，其实这个文件夹的命名就是 MD5("mm"+auth_info_key_prefs.xml中解析出微信的uin码)
        // 所以直接在计算出数据库的详细路径就可以了

        // 4、获取EnMicroMsg.db的详细路径
        String db_path = DB_DIR_PATH+"/"+encrypt("mm"+currentUin)+"/"+DB_FILE_NAME;

        // 5、将微信数据库复制到本地应用中（直接连接会导致微信客户端退出并出现异常）

        String mCurrentApkPath = "/data/data/"+getApplication().getPackageName();
        String localPath = mCurrentApkPath+"/"+COPY_DB_NAME;
        copyFile(db_path,localPath);

        // 6、查询数据库中的rconversation表
        File dbFile = new File(localPath);
        //  查询数据库，将得到的信息放入自己的数据库中
        linkAndOpenDataBase(dbFile, password);
    }

    private void openSearch() {
        //  intent = Intent { cmp=com.tencent.mm/.plugin.search.ui.FTSMainUI (has extras) }
        //  bundle = Bundle[mParcelledData.dataSize=220]
        String s =  "adb shell am start --activity-clear-top com.tencent.mm/com.tencent.mm.plugin.search.ui.FTSMainUI "+
                "--ei mParcelledData.dataSize 220"+
                "";
        try {
            Runtime.getRuntime().exec(s);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        Intent intent = new Intent();
//        intent.setClassName("com.tencent.mm", "com.tencent.mm.search.ui.FTSMainUI");
//        intent.putExtra("mParcelledData.dataSize", 220);
//        startActivity(intent);

    }

    private void autoDoTask() {

        // TODO 放子线程
        new Thread(new Runnable() {
            @Override
            public void run() {


                while (true){
                    try {
                        // TODO 持久化
                        // 每次只能执行一次，因为数据库中wxid的原因，unique
//                        if (mIsFinishCreateDatabase.get() == 0){
                        if (!sqlTableIsExist(DATABASE_NAME,TABLE_NAME)){
                            initLocalDatabase();
//                            mIsFinishCreateDatabase.getAndIncrement();
                        }


                        // 任务完成。打破循环
                        int i = doTask();
                        if (1 == i){
                            break;
                        }


                    }catch (Exception e){
                        e.printStackTrace();
                        String error = e.getMessage();
                        Message msg4 = Message.obtain();
                        msg4.what = 4;
                        msg4.obj = error;
                        mHandler.sendMessage(msg4);
//                        killWechat();
//                        sleepMinRandom(1,2);
                        sleepSecondRandom(10,20);

                    }

                }



            }
        }
        ).start();



    }


    // 睡眠秒数。区间段
    private void sleepSecondRandom(long startSecond,long endSecond) {
        double ran = Math.random();
        long interval = endSecond - startSecond;
        long time = (long) (startSecond  * 1000 + ran * 1000 * interval );
        SystemClock.sleep(time);
    }

    private void killWechat() {
        // 异常时杀死微信，这样就回到主界面了。这样做还有一个好处，保证下次任务开始时微信一定在主界面，从而继续执行任务
        String s = "adb shell am force-stop com.tencent.mm ";
        try {
            Runtime.getRuntime().exec(s);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }


    // 睡眠分钟数。区间段
    private void sleepMinRandom(long startMin,long endMin) {
        double ran = Math.random();
        long interval = endMin - startMin;
        long lon = (long) (startMin * 60 * 1000 + ran * 1000 * interval * 60);
        SystemClock.sleep(lon);
    }

    private boolean sqlTableIsExist(String dbName,String tableName) {
        boolean result = false;
        if (tableName == null) {
            return false;
        }
        android.database.sqlite.SQLiteDatabase db = null;
        Cursor cursor = null;
        try {

            db = openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null);
            String sql = "select count(*) as c from Sqlite_master  where type ='table' and name ='" + tableName.trim() + "' ";
            cursor = db.rawQuery(sql, null);
            if (cursor.moveToNext()) {
                int count = cursor.getInt(0);
                Log.i(TAG, "sqlTableIsExist: count = "+count);
                if (count > 0) {
                    result = true;
                }
            }

        } catch (Exception e) {

        }
        Log.i(TAG, "sqlTableIsExist: 数据库是否存在 res = "+result);
        return result;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private int doTask() throws MyTimeoutException {
//        // 点击跳转到通讯录
//        findAndClickContact();
//        waitFor(20000);

        MyDatabaseHelper helper = new MyDatabaseHelper(this);
        android.database.sqlite.SQLiteDatabase sqLiteDatabase = helper.getReadableDatabase();

        Cursor cursor = sqLiteDatabase.rawQuery("select * from "+TABLE_NAME + " where status = 0",null);
        List<Person> list  = DbManager.cursorToPerson(cursor);

        for (int i = 0; i < list.size(); i++) {
            Person person = list.get(i);
            String nickname = person.getNickname();
            String time = person.getTime();
            String wxid = person.getWxid();

            int sum = list.size();
            Message msg1 = Message.obtain();
            msg1.what = 1;
            msg1.arg1 = sum;
            mHandler.sendMessage(msg1);

            setCnt(0);
            sleepRandom();
            // 7、打开微信本地搜索按钮
            openSearch();

            waitFor(20000);
            if (getCnt() == 1){
                setCnt(0);

//                String s = conRemark.substring(0,4);


                findEditAndInputInfo(nickname);

                sleepRandom();

                // 找到相应的人
                AccessibilityNodeInfo friendInfo = findFriendSuccess();
                if (friendInfo!=null){
                    clickUtilSuccess(friendInfo);
                }else {
                    Log.i(TAG, "doTask: 搜索不到 "+nickname+" ，将跳过ta");
                    sqLiteDatabase.execSQL( "update "+TABLE_NAME +" set status = 2 where "+WXID+" = '"+wxid+"'");
                    throw  new RuntimeException("搜索不到 "+nickname+" ，将跳过ta");
                }

                sleepRandom();

                // 找到右上角的个人信息按钮
                AccessibilityNodeInfo personInfo = findPersonSuccess();
                clickUtilSuccess(personInfo);
                sleepRandom();


                // 聊天信息界面。也就是个人资料那里
                // 点击头像
                AccessibilityNodeInfo avatarInfo = findAvatarSuccess();
                clickUtilSuccess(avatarInfo);
                sleepRandom();


                // 点击设置备注
                AccessibilityNodeInfo changeInfo = findChangeRemark();
                clickUtilSuccess(changeInfo);
                sleepRandom();

                // 找到备注名的输入框并修改
                findAndChangeRemark(nickname,time);
                sleepRandom();
                // 点击确定按钮
                clickConfirm();
                sleepRandom();

                // 更新数据库中的状态，表示修改成功
                updateDatabaseStatus(sqLiteDatabase,wxid);

//
//                                // 返回到主界面
//                                do {
//                                    while (hasReturn()){
//                                        finishAndReturn();
//                                        SystemClock.sleep(300);
//                                    }
//                                }while (!startFinish());








            }
            int j = i +1;
            Message msg2 = Message.obtain();
            msg2.what = 2;
            msg2.arg1 = j;
            msg2.arg2 = list.size();
            mHandler.sendMessage(msg2);


        }

        if (cursor!=null){
            cursor.close();
            cursor = null;
        }

        Message msg3 = Message.obtain();
        msg3.what = 3;
        mHandler.sendMessage(msg3);

        return 1;




    }

    private void updateDatabaseStatus(android.database.sqlite.SQLiteDatabase sqLiteDatabase, String wxid) {
        String s = "update "+TABLE_NAME +" set status = 1 where "+WXID+" = '"+wxid+"'";
        sqLiteDatabase.execSQL(s);
    }

//    AccessibilityNodeInfo returnInfo;
//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//    private boolean hasReturn() {
//        Log.i("xyz","开始查找返回键");
//        int i = 0;
//        do {
//            // 找到左上角的返回键
//            AccessibilityNodeInfo root = getRoot();
//            returnInfo = findReturn(root);
//            SystemClock.sleep(200);
//            i++;
//        }while (i<5);
//
//        if (returnInfo == null){
//            Log.i("xyz","找到的返回为null");
//            return false;
//        }else {
//            Log.i("xyz","找到的返回不为null");
//            return true;
//        }
//
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//    private void finishAndReturn(){
//
//        Log.i("xyz","开始查找返回键");
//        do {
//            // 找到左上角的返回键
//            AccessibilityNodeInfo root = getRoot();
//            returnInfo = findReturn(root);
//            SystemClock.sleep(200);
//        }while (returnInfo == null);
//
//
//        if (returnInfo == null){
//            Log.i("xyz","找到的返回为null");
//        }else {
//            Log.i("xyz","找到的返回不为null");
//            while (returnInfo!=null && !returnInfo.isClickable()) {
//                returnInfo = returnInfo.getParent();
//            }
//            // 点击返回
//            returnInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//        }
//    }
//
//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
//    private AccessibilityNodeInfo findReturn(AccessibilityNodeInfo root) {
//
//
//        root = getRoot();
//        AccessibilityNodeInfo res = null;
//        if (root !=null){
//            List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/hg");
//            List<AccessibilityNodeInfo> list1 = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/h1");
//            if (list.size() > 0){
//                res = list.get(0);
//            }else if (list1.size() >0){
//                res = list1.get(0);
//            }else {
//                Log.i(TAG, "findReturn: 找不到返回键");
//            }
//        }
//
//
//        return res;
//
////        AccessibilityNodeInfo res = null;
////        for (int i = 0; i < root.getChildCount(); i++) {
////            AccessibilityNodeInfo nodeInfo = root.getChild(i);
////            if (nodeInfo != null&&nodeInfo.getClassName().equals("android.widget.ImageView") ) {
////                Log.i("fanhui","获取到ImageView");
////                Log.i("fanhui","nodeInfo = "+nodeInfo);
////                Rect rect = new Rect();
////                nodeInfo.getBoundsInScreen(rect);
////                int x = rect.centerX();
////                int y = rect.centerY();
////                Log.i("fanhui","x = "+ x+ " y = "+y);
////                if (5 < x && x < 35 && 13 < y && y < 43) {
////                    res =  nodeInfo;
////                    Log.i("fanhui","找到返回键");
////                    Log.i("fanhui","找到返回键的坐标 x = "+ x+ " y = "+y);
////                    break; // 这里必须有这个break，表示找到返回键之后就会打破循环，将找到的值返回
////                }
////            }else {
////                res = findReturn(nodeInfo);
////                if (res != null){
////                    return res;
////                }
////            }
////        }
////        return res;
//    }




    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean startFinish()  {
        List<AccessibilityNodeInfo> list;
        long aa = System.currentTimeMillis();
        do {
            AccessibilityNodeInfo root = getRoot();
            long bb =  System.currentTimeMillis();
            if (bb - aa >= 13000){
                Log.e("xyz","sss");
            }

            list = root.findAccessibilityNodeInfosByText("微信");
            SystemClock.sleep(500);
        }while (list == null || list.size() == 0);

        if (list.size() > 0){
            Log.i("xyz","微信启动完成");
            return true;
        }else {
            return false;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void clickConfirm() {
        AccessibilityNodeInfo root = getRoot();
        List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/gy");
        if (list.size() >0){
            for (AccessibilityNodeInfo info : list){
                info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    private void findAndChangeRemark(String conRemark,String time) {
        AccessibilityNodeInfo editRemarkInfo;
        // 找到备注编辑框
        editRemarkInfo = findEditRemarkText();
        // 点击
        editRemarkInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        // 清理原先的文本
        do {
            clearAllText(editRemarkInfo);
            editRemarkInfo = findEditRemarkText();
        }while (editRemarkInfo!=null && !TextUtils.isEmpty(editRemarkInfo.getText()) && editRemarkInfo.getText().length()!=0);

        // 修改备注
        String text = conRemark + time;
        ClipData data = ClipData.newPlainText("text",text);

        manager.setPrimaryClip(data);

        while (editRemarkInfo == null){
            editRemarkInfo = findEditRemarkText2();
        }

        editRemarkInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);


    }

    private void clearAllText(AccessibilityNodeInfo info) {
        // 每次只能删除一个字符
        String adb = "adb shell input keyevent 67";

        int length = info.getText().length();
        Log.i(TAG,"原先文本长度 "+ length);
        for (int i = 0; i < length; i++) {
            try {

                Runtime.getRuntime().exec(adb);
                Log.i(TAG,"执行一次");
                SystemClock.sleep(300); // 睡眠是为了保证能够全部清除
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    // 点击之前为TextView，id为aje
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private AccessibilityNodeInfo findEditRemarkText() {

        AccessibilityNodeInfo res = null;
        AccessibilityNodeInfo root = getRoot();

        List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/aje");
        if (list.size() >0){
            res = list.get(0);
        }else {
            Log.i(TAG, "findEditRemarkText: 找不到修改备注的编辑框");
        }
        return res;

    }

    // 点击之后为EditText，id为ajd
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private AccessibilityNodeInfo findEditRemarkText2() {

        AccessibilityNodeInfo res = null;
        AccessibilityNodeInfo root = getRoot();

        List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ajd");
        if (list.size() >0){
            res = list.get(0);
        }else {
            Log.i(TAG, "findEditRemarkText: 找不到修改备注的编辑框2");
        }
        return res;

    }
//
//    private AccessibilityNodeInfo findChangeRemarkSuccess(){
//        AccessibilityNodeInfo res = null;
//    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private AccessibilityNodeInfo findChangeRemark() {
        AccessibilityNodeInfo res = null;
        AccessibilityNodeInfo root = getRoot();
        List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/aib");
//        if (list.size() > 0){
//            for (AccessibilityNodeInfo info : list){
//                while (!info.isClickable()){
//                    info = info.getParent();
//                }
//                res = info;
//                break;
////                info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//            }
//        }
        if (list.size()>0){
            AccessibilityNodeInfo info = list.get(0);
            while (!info.isClickable()){
                info = info.getParent();
            }
            res = info;
//            info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }else {
            Log.i(TAG, "clickChangeRemark: 点击修改备注失败");
        }
        return res;
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private AccessibilityNodeInfo findAvatarSuccess() {

        AccessibilityNodeInfo res= null;
        AccessibilityNodeInfo root = getRoot();
        List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/cl5");
//        if (list.size() > 0){
//            for (AccessibilityNodeInfo info : list){
//                while (!info.isClickable()){
//                    info = info.getParent();
//                }
//                res = info;
//                break;
//            }
//        }
        if (list.size()>0){
            AccessibilityNodeInfo info = list.get(0);
            while (!info.isClickable()){
                info = info.getParent();
            }
            res = info;
//            info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }else {
            Log.i(TAG, "findAndClickAvatar: 找不到头像");
        }
        return res;

    }


    private void clickUtilSuccess(AccessibilityNodeInfo info) throws MyTimeoutException {
        Log.i(TAG, "clickUtilSuccess: info = "+info.toString());
        int failnum=0;
        boolean bok=false;
        sleepRandom();
        do {
            setCnt(0);
            info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            try {
                waitFor(10005);
                bok=true;
            } catch (MyTimeoutException e) {
                failnum++;
            }
            if(bok)
                break;
            if(failnum>=3){
                throw new MyTimeoutException("重试次数大于3");
            }
        }while (true);

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private AccessibilityNodeInfo findPersonSuccess() {
        AccessibilityNodeInfo personInfo = null;
        do {
            AccessibilityNodeInfo root = getRoot();
            if (root!=null){
                personInfo = findPerson(root);
            }
            SystemClock.sleep(200);
        }while (personInfo == null);

        return personInfo;
    }




    private AccessibilityNodeInfo findPerson(AccessibilityNodeInfo root) {
        if (root == null){
            return null;
        }
        AccessibilityNodeInfo res = null;

        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo nodeInfo = root.getChild(i);
            //&& nodeInfo.getContentDescription().equals("聊天信息")
            if (nodeInfo!=null && nodeInfo.getClassName().equals("android.widget.TextView") ) {
                Rect rect = new Rect();
                nodeInfo.getBoundsInScreen(rect);
                int x = rect.centerX();
                int y = rect.centerY();
                Log.i("fanhui","x = "+ x+ " y = "+y);
                if ( 345< x && x < 380 && 15 < y && y < 50) {
                    res =  nodeInfo;
                    Log.i(TAG, "findPerson: 找到个人信息");
                    Log.i(TAG,"找到个人信息的坐标 x = "+ x+ " y = "+y);
                    break; // 这里必须有这个break，表示找到之后就会打破循环，将找到的值返回
                }
//                res =  nodeInfo;
//                break; // 这里必须有这个break，表示找到输入框之后就会打破循环，将找到的值返回

            }else {

                res = findPerson(nodeInfo);
                if (res != null){
                    return res;
                }


            }
        }
        return res;

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private AccessibilityNodeInfo findFriendSuccess() {
        AccessibilityNodeInfo res = null;
        AccessibilityNodeInfo root = getRoot();
        List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/jy");
        if (list.size()>0){
            AccessibilityNodeInfo info = list.get(0);
            while (!info.isClickable()){
                info = info.getParent();
            }
            res = info;
//            info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }else {
            Log.i(TAG, "findAndClickFriend: 找不到对应的好友");
        }
        return res;
    }

    // 线程睡眠，让出cpu
    public void waitFor(long overTime) throws MyTimeoutException {
        long before = System.currentTimeMillis();
        do{
            long now = System.currentTimeMillis();
            if (now - before >= overTime){
                Log.i("xyz","等待超时");
                throw new MyTimeoutException("等待辅助类方法超时 "+overTime);
            }
            SystemClock.sleep(300);
        }while (getCnt() == 0);
    }

    private int getCnt(){
        int i;
        i = MyService.cnt.get();
        Log.i(TAG,"get cnt = "+ MyService.cnt);
        return i;
    }

    private void setCnt(int i){
        MyService.cnt.set(i);
        Log.i(TAG,"set cnt = "+ MyService.cnt);

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void findAndClickContact() {
        AccessibilityNodeInfo root = getRoot();

        if (root!=null){
            List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByText("通讯录");

            if ( list.size() <= 0){
                Log.i(TAG, "findContact: 找不到通讯录");
            }else {
                for (AccessibilityNodeInfo info : list){
                    while (!info.isClickable()) {
                        info = info.getParent();
                    }
                    info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Log.i(TAG, "findContact: 点击了通讯录");
                }
            }
        }
    }


    // 每次都等待200ms后获取root根节点信息
    @android.support.annotation.RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private AccessibilityNodeInfo getRoot() {
        mService = (AccessibilityService) getContext();
        AccessibilityNodeInfo root;
        do {
            root = mService.getRootInActiveWindow();
            SystemClock.sleep(200);
        }while (root==null);
        root.refresh();
        return root;
    }


    // 一连就手动打不开了,但是代码却可以访问到
    private void linkAndOpenDataBase(File dbFile,String password) throws MyTimeoutException {
        // 这个SQLiteDatabase记得导入的是sqlcipher的类
        SQLiteDatabase.loadLibs(this);

        SQLiteDatabaseHook hook = new SQLiteDatabaseHook() {
            @Override
            public void preKey(net.sqlcipher.database.SQLiteDatabase sqLiteDatabase) {

            }

            @Override
            public void postKey(net.sqlcipher.database.SQLiteDatabase sqLiteDatabase) {
                sqLiteDatabase.rawExecSQL("PRAGMA cipher_migrate;"); //兼容2.0的数据库  
            }

        };

        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, password, null,hook);


//        Cursor cursor = db.rawQuery("select * from rconversation where msgType = 1 ",null);
//        Log.i("xyz","count == "+  cursor.getCount());
//        textView.setText(cursor.getCount()+" ");


        //cursorInRContact
        Cursor cursorInRContact = db.rawQuery("select * from rcontact where type = 3 and verifyFlag = 0",null);
        List<String> nickList = new ArrayList<>();
        List<String> wxidList = new ArrayList<>();
        List<String> timeList = new ArrayList<>();
        while (cursorInRContact.moveToNext()){


            String conRemark = cursorInRContact.getString(cursorInRContact.getColumnIndex("conRemark"));
            if ((conRemark != null) && (!conRemark.equals("")) && (isContains(conRemark,"2017."))){
                continue; // 如果备注有2017的话，说明已经修改过了，直接跳过
            }

            // 需求变了，所有好友。所以拿的是nickname字段
            String nickname = cursorInRContact.getString(cursorInRContact.getColumnIndex("nickname"));

//            if ((conRemark !=null) &&  (!conRemark.equals("")) && conRemark.length() >=4 && (conRemark.substring(3,4).equals("_")) && !isContains(conRemark,"2017.")){
            if ((nickname !=null) &&  (!nickname.equals("")) ){



                String wxid = cursorInRContact.getString(cursorInRContact.getColumnIndex("username"));

                Cursor cursorInRConversation = db.rawQuery("select * from rconversation where username = '"+wxid+"'",null);

                String time = "";
                if (cursorInRConversation.moveToFirst()){
                     time = cursorInRConversation.getString(cursorInRConversation.getColumnIndex("conversationTime"));

                }else {
//                //     好友刚好是第四个下划线，但是没聊过天。所以没有时间记录
                    Log.i(TAG, "linkAndOpenDataBase: 出事了  "+wxid+" nickname "+nickname);
                    // 没有聊天记录，默认也改了，不管是不是有下划线的
                    time = "1510353204638"; // 2017.11.11

//                    throw new MyTimeoutException("linkAndOpenDataBase 出事了"+"wxid "+wxid+ " conRemark "+conRemark );
                }

                Log.i(TAG, "linkAndOpenDataBase: nickname = "+nickname);
                Log.i(TAG, "linkAndOpenDataBase: wxid = "+wxid);
                Log.i(TAG, "linkAndOpenDataBase: time = " +time);

                nickList.add(nickname);
                wxidList.add(wxid);
                timeList.add(time);

                if (cursorInRConversation !=null){
                    cursorInRConversation.close();// 记得close游标，不然下次就会报错了
                }


            }
        }
        if (cursorInRContact !=null){
            cursorInRContact.close();// 记得close游标，不然下次就会报错了
        }


//
//        for (String  s: wxidList){
//
//        }



        // 本地数据库
        MyDatabaseHelper helper = new MyDatabaseHelper(this);
        android.database.sqlite.SQLiteDatabase sqLiteDatabase = helper.getReadableDatabase();


        sqLiteDatabase.beginTransaction();
        try{

            for (int i = 0; i < nickList.size(); i++) {
                String nickname = nickList.get(i);
                String wxid = wxidList.get(i);
                String time = timeList.get(i);
                time = TimeTrans.stampToDate(time);
//                 字段处理
                nickname = nickname.replaceAll("\r|\n","");
                nickname = nickname.replaceAll("'","");
                sqliteEscape(nickname); // 主要是 ' 的问题


                String insert = "insert into " + TABLE_NAME + " values('" + wxid + "','" + nickname + "','" + time + "',0)";
//                ContentValues values = new ContentValues();
//                values.put(WXID,wxid);
//                values.put(CONREMARK,conRemark);
//                values.put(TIME,time);
//                values.put(STATUS,0);
//                sqLiteDatabase.insert(TABLE_NAME,"time",values);

                sqLiteDatabase.execSQL(insert);

            }
            sqLiteDatabase.setTransactionSuccessful();
        }finally {
            sqLiteDatabase.endTransaction();
            if (sqLiteDatabase!=null){
                sqLiteDatabase.close();
            }
        }



        if (db != null){
            db.close();
        }


    }


    private String sqliteEscape(String s) {
        s = s.replace("/","//");
        s = s.replace("'","''");
        s = s.replace("[","/[");
        s = s.replace("]","/]");
        s = s.replace("%","/%");
        s = s.replace("&","/&");
        s = s.replace("_","/_");
        s = s.replace("(","/(");
        s = s.replace(")","/)");
        return s;

    }


    private boolean isContains(String conRemark, String target) {
        if (conRemark.indexOf(target)!=-1){
            return true;
        }else {
            return false;
        }

    }


    private void copyFile(String oldPath, String newPath) throws IOException {
        Log.i("xyz","oldPath = "+oldPath);
        Log.i("xyz","newPath = "+newPath);

        File file = new File(oldPath);
        if (file.exists()){

                FileInputStream fis = new FileInputStream(oldPath);
                FileOutputStream fos = new FileOutputStream(newPath);
                byte [] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer))!= -1){
                    fos.write(buffer,0,len);
                }
                fis.close();

        }
    }



}
