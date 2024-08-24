前端：uni-app
后端：SpringBoot；
1.	自定义异常类；
2.	封装Web返回对象；
3.	Swagger；
4.	继承HttpServletRequestWrapper父类 定义请求包装类。目的：抵御即跨站脚本（XSS）攻击，就是对用户输入的数据进行转义，然后存储到数据库里面。等到视图层渲染HTML页面的时候。转义后的文字是不会被当做JavaScript执行的，这就可以抵御XSS攻击。
5.	Shiro实现认证和授权
6.	JWT
7.	注册用户：使用RABC权限模型（简化用户的权限管理，减少系统的开销）
8.	腾讯云对象存储服务
9.	Redis：缓存Token；更新令牌；缓存系统中的激活码；
10.	MongoDB ：存放消息数据；（适合存储海量低价值的数据）
11.	RabbitMQ：实现消息投递削峰填谷（系统如果有800万注册用户，需要向他们发送公告消息）
实现：企业在线办公系统
![image](https://github.com/user-attachments/assets/9f7f167c-28b7-409f-99f9-be06cc234385)
![image](https://github.com/user-attachments/assets/cfa219b2-8937-4319-ba0c-88e3b349d21b)
![image](https://github.com/user-attachments/assets/053a0d5b-5095-4781-a34c-7c16955352a2)
![image](https://github.com/user-attachments/assets/f55123f8-bcc5-415f-a9dc-157598ea3214)
![image](https://github.com/user-attachments/assets/1176c285-cb97-45f2-b1aa-cb7d729bb887)
![image](https://github.com/user-attachments/assets/06a0d401-f3dc-4e6d-94f2-708ce3346871)





