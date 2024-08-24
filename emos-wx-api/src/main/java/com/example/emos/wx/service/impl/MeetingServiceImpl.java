package com.example.emos.wx.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.emos.wx.db.dao.TbMeetingDao;
import com.example.emos.wx.db.dao.TbUserDao;
import com.example.emos.wx.db.pojo.TbMeeting;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.MeetingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;

@Slf4j
@Service
public class MeetingServiceImpl implements MeetingService {
    @Autowired
    private TbMeetingDao tbMeetingDao;

    @Autowired
    private TbUserDao userDao;

    @Value("${emos.code}")
    private String code;

    @Value("${workflow.url}")
    private String workflow;

    @Value("${emos.recieveNotify}")
    private String recieveNotify;


    @Override
    public void insertMeeting(TbMeeting entity) {
//        保存数据
        int row = tbMeetingDao.insertMeeting(entity);
        if(row!=1){
            throw new EmosException("会议添加失败");
        }
//        TODO 开启审批工作流
//        假装以及会议审批通过
//        startMeetingWorkflow(entity.getUuid(),entity.getCreatorId().intValue(),entity.getDate(),entity.getStart());
    }

    @Override
    public ArrayList<HashMap> searchMyMeetingListByPage(HashMap param) {
        ArrayList<HashMap> list = tbMeetingDao.searchMyMeetingListByPage(param);
        String temp = null;
        ArrayList resultList = new ArrayList();
        HashMap resultMap = null;
        JSONArray array  = null;
        for (HashMap map : list){
            String date = map.get("date").toString();
            if(!date.equals(temp)){
//                说明是新的日期下的会议(毕竟一个日期下当天有多个会议)
                temp = date;
                resultMap = new HashMap();
                resultList.add(resultMap);
                array = new JSONArray();
                resultMap.put("date", temp);
                resultMap.put("list", array);
            }
            array.put(map);

        }
        return resultList;
    }

    @Override
    public HashMap searchMeetingById(int id) {
//        查询会议的基本信息
        HashMap map = tbMeetingDao.searchMeetingById(id);
        //        查询会议的参会人
        ArrayList<HashMap> list = tbMeetingDao.searchMeetingMembers(id);
        map.put("members", list);
        return map;
    }

    @Override
    public void updateMeetingInfo(HashMap param) {
//        取出信息
        int id = (int) param.get("id");
        String date = param.get("date").toString();
        String start = param.get("start").toString();
        String instanceId = param.get("instanceId").toString();
//        查询已有的会议信息
        HashMap oldMeeting = tbMeetingDao.searchMeetingById(id);
//      获取已有会议信息的uuid
        String uuid = oldMeeting.get("uuid").toString();
//        获取已有会议信息的creatorId
        Integer creatorId = Integer.parseInt(oldMeeting.get("creatorId").toString());
//        修改会议记录
        int row = tbMeetingDao.updateMeetingInfo(param);
        if (row != 1) {
            throw new EmosException("会议更新失败");
        }
        System.out.println("会议更新成功");
        JSONObject json = new JSONObject();
        json.set("instanceId", instanceId);
        json.set("reason", "会议被修改");
        json.set("uuid", uuid);
        json.set("code", code);
        String url = workflow + "/workflow/deleteProcessById";
//        发出请求，得到响应
        HttpResponse resp = HttpRequest.post(url).header("content-type", "application/json")
                .body(json.toString()).execute();
        if (resp.getStatus() != 200) {
            log.error("删除工作流失败");
            throw new EmosException("删除工作流失败");
        }
//        创建新的会议工作流
        startMeetingWorkflow(uuid, creatorId, date, start);
    }


//    @Override
    public void deleteMeetingById(int id) {
//            先查询已有会议记录
        HashMap meeting = tbMeetingDao.searchMeetingById(id);
//        提取该会议的uuid
        String uuid = meeting.get("uuid").toString();
        String instanceId = meeting.get("instanceId").toString();
        DateTime date = DateUtil.parse(meeting.get("date") + " " + meeting.get("start"));
        DateTime now = DateUtil.date();
//        会议开始前20分钟，不能删除会议  直接删除吧，不设置会议开始前20分钟了
//        if (now.isAfterOrEquals(date.offset(DateField.MINUTE, -20))) {
//            throw new EmosException("距离会议开始不足20分钟，不能删除会议");
//        }
//        否则删除
        int row = tbMeetingDao.deleteMeetingById(id);
        if (row != 1) {
            throw new EmosException("会议删除失败");
        }
//        更改工作流 假设已经修改了工作流
//        JSONObject json = new JSONObject();
//        json.set("instanceId", instanceId);
//        json.set("reason", "会议被修改");
//        json.set("uuid", uuid);
//        json.set("code", code);
//        String url = workflow + "/workflow/deleteProcessById";
//        HttpResponse resp = HttpRequest.post(url).header("content-type", "application/json")
//                .body(json.toString()).execute();
//        if (resp.getStatus() != 200) {
//            log.error("删除工作流失败");
//            throw new EmosException("删除工作流失败");
//        }
    }


//    每个员工只能看到自己参与的会议记录，自己不参与的会议是看不到的。而且会议必须是未开始
//状态，或者进行中状态才能被看到，已经结束的会议、审批中的会议、审批不通过的会议都是无
//法被看到的


//    需要第三方软件 生成instance——id 来绑定会议的uuid
    private void startMeetingWorkflow(String uuid, int creatorId, String date, String start) {
//        先查询出用户的基本 信息
        HashMap info = userDao.searchUserInfo(creatorId);
        JSONObject json = new JSONObject();
//        放一些需要提交的参数
        json.set("url", recieveNotify);
        json.set("uuid", uuid);
        json.set("openId", info.get("openId"));
        json.set("code", code);
        json.set("date", date);
        json.set("start", start);
//        把字符串的内容切分成 数组
        String[] roles = info.get("roles").toString().split("，");
//        判断在这个数组里面 是否包含总经理这个角色
        if (!ArrayUtil.contains(roles, "总经理")) {//不是总经理
//            查询部门经理的id
            Integer managerId = userDao.searchDeptManagerId(creatorId);
            json.set("managerId", managerId);
//            以及查询总经理的id
            Integer gmId = userDao.searchGmId();
            json.set("gmId", gmId);
//            查询参会人是否同一个部门的
            boolean bool = tbMeetingDao.searchMeetingMembersInSameDept(uuid);
            json.set("sameDept", bool);
        }
//        上述完成了提交数据的参数请求
//        完成url
        String url = workflow + "/workflow/startMeetingProcess";
//        向服务端docker 发起服务器请求
        HttpResponse resp = HttpRequest.post(url).header("Content-Type", "application/json")
                .body(json.toString()).execute();
//        判断响应的状态码
        if (resp.getStatus() == 200) {
            json = JSONUtil.parseObj(resp.body());//解析 把字符串转为JSON对象
            String instanceId = json.getStr("instanceId");
            HashMap param = new HashMap();
            param.put("uuid", uuid);
            param.put("instanceId", instanceId);
//            把创建出来工作流的工作id绑定到会议上了
            int row = tbMeetingDao.updateMeetingInstanceId(param);
            if (row != 1) {
                throw new EmosException("保存会议工作流实例ID失败");
            }
        }
    }


}
