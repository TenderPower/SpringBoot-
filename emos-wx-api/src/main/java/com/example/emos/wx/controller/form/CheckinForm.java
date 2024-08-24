package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@ApiModel("ChecinForm 是整个APi中其中的一个Model")
@Data
public class CheckinForm {
    private String address;
    private String country;
    private String province;
    private String city;
    private String district;
}
