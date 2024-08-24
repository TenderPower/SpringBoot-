package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@ApiModel("LoginForm 是整个APi中其中的一个Model")
@Data
//创建 LoginForm.java 类，封装前端提交的数据
public class LoginForm {
    @NotBlank(message = "临时授权不能为空")
    private String code;
}
