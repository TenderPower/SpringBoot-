package com.example.emos.wx.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.example.emos.wx.config.SystemConstants;
import com.example.emos.wx.controller.form.CheckinForm;
import com.example.emos.wx.db.dao.*;
import com.example.emos.wx.db.pojo.TbCheckin;
import com.example.emos.wx.db.pojo.TbFaceModel;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
import com.example.emos.wx.task.EmailTask;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@Service
@Scope("prototype")//设置为多例对象，目的是可以进行异步执行
@Slf4j
public class CheckinServiceImpl implements CheckinService {
    //    定义变量
    @Autowired
    private SystemConstants systemConstants;
    @Autowired
    private TbHolidaysDao holidaysDao;
    @Autowired
    private TbWorkdayDao workdayDao;

    @Autowired
    private TbCheckinDao checkinDao;

    @Autowired
    private TbFaceModelDao faceModelDao;

    @Autowired
    private TbCityDao cityDao;

    @Autowired
    private TbUserDao userDao;

    @Value("${emos.face.createFaceModelUrl}")
    private String createFaceModelUrl;

    @Value("${emos.face.checkinUrl}")
    private String checkinUrl;


    @Value("${emos.email.hr}")
    private String hrEmail;

    @Value("${emos.code}")
    private String code;//证明你是本课程学院，但咱不是

    @Autowired
    private EmailTask emailTask;

    @Override
    public String validCanCheckIn(int userId, String date) {
//      调用持久层中dao中的方法，分别查询当天是节假日还是特殊的工作日
        boolean bool_1 = holidaysDao.searchTodayIsHolidays() != null ? true : false;
        boolean bool_2 = workdayDao.searchTodayIsWorkday() != null ? true : false;
//        设置当天默认为 工作日
        String type = "工作日";
//        查看当前这一天 是工作日 还是 节假日
//        DateUtil.date(）生成的是DateTime的对象
        if (DateUtil.date().isWeekend()) {//使用DateUtil里面的date方法 获得当前的日期对象
            type = "节假日";
        }
        if (bool_1) {
            type = "节假日";
        } else if (bool_2) {
            type = "工作日";
        }

        if (type.equals("节假日")) {
            return "节假日不需要考勤";
        } else {
//            当天是正常的一天，进行正常的考勤
//            生成DateTime对象（比Date对象包含的功能更多）
            DateTime now = DateUtil.date();
//            将当前这一天生成一个字符串
            String start = DateUtil.today() + " " + systemConstants.attendanceStartTime;
            String end = DateUtil.today() + " " + systemConstants.attendanceEndTime;
//             将字符串转换为DateTime对象
            DateTime attendanceStart = DateUtil.parse(start);
            DateTime attendanceEnd = DateUtil.parse(end);
//            开始比较 当前日期对象 与截至日期对象
            if (now.isBefore(attendanceStart)) {
                return "没到上班考勤开始时间";
            } else if (now.isAfter(attendanceEnd)) {
                return "超过了上班考勤结束时间";
            } else {
//                在考勤的时间段内，进行考勤
                HashMap map = new HashMap();
                map.put("userId", userId);
                map.put("date", date);
                map.put("start", start);
                map.put("end", end);
//                查看是否已经考勤
                boolean bool = checkinDao.haveCheckin(map) != null ? true : false;
                return bool ? "今日已经考勤，不用重复考勤" : "可以考勤";
            }
        }
    }

