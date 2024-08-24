package com.example.emos.wx.controller;

import cn.hutool.json.JSONUtil;
import com.example.emos.wx.common.util.R;
import com.example.emos.wx.config.shiro.JwtUtil;
import com.example.emos.wx.controller.form.LoginForm;
import com.example.emos.wx.controller.form.RegisterForm;
import com.example.emos.wx.controller.form.SearchMembersForm;
import com.example.emos.wx.controller.form.SearchUserGroupByDeptForm;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController //Web层（用于生成url，提供给前端使用）
//@CrossOrigin
@RequestMapping("/user")
@Api("用户模块(UserController)Web接口")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RedisTemplate redisTemplate;
    @Value("${emos.jwt.cache-expire}")
    private int cacheExpire;

    private void saveCacheToken(String token, int userId) {
//        将令牌和id 存放到Redis数据库中（固定写法）
        redisTemplate.opsForValue().set(token, userId + "", cacheExpire, TimeUnit.DAYS);
    }

    @PostMapping("/register")
    @ApiOperation("注册用户")
    //    @RequestBody 接收前端传递给后端的json字符串中的数据
    public R register(@Valid @RequestBody RegisterForm form) {
//        获取主键
        int id = userService.registerUser(form.getRegisterCode(), form.getCode(), form.getNickname(), form.getPhoto());
//        根据主键值获取字符串令牌
        String token = jwtUtil.createToken(id);
//        获取该主键值所对应的权限
        Set<String> permsSet = userService.searchUserPermissions(id);
//        保存
        saveCacheToken(token, id);
        return R.ok("用户注册成功").put("token", token).put("permission", permsSet);
    }

    //    创建登录的Web方法
    @PostMapping("/login")
    @ApiOperation("登录 系统")
    public R login(@Valid @RequestBody LoginForm form) {
//        通过微信临时授权字符串，查找对应的openid 是否存在对应的用户
        int id = userService.login(form.getCode());//存在就返回id，不存在就抛异常了
        String token = jwtUtil.createToken(id);
//        获取id所对应的权限
        Set<String> permsSet = userService.searchUserPermissions(id);
//        保存到Redis数据库中
        saveCacheToken(token,id);
//        判定用户登陆成功之后，向客户端返回权限列表和Token令牌
        return R.ok("登陆成功").put("token", token).put("permission", permsSet);
    }

    @GetMapping("/searchUserSummary")
    @ApiOperation("查询用户基本信息")
    public R searchUserSummary(@RequestHeader("token") String token){
        int userId = jwtUtil.getUserId(token);
        HashMap map = userService.searchUserSummary(userId);
        return R.ok().put("result",map);
    }

    @PostMapping("/searchUserGroupByDept")
    @ApiOperation("查询员工列表，按照部门分组排列")
    @RequiresPermissions(value = {"ROOT","EMPLOYEE:SELECT"},logical = Logical.OR)
    public R searchUserGroupByDept(@Valid @RequestBody SearchUserGroupByDeptForm form){
        ArrayList<HashMap> list=userService.searchUserGroupByDept(form.getKeyword());
        return R.ok().put("result",list);
    }

    @PostMapping("/searchMembers")
    @ApiOperation("查询成员")
    @RequiresPermissions(value = {"ROOT", "MEETING:INSERT", "MEETING:UPDATE"},logical = Logical.OR) //设置权限
    public R searchMembers(@Valid @RequestBody SearchMembersForm form){
//        判断前端发的字符串 是否为json数组
        if(!JSONUtil.isJsonArray(form.getMembers())){
            throw new EmosException("members不是JSON数组");
        }
//        如果是,就将字符串解析成json数组, 然后转为List对象, 然后每个数据的类型都是整数
        List param=JSONUtil.parseArray(form.getMembers()).toList(Integer.class);
        if (param.isEmpty()){
            return R.ok().put("result",new ArrayList<>());
        }
        ArrayList list=userService.searchMembers(param);

        return R.ok().put("result",list);
    }


}
