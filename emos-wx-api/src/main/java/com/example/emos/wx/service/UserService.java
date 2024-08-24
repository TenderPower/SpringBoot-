package com.example.emos.wx.service;

import com.example.emos.wx.db.pojo.TbUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


public interface UserService {

    public int registerUser(String registerCode, String code, String nickname, String photo);

    public Set<String> searchUserPermissions(int userId);

    public Integer login(String code);

//    通过id获取用户信息
    public TbUser searchById(int userId);

//    获取入职日期
    public String searchUserHiredate(int userId);

//    根据id获取用户基本信息
    public HashMap searchUserSummary(int userId);

    public ArrayList<HashMap> searchUserGroupByDept(String keyword);

    public int insertUser(HashMap parm);

    public ArrayList<HashMap> searchMembers(List param);
}
