package com.example.admin.remarkdemo;

import android.text.TextUtils;
import android.util.Log;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by admin on 2017/12/13.
 */

// 获取手机IMEI和uin，拼接数据库密码
public class CipherUtils {

//    /data/data/com.sollyu.android.appenv.dev/shared_prefs/XPOSED.xml
    public static final String SP_YYBL = "/data/data/com.sollyu.android.appenv.dev";
    public static final String SP_YYBL_PATH = "/data/data/com.sollyu.android.appenv.dev/shared_prefs/XPOSED.xml";
//         <?xml version='1.0' encoding='utf-8' standalone='yes' ?>
//              <map>
//                   <string name="com.tencent.mm">{&quot;buildManufacturer&quot;:&quot;OnePlus&quot;,&quot;buildModel&quot;:&quot;A1001&quot;,&quot;buildSerial&quot;:&quot;wql8sm0tn383&quot;,&quot;empty&quot;:false,&quot;settingsSecureAndroidId&quot;:&quot;6iset1lvocevt3j&quot;,&quot;telephonyGetDeviceId&quot;:&quot;865209034938374&quot;,&quot;telephonyGetLine1Number&quot;:&quot;13700511132&quot;,&quot;telephonyGetNetworkType&quot;:&quot;9&quot;,&quot;telephonyGetSimSerialNumber&quot;:&quot;89154542370167379630&quot;,&quot;wifiInfoGetMacAddress&quot;:&quot;00:1F:5D:AC:5F:1B&quot;,&quot;wifiInfoGetSSID&quot;:&quot;TP-LINK_I7DDPO&quot;}</string>
//              </map>

//    public static void main(String []args) throws JSONException {
//        // 没有root的要先赋予权限
//        execCMD("chmod -R 777 " + SP_YYBL);
//        // 获取包含IMEI的文本
//        String source = getIMEIText();
//
//        JSONObject object = new JSONObject(source);
//        String  s = object.getString("telephonyGetDeviceId");
//
//        System.out.println(s);
//        String source = "{&quot;buildManufacturer&quot;:&quot;OnePlus&quot;,&quot;buildModel&quot;:&quot;A1001&quot;,&quot;buildSerial&quot;:&quot;wql8sm0tn383&quot;,&quot;empty&quot;:false,&quot;settingsSecureAndroidId&quot;:&quot;6iset1lvocevt3j&quot;,&quot;telephonyGetDeviceId&quot;:&quot;865209034938374&quot;,&quot;telephonyGetLine1Number&quot;:&quot;13700511132&quot;,&quot;telephonyGetNetworkType&quot;:&quot;9&quot;,&quot;telephonyGetSimSerialNumber&quot;:&quot;89154542370167379630&quot;,&quot;wifiInfoGetMacAddress&quot;:&quot;00:1F:5D:AC:5F:1B&quot;,&quot;wifiInfoGetSSID&quot;:&quot;TP-LINK_I7DDPO&quot;}";
//        String regex = "telephonyGetDeviceId&quot;:&quot;(\\d+)&quot;";
//        telephonyGetDeviceId&quot;:&quot;865209034938374&quot;
//        System.out.println(getMatcher(regex,source));
//    }

    public static final String WECHAT_PATH = "/data/data/com.tencent.mm/";
    public static final String SP_UIN_PATH = WECHAT_PATH +"shared_prefs/auth_info_key_prefs.xml";





    // 修改权限
    public static void execCMD(String paramString) throws IOException, InterruptedException {

            java.lang.Process process = Runtime.getRuntime().exec("su");
            Object object = process.getOutputStream();
            DataOutputStream dos = new DataOutputStream((OutputStream) object);
            String s = String.valueOf(paramString);
            object = s +"\n";
            dos.writeBytes((String) object);
            dos.flush();
            dos.writeBytes("exit\n");
            dos.flush();
            process.waitFor();
            object = process.exitValue();

    }

    // 获取包含IEMI的文本
    public static String getIMEIText() throws FileNotFoundException, DocumentException {
        File file = new File(SP_YYBL_PATH);

        FileInputStream fis = new FileInputStream(file);
        // 利用dom4j里面的类
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(fis);
        Element root = document.getRootElement();
        List<Element> list = root.elements();

        for (Element element : list){
            if ("com.tencent.mm".equals(element.attributeValue("name"))){
                String currentUin = element.getStringValue();
                Log.i("xyz", "getIMEIText:  "+currentUin);
                return currentUin;
            }
        }

        return "";
    }

//    // 正则表达式获取到IEMI码。
//    public static String getMatcher(String regex, String source){
//        String res = "";
//        Pattern pattern = Pattern.compile(regex);
//        Matcher matcher = pattern.matcher(source);
//        while (matcher.find()){
//            res = matcher.group(1);
//        }
//        return res;
//    }


    // 获取IMEI值
    public static String getIMEI(String source) throws JSONException {
        String res = "";

        JSONObject object = new JSONObject(source);
        res = object.getString("telephonyGetDeviceId");

        return res;
    }

    // 获取当前微信的uin值
    public static String initCurrentUin() throws FileNotFoundException, DocumentException {
        File file = new File(SP_UIN_PATH);
        String currentUin = "";

        FileInputStream fis = new FileInputStream(file);
        // 利用dom4j里面的类
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(fis);
        Element root = document.getRootElement();
        List<Element> list = root.elements();

        for (Element element : list){
            if ("_auth_uin".equals(element.attributeValue("name"))){
                currentUin = element.attributeValue("value");
                Log.i("xyz","currentUin = "+currentUin);
                return currentUin;
            }
        }

        return currentUin;
    }

    // 拼接得到数据库密码
    public static String initDbPassword(String phoneIMEI, String currentUin) {
        if (TextUtils.isEmpty(phoneIMEI)||TextUtils.isEmpty(currentUin)){
            Log.i("xyz","IMEI 和 Uin不能为空");
            return "";
        }
        String content = phoneIMEI + currentUin;
        String password = encrypt(content).substring(0,7).toLowerCase();
        Log.i("xyz","password = "+password);
        return password;
    }

    // MD5 加密
    public static String encrypt(String content) {

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.update(content.getBytes("UTF-8"));
            byte[] encryption = digest.digest();  // 加密
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < encryption.length; i++) {
                if (Integer.toHexString(0xff & encryption[i]).length() == 1){
                    sb.append("0").append(Integer.toHexString(0xff & encryption[i]));
                }else {
                    sb.append(Integer.toHexString(0xff & encryption[i]));
                }
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }


}
