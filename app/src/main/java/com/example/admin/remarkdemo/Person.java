package com.example.admin.remarkdemo;

/**
 * Created by admin on 2017/12/14.
 */

public class Person {

    private String  wxid;
    private String nickname;
    private String time;
    private int status;

    public Person(String wxid, String nickname, String time , int status) {
        this.wxid = wxid;
        this.nickname = nickname;
        this.time = time;
        this.status = status;
    }

    @Override
    public String toString() {
        return "wxid="+this.wxid+",conRemark="+this.nickname+",time="+this.time+",status="+this.status;
    }

    public String getWxid() {
        return wxid;
    }

    public void setWxid(String wxid) {
        this.wxid = wxid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }


}