    @Override
    public void checkin(HashMap param) {
//        当前系统时间
        Date d1=DateUtil.date();
//        获取上班时间
        Date d2=DateUtil.parse(DateUtil.today()+" "+systemConstants.attendanceTime);
//        获取上班考勤的结束时间
        Date d3=DateUtil.parse(DateUtil.today()+" "+systemConstants.attendanceEndTime);
        int status=1;
        if(d1.compareTo(d2)<=0){//当前时间小于上班时间（正常的考勤）
            status=1;
        }
        else if(d1.compareTo(d2)>0&&d1.compareTo(d3)<0){//员工迟到，过了签到时间，但没有过考勤的时间
            status=2;
        }
        else{ //过了考勤时间，就没有必要记录到数据库里，直接空这就行
            throw new EmosException("超出考勤时间段，无法考勤");
        }
//        获取uerid的数据
        int userId= (Integer) param.get("userId");
//        获取签到人的人脸模型数据 （其实就是字符串）
//        我认为设置为默认的“renlianmoxing”
        String faceModel=faceModelDao.searchFaceModel(userId);
//        判断facemodel
        if(faceModel==null){
            throw new EmosException("不存在人脸模型");
        }
        else{//存在人脸模型
//            需要判断用户提交的人脸模型提取出来，跟数据库进行比较
//            获取图像路径
            String path=(String)param.get("path");

//            默认人脸模型匹配成功
            String body = "True";
            if (faceModel.equals("renlianmoxing")){
                body = "True";
            }else {
                body = "False";
            }
            if("True".equals(body)){
//                判断疫情的风险等级
                //查询疫情风险等级
                int risk=1;
//                获取城市信息
                String city= (String) param.get("city");
                String district= (String) param.get("district");

                String address= (String) param.get("address");
                String country= (String) param.get("country");
                String province= (String) param.get("province");

//                如果city 和 district不为空值，即可进行疫情风险等级的查询
                if(!StrUtil.isBlank(city)&&!StrUtil.isBlank(district)){
//                    查询城市对应的code（编号）
                    String code=cityDao.searchCode(city);
                    try{
//                        因为疫情已经过去，所以该链接已经生效
                        String url = "http://m." + code + ".bendibao.com/news/yqdengji/?qu=" + district;
                        System.out.println("获取疫情风险等级url："+url);
/*
//                        发出请求
                        Document document= Jsoup.connect(url).get();
//                        查找相应的控件
                        Elements elements=document.getElementsByClass("list-content");
* */
//                        elements.size()>0
                        if(true){
//                            取出其中的一个元素
//                            Element element=elements.get(0);
//                            找到想要的标签p，并获取其内容
//                            String result=element.select("p:last-child").text();
                            String result="低风险";
                            if("高风险".equals(result)){
                                risk=3;
                                //编写发送告警邮件
                                HashMap<String,String> map=userDao.searchNameAndDept(userId);
                                String name = map.get("name");
                                String deptName = map.get("dept_name");
                                deptName = deptName != null ? deptName : "";
                                SimpleMailMessage message=new SimpleMailMessage();
                                message.setTo(hrEmail);
                                message.setSubject("员工" + name + "身处高风险疫情地区警告");
                                message.setText(deptName + "员工" + name + "，" + DateUtil.format(new Date(), "yyyy年MM月dd日") + "处于" + address + "，属于新冠疫情高风险地区，请及时与该员工联系，核实情况！");
//                                发送邮件
                                emailTask.sendAsync(message);
                            }
                            else if("中风险".equals(result)){
                                risk=2;
                            }
                        }
                    }catch (Exception e){
                        log.error("执行异常",e);
                        throw new EmosException("获取风险等级失败");
                    }
                }
                //保存签到记录


                TbCheckin entity=new TbCheckin();

                entity.setUserId(userId);
                entity.setAddress(address);
                entity.setCountry(country);
                entity.setProvince(province);
                entity.setCity(city);
                entity.setDistrict(district);

                entity.setStatus((byte) status);
                entity.setRisk(risk);
                entity.setDate(DateUtil.today());
//                签到时间
                entity.setCreateTime(d1);
                checkinDao.insert(entity);
            }

/*
//            向 人脸识别程序发出http请求
            HttpRequest request= HttpUtil.createPost(checkinUrl);
//            现在，向python 程序发起http请求，上传照片
            request.form("photo", FileUtil.file(path),"targetModel",faceModel);
//            这个只有本课程的人员才可以调用
            request.form("code",code);
//            获取响应
            HttpResponse response=request.execute();
            if(response.getStatus()!=200){
                log.error("人脸识别服务异常");
                throw new EmosException("人脸识别服务异常");
            }
//            接受响应返回的内容（响应体）
            String body=response.body();
            if("无法识别出人脸".equals(body)||"照片中存在多张人脸".equals(body)){
                throw new EmosException(body);
            }
            else if("False".equals(body)){
                throw new EmosException("签到无效，非本人签到");
            }
            else if("True".equals(body)){
//                判断疫情的风险等级
                //查询疫情风险等级
                int risk=1;
//                获取城市信息
                String city= (String) param.get("city");
                String district= (String) param.get("district");

                String address= (String) param.get("address");
                String country= (String) param.get("country");
                String province= (String) param.get("province");

//                如果city 和 district不为空值，即可进行疫情风险等级的查询
                if(!StrUtil.isBlank(city)&&!StrUtil.isBlank(district)){
//                    查询城市对应的code（编号）
                    String code=cityDao.searchCode(city);
                    try{
                        String url = "http://m." + code + ".bendibao.com/news/yqdengji/?qu=" + district;
//                        发出请求
                        Document document= Jsoup.connect(url).get();
//                        查找相应的控件
                        Elements elements=document.getElementsByClass("list-content");
                        if(elements.size()>0){
//                            取出其中的一个元素
                            Element element=elements.get(0);
//                            找到想要的标签p，并获取其内容
                            String result=element.select("p:last-child").text();
//                          result="高风险";
                            if("高风险".equals(result)){
                                risk=3;
                                //编写发送告警邮件
                                HashMap<String,String> map=userDao.searchNameAndDept(userId);
                                String name = map.get("name");
                                String deptName = map.get("dept_name");
                                deptName = deptName != null ? deptName : "";
                                SimpleMailMessage message=new SimpleMailMessage();
                                message.setTo(hrEmail);
                                message.setSubject("员工" + name + "身处高风险疫情地区警告");
                                message.setText(deptName + "员工" + name + "，" + DateUtil.format(new Date(), "yyyy年MM月dd日") + "处于" + address + "，属于新冠疫情高风险地区，请及时与该员工联系，核实情况！");
//                                发送邮件
                                emailTask.sendAsync(message);
                            }
                            else if("中风险".equals(result)){
                                risk=2;
                            }
                        }
                    }catch (Exception e){
                        log.error("执行异常",e);
                        throw new EmosException("获取风险等级失败");
                    }
                }
                //保存签到记录


                TbCheckin entity=new TbCheckin();

                entity.setUserId(userId);
                entity.setAddress(address);
                entity.setCountry(country);
                entity.setProvince(province);
                entity.setCity(city);
                entity.setDistrict(district);

                entity.setStatus((byte) status);
                entity.setRisk(risk);
                entity.setDate(DateUtil.today());
                entity.setCreateTime(d1);
                checkinDao.insert(entity);
            }
* */

        }
    }

