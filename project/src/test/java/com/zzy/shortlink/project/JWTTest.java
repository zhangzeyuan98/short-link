package com.zzy.shortlink.project;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.zzy.shortlink.project.toolkit.JWTUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@Slf4j
public class JWTTest {

    @Test
    void contextLoads() {
        Map<String, String> payload = new HashMap<>();
        payload.put("id", "21");
        payload.put("name", "xiaoshuang");
        // 生成jwt令牌
        String token = JWTUtils.getToken(payload);
        System.out.println(token);
        verify(token);
    }
    @Test
    public void verify(String token){
        // 通过签名生成验证对象
        log.info("当前token为：[{}]",token);
        Map<String,Object> map = new HashMap<>();
        try {
            // 验证令牌
            DecodedJWT verify = JWTUtils.verify(token);
            System.out.println(verify);
            map.put("state",true);
            map.put("msg","请求成功");
            map.put("verify", verify);
        } catch (SignatureVerificationException e) {
            e.printStackTrace();
            map.put("msg","无效签名！");
        }catch (TokenExpiredException e){
            e.printStackTrace();
            map.put("msg","token过期");
        }catch (AlgorithmMismatchException e){
            e.printStackTrace();
            map.put("msg","算法不一致");
        }catch (Exception e){
            e.printStackTrace();
            map.put("msg","token无效！");
        }
        map.put("state",false);
        map.forEach((k, v) -> System.out.println(k + ": " + v));
    }
}
