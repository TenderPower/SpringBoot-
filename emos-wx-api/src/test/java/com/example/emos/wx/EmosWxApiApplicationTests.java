package com.example.emos.wx;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRefEntity;
import com.example.emos.wx.db.pojo.TbMeeting;
import com.example.emos.wx.db.pojo.TbUser;
import com.example.emos.wx.service.MeetingService;
import com.example.emos.wx.service.MessageService;
import com.example.emos.wx.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@SpringBootTest
class EmosWxApiApplicationTests {
    @Autowired
    private MessageService messageService;

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private UserService userService;

    @Test
    void contextLoads() {
        for (int i = 1; i <= 100; i++) {
            MessageEntity message = new MessageEntity();
            message.setUuid(IdUtil.simpleUUID());
            message.setSenderId(0);
            message.setSenderName("系统消息");
            message.setMsg("这是第" + i + "条测试消息");
            message.setSendTime(new Date());
            String id=messageService.insertMessage(message);

            MessageRefEntity ref=new MessageRefEntity();
            ref.setMessageId(id);
            ref.setReceiverId(8); //接收人ID
            ref.setLastFlag(true);
            ref.setReadFlag(false);
            messageService.insertRef(ref);
        }
    }

    @Test
    void creatMeetingData(){
        for (int i=1;i<=100;i++){
            TbMeeting meeting=new TbMeeting();
            meeting.setId((long)i);
            meeting.setUuid(IdUtil.simpleUUID());
            meeting.setTitle("测试会议"+i);
//            +L表示Long 类型
            meeting.setCreatorId(8L); //ROOT用户ID
            meeting.setDate(DateUtil.today());
            meeting.setPlace("线上会议室");
            meeting.setStart("08:30");
            meeting.setEnd("10:30");
            meeting.setType((short) 1);
            meeting.setMembers("[8,16]");
            meeting.setDesc("会议研讨Emos项目上线测试");
            meeting.setInstanceId(IdUtil.simpleUUID());
//            1待审批，2审批不通过，3未开始，4进行中，5已结束
            meeting.setStatus((short)3);
            meetingService.insertMeeting(meeting);
        }
    }
    @Test
    void creatUserData(){
        List L = new ArrayList();
        L.add("https://ygkstorage-1305279327.cos.ap-beijing.myqcloud.com/img/header/OIP%20%281%29.jpg ");
        L.add("https://ygkstorage-1305279327.cos.ap-beijing.myqcloud.com/img/header/OIP%20%282%29.jpg");
        L.add("https://ygkstorage-1305279327.cos.ap-beijing.myqcloud.com/img/header/OIP%20%283%29.jpg");
        L.add("https://ygkstorage-1305279327.cos.ap-beijing.myqcloud.com/img/header/OIP%20%284%29.jpg");
        L.add("https://ygkstorage-1305279327.cos.ap-beijing.myqcloud.com/img/header/OIP%20%285%29.jpg");
        L.add("https://ygkstorage-1305279327.cos.ap-beijing.myqcloud.com/img/header/OIP%20%286%29.jpg");

        for(int i=4;i<=5;i++){
            HashMap param = new HashMap();
            param.put("openId","od4pu5Dvq5-UMyG9ZCPuDeHFvvM"+(i+1));
            param.put("nickname","nickname_"+i);
            param.put("photo",L.get(i));
            param.put("role","[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]");
            param.put("status",1);
            param.put("deptName","行政部");
            param.put("createTime",new Date());
            param.put("root",false);
            userService.insertUser(param);
        }
    }

}
