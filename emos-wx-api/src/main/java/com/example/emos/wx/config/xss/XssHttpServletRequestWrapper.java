package com.example.emos.wx.config.xss;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.json.JSONUtil;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
//        在请求中获取的原始数据 放入 value
        String value = super.getParameter(name);
//        判断 value是否为空
        if (!StrUtil.hasEmpty(value)) {
//            对value进行转义 目的是方式XSS
            value = HtmlUtil.filter(value);
        }
        return value;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
//        对数组中的每个元素 进行 转义
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                String value = values[i];
//                单个元素转义
                if (!StrUtil.hasEmpty(value)) {
                    value = HtmlUtil.filter(value);
                }
                values[i] = value;
            }
        }
        return values;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> parameters = super.getParameterMap();
//        新定义一个map 用于存放 转义好的 信息
        LinkedHashMap<String, String[]> map = new LinkedHashMap();
        if (parameters != null) {
            for (String key : parameters.keySet()) {

                String[] values = parameters.get(key);
                for (int i = 0; i < values.length; i++) {

                    String value = values[i];
                    if (!StrUtil.hasEmpty(value)) {
                        value = HtmlUtil.filter(value);
                    }
                    values[i] = value;
                }
                map.put(key, values);
            }
        }
        return map;
    }

    @Override
    public String getHeader(String name) {
//        用value 保存从请求头请求的数据
        String value = super.getHeader(name);
//        进行转义
        if (!StrUtil.hasEmpty(value)) {
            value = HtmlUtil.filter(value);
        }
        return value;
    }

    @Override
//    重写getInputStream方法，目的：
//    Spring MVC 框架 就是通过这个方法 从请求里面 提取 客户端提交的数据， 然后把这些数据封装到Form对象里面，
//    如果，我们不对getInputStream方法读取的数据进行转义
//    那么后端项目不具备脚本读取的能力
    public ServletInputStream getInputStream() throws IOException {
//        保持IO流
        InputStream in = super.getInputStream();
//        创建字符流
        InputStreamReader reader = new InputStreamReader(in, Charset.forName("UTF-8"));
//        创建缓冲流
        BufferedReader buffer = new BufferedReader(reader);
//        方便后面做字符串拼接
        StringBuffer body = new StringBuffer();
//        从缓冲流中 读取 第一行数据
        String line = buffer.readLine();
//        判断第一行数据 是否有效
        while (line != null) {
            body.append(line);
//            读取下一行的数据
            line = buffer.readLine();
        }
//        读取完毕后，进行关闭操作
        buffer.close();
        reader.close();
        in.close();
//        接下来，将读取出来的请求内容 进行 转换
//        将JSON格式 转为 Map 对象
        Map<String, Object> map = JSONUtil.parseObj(body.toString());
//        将Map对象中的数据进行转义，将转义之后的结果 存放到新Map对象中
        Map<String, Object> result = new LinkedHashMap<>();
//        进行转义
        for (String key : map.keySet()) {
            Object val = map.get(key);
//            val是String对象 就进行转义
//            否则进行保持即可
            if (val instanceof String) {
                if (!StrUtil.hasEmpty(val.toString())) {
                    result.put(key, HtmlUtil.filter(val.toString()));
                }
            } else {
                result.put(key, val);
            }
        }
//        现在，将处理好的数据，重新转为JSON格式的字符串
        String json = JSONUtil.toJsonStr(result);
//        创建一个IO流，然后，从这个字符串中读数据
        ByteArrayInputStream bain = new ByteArrayInputStream(json.getBytes());
//        接下来，构建 返回 IO的对象
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return bain.read();
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }
        };
    }
}