    @Override
    public void createFaceModel(int userId, String path) {
//            将人脸模型数据保存起来
        TbFaceModel entity=new TbFaceModel();
//            设置userId
        entity.setUserId(userId);
//            设置faceModel
        entity.setFaceModel("renlianmoxing");
//            存放到数据里
        faceModelDao.insert(entity);

//        目前人脸模型用不了  干脆设置为默认值吧
/*
//        发送HTTP请求给python方法
        HttpRequest request=HttpUtil.createPost(createFaceModelUrl);
//        设置上传的文件
        request.form("photo",FileUtil.file(path));
//            这个只有本课程的人员才可以调用
        request.form("code",code);
//        接受响应
        HttpResponse response=request.execute();
        String body=response.body();
//        进行判断
        if("无法识别出人脸".equals(body)||"照片中存在多张人脸".equals(body)){
            throw new EmosException(body);
        }
        else{
//            将人脸模型数据保存起来
            TbFaceModel entity=new TbFaceModel();
//            设置userId
            entity.setUserId(userId);
//            设置faceModel
            entity.setFaceModel(body);
//            存放到数据里
            faceModelDao.insert(entity);
        }
* */

    }

    @Override
    public HashMap searchTodayCheckin(int userId) {
        HashMap map=checkinDao.searchTodayCheckin(userId);
        return map;
    }

