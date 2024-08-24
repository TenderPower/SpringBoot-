package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@ApiModel("TestSayHelloForm 是整个APi中其中的一个Model") //描述一个模型
@Data//用于自动生成 Java 类的常见方法，如equals()、hashCode()、toString()、getter和setter等
public class TestSayHelloForm  {
//    @NotBlank
//    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{2,15}$")
    @ApiModelProperty("姓名")
    private String name;
}
