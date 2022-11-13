package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;


/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    StringRedisTemplate template;
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //校验未通过
        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }
        //通过
        //生成验证码
        String code = RandomUtil.randomNumbers(6);
        //保存验证码到session
        template.opsForValue().set(RedisConstants.LOGIN_CODE_KEY +phone,code,LOGIN_CODE_TTL,TimeUnit.MINUTES);
        log.debug("验证码为:"+code);
        return Result.ok();
    }

    @Override
    public Result login(String phone, String code, HttpSession session) {
        //根据电话查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        User user = baseMapper.selectOne(queryWrapper.eq(User::getPhone, phone));

        String code1 = template.opsForValue().get(RedisConstants.LOGIN_CODE_KEY +phone);
        //用户不存在,创建用户
        if (user == null){
            user = createWithPhone(phone);
        }
        //用户存在
        log.debug(code1);

        if ( code1 == null || (!code1.equals(code))){
            return Result.fail("验证码错误");
        }
        String token = UUID.randomUUID().toString(true);
        UserDTO userDTO = BeanUtil.copyProperties(user,UserDTO.class);

        Map<String, Object> map = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName,fieldValue) -> fieldValue.toString()));
        System.out.println("map:"+map);
        String tokenKey = RedisConstants.LOGIN_USER_KEY+token;
        template.opsForHash().putAll(tokenKey,map);
        template.expire(tokenKey,LOGIN_USER_TTL, TimeUnit.MINUTES);

        return Result.ok(token);
    }

    private User createWithPhone(String phone){
        User user = new User();
        user.setPhone(phone);
        user.setNickName("user"+phone.substring(0,8));
        boolean save = save(user);
        return user;
    }
}
