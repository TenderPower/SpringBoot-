package com.example.emos.wx.config.shiro;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component //泛指各种组件，就是说当我们的类不属于各种归类的时候（不属于@Controller、@Services等的时候），我们就可以使用@Component来标注这个类
@Slf4j //日志注解
public class JwtUtil {
    @Value("${emos.jwt.secret}")//进行值注入 （根据配置文件application.yml 进行注入）
    private String secret;

    @Value("${emos.jwt.expire}")
    private int expire;

    public String createToken(int userId){
//        做日期偏移 偏移5天
        Date date=DateUtil.offset(new Date(), DateField.DAY_OF_YEAR,expire);
//        根据密钥 进行加密 构建算法对象
        Algorithm algorithm=Algorithm.HMAC256(secret);
//        创建内部类
        JWTCreator.Builder builder= JWT.create();
//        生成字符串令牌(加密后的）
        String token=builder.withClaim("userId",userId).withExpiresAt(date).sign(algorithm);
        return token;
    }

//    根据令牌 返回 userID
    public int getUserId(String token){
//        进行解码
        DecodedJWT jwt=JWT.decode(token);
//        根据上面生成 令牌 时所为 userId 起的属性名是叫 “userId"
//        那我们就可以通过这个属性名得到userId
        int userId=jwt.getClaim("userId").asInt();
        return userId;
    }

//    严重令牌字符串的有效性
//    通过时，不需要操作
//    不通过时，抛异常
    public void verifierToken(String token){
//        根据密钥 进行加密 构建算法对象
        Algorithm algorithm=Algorithm.HMAC256(secret);
//        创建验证对象
        JWTVerifier verifier=JWT.require(algorithm).build();
//        验证令牌（token）
        verifier.verify(token);
    }
}
