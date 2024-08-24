package com.example.emos.wx.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.emos.wx.db.dao.TbDeptDao;
import com.example.emos.wx.db.dao.TbUserDao;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.TbUser;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.UserService;
import com.example.emos.wx.task.MessageTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.*;

@Service //业务层，（用于实现与持久层的交互）
@Slf4j
@Scope("prototype")
public class UserServiceImpl implements UserService {
    //    值注入
    @Value("${wx.app-id}")
    private String appId;
    @Value("${wx.app-secret}")
    private String appSecret;
    //    类注入
    @Autowired
    private TbUserDao userDao;
    @Autowired
    private TbDeptDao deptDao;

    @Autowired
    private MessageTask messageTask;

    //    获取微信用户的 OpenId ，需要后端程序向微信平台发出请求，并上传若干参数，最终才能得到
    private String getOpenId(String code) {
//        该Code是前端生成的临时授权字符串，位于前端register.vue
//        微信的url
        String url = "https://api.weixin.qq.com/sns/jscode2session";
        HashMap map = new HashMap();
        map.put("appid", appId);
        map.put("secret", appSecret);
//        临时授权字符串
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String response = HttpUtil.post(url, map);

//        将响应转换为Json格式
        JSONObject json = JSONUtil.parseObj(response);
//        获取openId
        String openId = json.getStr("openid");
        if (openId == null || openId.length() == 0) {
            throw new RuntimeException("临时登录凭证错误");
        }
        return openId;
    }


    //    编写注册新用户的业务代码
    @Override
    public int registerUser(String registerCode, String code, String nickname, String photo) {
//        如果邀请码是000000，代表超级管理员
        if (registerCode.equals("000000")) {
//            查询超级管理员账号是否已经绑定
//            从调用Sql，进行查询
            boolean bool = userDao.haveRootUser();
            if (!bool) {
//                就把当前用户进行绑定
                String openId = getOpenId(code);
//                设置参数
                HashMap param = new HashMap();
                param.put("openId", openId);
                param.put("nickname", nickname);
                param.put("photo", photo);
                param.put("role", "[0]");
                param.put("status", 1);
//              设置部门
                param.put("deptName", "管理部");
                param.put("createTime", new Date());
                param.put("hiredate",new Date());
                param.put("root", true);

                userDao.insert(param);
//                在员工表中插入新纪录，由于主键是自动生成的, 所以我们需要知道新记录的主键值是多少
                int id = userDao.searchIdByOpenId(openId);

//                用户注册流程中，当用户成功注册的时候，我们就利用 MessageTask 异步发送消息到MQ队列中
//                添加发送系统消息的代码
                MessageEntity entity = new MessageEntity();
                entity.setSenderId(0);
                entity.setSenderName("系统消息");
                entity.setUuid(IdUtil.simpleUUID());
                entity.setMsg("欢迎您注册成为超级管理员，请及时更新你的员工个人信息。");
                entity.setSendTime(new Date());
//                rabbitmq中的topic其实就是用户的userId
                messageTask.sendAsync(id + "", entity);
//                用户注册成功之后，系统消息应该发送到 message 集合作为存档，
//                而 message_ref 集合中没有记录。
                return id;
            } else {
//                如果root已经绑定了，就抛出异常
//                自己写的异常类
                throw new EmosException("无法绑定超级管理员账号");
            }
        }
//        TODO  此处还有其他判断内容
        else {

            return 0;
        }
    }

    @Override
    public Set<String> searchUserPermissions(int userId) {
//        查询用户的权限列表
        return userDao.searchUserPermissions(userId);
    }

    //    实现用户登录功能
    @Override
    public Integer login(String code) {
//        用户在Emos登陆页面点击登陆按钮，然后小程序把 临时授权字符串 提交给后端Java系统
//        后端Java系统拿着临时授权字符串换取到 openid
        String openId = getOpenId(code);
//        如果不存在，说明该用户尚未注册，目前还不是我们的员工，所以禁止登录
        Integer id = userDao.searchIdByOpenId(openId);
        if (id == null) {
            throw new EmosException("账号不存在");
        }
//        如果存在openid，意味着该用户是已注册用户，可以登录。
//        TODO 从消息队列中接受消息，
//        1.在用户离线的过程中，Emos系统发送的消息通知是存放在MQ队列中的
//        2.所以用户登陆之后就需要接收这些消息通知
//        3.就是让 MessageTask 异步接受MQ中的消息，然后存储在message_ref 集合中。
        messageTask.receiveAsync(id+"");
        return id;
    }

    //    实现通过id查询用户信息
    @Override
    public TbUser searchById(int userId) {
        TbUser user = userDao.searchById(userId);
        return user;
    }

    @Override
    public String searchUserHiredate(int userId) {
        return userDao.searchUserHiredate(userId);
    }

    @Override
    public HashMap searchUserSummary(int userId) {
        return userDao.searchUserSummary(userId);
    }

    @Override
    public ArrayList<HashMap> searchUserGroupByDept(String keyword) {

        ArrayList<HashMap> list_1=deptDao.searchDeptMembers(keyword);//        只找部门有员工的部门名
        ArrayList<HashMap> list_2=userDao.searchUserGroupByDept(keyword);
        for(HashMap map_1:list_1){
            long deptId=(Long)map_1.get("id");
            ArrayList members=new ArrayList();
            for(HashMap map_2:list_2){
                long id=(Long) map_2.get("deptId");
                if(deptId==id){
                    members.add(map_2);
                }
            }
            map_1.put("members",members);
        }
        return list_1;
    }

    @Override
    public int insertUser(HashMap parm) {

        return userDao.insert(parm);
    }

    @Override
    public ArrayList<HashMap> searchMembers(List param) {
        return userDao.searchMembers(param);
    }


}
