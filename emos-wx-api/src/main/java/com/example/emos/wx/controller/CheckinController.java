package com.example.emos.wx.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.example.emos.wx.common.util.R;
import com.example.emos.wx.config.SystemConstants;
import com.example.emos.wx.config.shiro.JwtUtil;
import com.example.emos.wx.controller.form.CheckinForm;
import com.example.emos.wx.controller.form.SearchMonthCheckinForm;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
import com.example.emos.wx.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

@RequestMapping("/checkin") //分配请求路径
@RestController//目的是将接受数据和返回数据都是JSON格式
@Api("签到模块（CheckinController）Web模块")
@Slf4j
public class CheckinController {
    @Autowired
//    定义该变量的目的：
//    因为业务层ChecinService中的方法有两个参数一个是userId，一个是date
//    那么userId怎么来的呢？-》是通过请求头中获得token字符串
//    然后token通过JwtUtil中的方法 获得 userId
    private JwtUtil jwtUtil;
    @Autowired
    private CheckinService checkinService;

    @Value("${emos.image-folder}")
    private String imageFolder;

    @Autowired
    private UserService userService;

    @Autowired
    private SystemConstants systemConstants;

    @GetMapping("/validCanCheckIn")
    @ApiOperation("查看用户今天是否可以签到")
//    @RequestHeader("token") 从请求头里面获取用户上传的token字符串
    public R validCanCheckIn(@RequestHeader("token") String token){
//        解析token 获得 userId
        int userId=jwtUtil.getUserId(token);
//        查看用户今天签到情况
        String result=checkinService.validCanCheckIn(userId, DateUtil.today());//DateUtil.today() 返回现在的日期字符串
        return R.ok(result);
    }

    @PostMapping("/checkin")
    @ApiOperation("执行签到")
//    该方法用到了三个参数
//    1. checkinForm
//    2. 用来接受上传文件的（photo）
//    3. token
    public R checkin(@Valid CheckinForm form, @RequestParam("photo") MultipartFile file, @RequestHeader("token") String token){
//        判断上传的文件是否为空值
        if (file == null) {
            return R.error("没有上传文件");
        }
//        先从token里获取userId
        int userId = jwtUtil.getUserId(token);
//        判断文件图片是否为jpg格式的
//        获取文件名字
        String fileName = file.getOriginalFilename().toLowerCase();

        System.out.println(fileName);

        if(!fileName.endsWith(".jpg")){
            return R.error("必须提交JPG格式图片");

        }else{
//            将图片进行存储
            String path = imageFolder+"/"+fileName;
            try {
                file.transferTo(Paths.get(path));
//              调用业务层方法
                HashMap param = new HashMap();
                param.put("userId",userId);
                param.put("path",path);
                param.put("city",form.getCity());
                param.put("district",form.getDistrict());
                param.put("address",form.getAddress());
                param.put("country",form.getCountry());
                param.put("province",form.getProvince());
//                验证用户图片是否合理 地址是否高危 签到时间是否截至
                checkinService.checkin(param);
                return R.ok("签到成功");
            }catch (IOException e){
                log.error(e.getMessage(),e);
                throw new EmosException("图片保存错误");
            }finally {
//                签到成功，即可删除上传的图片
                FileUtil.del(path);
            }

        }
    }



    @PostMapping("/createFaceModel")
    @ApiOperation("创建人脸模型")
//    参数只需要两个，一个是上传的图片，一个是token
//    并不需要Form类去接受前端发出的参数值
    public R createFaceModel(@RequestParam("photo") MultipartFile file,@RequestHeader("token") String token){
//        判断文件
        if(file==null){
            return R.error("没有上传文件");
        }
        int userId=jwtUtil.getUserId(token);
//        判断图片格式
        String fileName=file.getOriginalFilename().toLowerCase();
        if(!fileName.endsWith(".jpg")){
            return R.error("必须提交JPG格式图片");
        }
        else{
//            将图片文件进行保存
            String path=imageFolder+"/"+fileName;
            try{
//                将文件保存到指定路径下的文件夹里
                file.transferTo(Paths.get(path));
//                将暂存的文件和对应的userId传给业务层 构建相应的人脸模型
                checkinService.createFaceModel(userId,path);
                return R.ok("人脸建模成功");
            }catch (IOException e){
                log.error(e.getMessage(),e);
                throw new EmosException("图片保存错误");
            }
            finally {
                FileUtil.del(path);
            }

        }
    }


