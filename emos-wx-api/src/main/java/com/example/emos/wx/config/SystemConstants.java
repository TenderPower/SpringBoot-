package com.example.emos.wx.config;

import lombok.Data;
import org.springframework.stereotype.Component;

//为该Java项目设置 系统常量 ->让全局可以使用
@Data
@Component
public class SystemConstants {
    public String attendanceStartTime;
    public String attendanceTime;
    public String attendanceEndTime;
    public String closingStartTime;
    public String closingTime;
    public String closingEndTime;
}
