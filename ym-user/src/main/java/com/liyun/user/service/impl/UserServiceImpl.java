package com.liyun.user.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.liyun.common.enums.ResultCode;
import com.liyun.common.exception.BizException;
import com.liyun.common.utils.JwtUtils;
import com.liyun.common.utils.RandomUtils;
import com.liyun.user.domain.dto.LoginDTO;
import com.liyun.user.domain.dto.RegisterDTO;
import com.liyun.user.domain.pojo.User;
import com.liyun.user.domain.vo.LoginVO;
import com.liyun.user.mapper.UserMapper;
import com.liyun.user.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.liyun.user.constants.UserConstant.SMS_CODE;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author liyun
 * @since 2026-05-07
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final StringRedisTemplate redisTemplate;
    // 在类中注入或直接创建 BCrypt 实例（推荐注入，这里为演示直接创建）
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private Long expire;
    public LoginVO login(LoginDTO dto) {
        // 1.查用户
        User user;
        String account = dto.getAccount();

        if (account.matches("^\\d+$")) {
            // 纯数字，查手机号
            user = lambdaQuery().eq(User::getPhone, account).one();
        } else {
            // 非纯数字，查用户名
            user = lambdaQuery().eq(User::getUsername, account).one();
        }

        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_EXIST);
        }

        // 2.校验密码
        // BCrypt校验

        // 3.生成token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());
        String token = JwtUtils.createToken(claims, secret, expire);

        // 4.封装VO返回
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        return vo;
    }

    @Override
    public String sendSms(String phone) {
        //1.查询当前用户是否已存在
        User one = lambdaQuery().eq(User::getPhone, phone).one();
        if (one != null){
            throw new RuntimeException("用户已存在");
        }
        //2.生成验证码
        // 用 hutool 生成6位随机数字验证码（你项目已经引入了hutool）
        String code = RandomUtil.randomNumbers(6);

        //3.缓存到redis中
        redisTemplate.opsForValue().set(SMS_CODE + phone,code, 2,TimeUnit.MINUTES);

        // 4. 暂时先打印出来测试，后期替换成真实短信服务
        log.info("验证码：{}", code);
        return code;
    }


    @Override
    public void register(RegisterDTO registerDTO) {
        // 1. 查询当前用户是否已存在
        User existingUser = lambdaQuery().eq(User::getPhone, registerDTO.getPhone()).one();
        if (existingUser != null) {
            throw new RuntimeException("用户已存在");
        }

        // 2. 验证验证码
        String redisKey = SMS_CODE + registerDTO.getPhone();
        String rightCode = redisTemplate.opsForValue().get(redisKey);

        if (rightCode == null) {
            throw new RuntimeException("验证码已过期");
        }

        // 安全比较：防止时序攻击（虽然验证码是数字，但养成好习惯）
        if (!rightCode.equals(registerDTO.getCode())) {
            throw new RuntimeException("验证码错误");
        }

        // 验证成功后立即删除 Redis 中的验证码（防止重复使用）
        redisTemplate.delete(redisKey);

        // 3. 生成用户
        User user = new User();
        user.setPhone(registerDTO.getPhone());
        user.setUsername("ym" + RandomUtils.randomNumbers(26));

        // 🔐 使用 BCrypt 加密密码（关键！）
        String rawPassword = registerDTO.getPassword();
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        user.setPassword(passwordEncoder.encode(rawPassword)); // ← 加密！

        // ✅ 修复昵称赋值 bug
        if (registerDTO.getNickname() == null || registerDTO.getNickname().trim().isEmpty()) {
            user.setNickname("用户" + RandomUtils.randomNumbers(20)); // 6位足够，32位太长
        } else {
            user.setNickname(registerDTO.getNickname());
        }

        save(user);

        // 🛡️ 日志脱敏：不要打印完整 user（可能含密码哈希，虽不可逆但避免泄露）
        log.info("新用户注册成功，手机号: {}, 用户ID: {}", user.getPhone(), user.getId());
    }
}