    //    查询用户签到结果
    @GetMapping("/searchTodayCheckin")
    @ApiOperation("查询用户当日签到数据")
    //移动端向后端的web层提交token请求
    public R searchTodayCheckin(@RequestHeader("token") String token){

        int userId=jwtUtil.getUserId(token);
    //    获取员工当天的考勤结果和基本信息（但没有当天考勤的开始时间和结束时间）
        HashMap map=checkinService.searchTodayCheckin(userId);
    //    要添加其他必要信息
        map.put("attendanceTime",systemConstants.attendanceTime);
        map.put("closingTime",systemConstants.closingTime);
    //    获取员工签到的总天数
        long days=checkinService.searchCheckinDays(userId);
        map.put("checkinDays",days);

    //   获取员工的入职日期（封装成日期对象）
        DateTime hiredate=DateUtil.parse(userService.searchUserHiredate(userId));
    //   我想知道，本周开始的日期是什么样子的
        DateTime startDate=DateUtil.beginOfWeek(DateUtil.date());
    //    判断本周开始日期是否 在用户入职之前
        if(startDate.isBefore(hiredate)){
            startDate=hiredate;
        }
    //    本周的结束日期
        DateTime endDate=DateUtil.endOfWeek(DateUtil.date());
    //    将上述的变量，封装成参数传给业务层
        HashMap param=new HashMap();
        param.put("startDate",startDate.toString());
        param.put("endDate",endDate.toString());
        param.put("userId",userId);
        ArrayList<HashMap> list=checkinService.searchWeekCheckin(param);
    //    最后将业务层返回的结果封装到map中，用于返回到前端
        map.put("weekCheckin",list);
        return R.ok().put("result",map);
    }

    @PostMapping("/searchMonthCheckin")
    @ApiOperation("查询用户某月签到数据")
    public R searchMonthCheckin(@Valid @RequestBody SearchMonthCheckinForm form, @RequestHeader("token") String token){
        int userId=jwtUtil.getUserId(token);
//        获取用户的入职日期
        DateTime hiredate=DateUtil.parse(userService.searchUserHiredate(userId));
//        将月份1 更改成01 构成二位数
        String month=form.getMonth()<10?"0"+form.getMonth():form.getMonth().toString();
//        构造查询月第一天的日期对象
        DateTime startDate=DateUtil.parse(form.getYear()+"-"+month+"-01");
//        根据入职日期作比较
//        如果查询的某月 在用户入职月份之前  （抛异常）
        if(startDate.isBefore(DateUtil.beginOfMonth(hiredate))){
            throw new EmosException("只能查询考勤之后日期的数据");
        }
//        说明查询的月份是和用户入职月份一致的
        if(startDate.isBefore(hiredate)){
//            则定义当前月的起始日期 为用户的入职日期
            startDate=hiredate;
        }
//        构造月份的结束日期(月）
        DateTime endDate=DateUtil.endOfMonth(startDate);
//        构造查询参数
        HashMap param=new HashMap();
        param.put("userId",userId);
        param.put("startDate",startDate.toString());
        param.put("endDate",endDate.toString());
//        查询月份的签到结果
        ArrayList<HashMap> list=checkinService.searchMonthCheckin(param);
        int sum_1=0,sum_2=0,sum_3=0;
        for(HashMap<String,String> one:list){
            String type=one.get("type");
            String status=one.get("status");
            if("工作日".equals(type)){
                if("正常".equals(status)){
                    sum_1++;
                }
                else if("迟到".equals(status)){
                    sum_2++;
                }
                else if("缺勤".equals(status)){
                    sum_3++;
                }
            }
        }
        return R.ok().put("list",list).put("sum_1",sum_1).put("sum_2",sum_2).put("sum_3",sum_3);
    }
}