    @Override
    public long searchCheckinDays(int userId) {
        long days=checkinDao.searchCheckinDays(userId);
        return days;
    }

    @Override
    public ArrayList<HashMap> searchWeekCheckin(HashMap param) {
//        1、首先查询用户本周的考勤情况 包括特殊的工作日和节假日
        ArrayList<HashMap> checkinList=checkinDao.searchWeekCheckin(param);
//        特殊的节假日
        ArrayList holidaysList=holidaysDao.searchHolidaysInRange(param);
//        特殊的工作日
        ArrayList workdayList=workdayDao.searchWorkdayInRange(param);
//        2、接下来要生成 本周的日期对象
//        2.1生成日期对象必须规定起始日期 和 结束日期
//        在参数中获取本周的起始日期
        DateTime startDate=DateUtil.parseDate(param.get("startDate").toString());
//        获取本周的结束日期
        DateTime endDate=DateUtil.parseDate(param.get("endDate").toString());
//        通过DateUtil中的range函数， 就可以生成本周的七天日期
        DateRange range=DateUtil.range(startDate,endDate, DateField.DAY_OF_MONTH);
//        3. 接下来，拿着每天的日期看一下，当天是工作日还好节假日
//        如果是工作日就要看一下签到情况
        ArrayList<HashMap> list=new ArrayList<>();
        range.forEach(one->{
//            设置日期格式
            String date=one.toString("yyyy-MM-dd");
//            默认为工作日
            String type="工作日";
//            判断当前日期是否为周末
            if(one.isWeekend()){
                type="节假日";
            }
//            判断特殊的节假日里面是否存在当前日期
            if(holidaysList!=null&&holidaysList.contains(date)){
                type="节假日";
            }
//            判断特殊工作日里面是否存在当前日期
            else if(workdayList!=null&&workdayList.contains(date)){
                type="工作日";
            }
//            上述操作让我们知道当天是否是工作日还是节假日了
//            接下来，查看考勤结果
            String status="";
//            如果当前这一天one是工作日 并且 当天这一天one（相对于当下时间）已经发生了
//            比如今天是星期五 我们one是星期四
            if(type.equals("工作日")&&DateUtil.compare(one,DateUtil.date())<=0){
//                默认值
                status="缺勤";
                boolean flag=false;
//                如果能查询当天one的考勤结果，就把当前的考勤结果提出来
                for (HashMap<String,String> map:checkinList){
//                    date是one的格式化结果
                    if(map.containsValue(date)){
                        status=map.get("status");
                        flag=true;
                        break;
                    }
                }
//                如果在工作日里查询不到员工的考勤数据
//                有可能当天的考勤还没有结束
//                如果没结束的话，就判定该员工为缺勤，逻辑上不合理
//                所以需要做一下判断
//                4.设置当天考勤的结束时间
                DateTime endTime=DateUtil.parse(DateUtil.today()+" "+systemConstants.attendanceEndTime);
                String today=DateUtil.today();
//                判断date等于今天，并且当天的时间早于考勤的结束时间
                if(date.equals(today)&&DateUtil.date().isBefore(endTime)&&flag==false){
                    status="";
                }
            }
//            将拿到的某一天结果进行封装
            HashMap map=new HashMap();
            map.put("date",date);
            map.put("status",status);
            map.put("type",type);
//            将”星期“换成”周“
            map.put("day",one.dayOfWeekEnum().toChinese("周"));
//            将封装结果放到list里面
            list.add(map);
        });
        return list;
    }

    @Override
    public ArrayList<HashMap> searchMonthCheckin(HashMap param) {
//        param 时间范围设置为一个月
        return this.searchWeekCheckin(param);
    }
}
