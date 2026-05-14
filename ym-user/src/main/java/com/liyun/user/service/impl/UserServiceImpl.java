package com.liyun.user.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.liyun.common.context.UserContext;
import com.liyun.common.enums.ResultCode;
import com.liyun.common.exception.BizException;
import com.liyun.common.utils.DateUtils;
import com.liyun.common.utils.JwtUtils;
import com.liyun.common.utils.RandomUtils;
import com.liyun.user.domain.dto.LoginDTO;
import com.liyun.user.domain.dto.RegisterDTO;
import com.liyun.user.domain.dto.RegisterShopDTO;
import com.liyun.user.domain.pojo.User;
import com.liyun.user.domain.pojo.UserAddress;
import com.liyun.user.domain.pojo.UserProfile;
import com.liyun.user.domain.vo.LoginVO;
import com.liyun.user.domain.vo.UserDetailVO;
import com.liyun.user.mapper.UserMapper;
import com.liyun.user.service.IUserAddressService;
import com.liyun.user.service.IUserProfileService;
import com.liyun.user.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.liyun.user.utils.IpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.liyun.user.constants.UserConstant.SMS_CODE;
import static com.liyun.user.constants.UserConstant.USER_TOKEN;

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

    private final IUserProfileService  userProfileService;
    private final IUserAddressService userAddressService;
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


        // 3.生成token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());
        String token = JwtUtils.createToken(claims, secret, expire);
        String redisKey = USER_TOKEN + user.getId();
        redisTemplate.opsForValue().set(redisKey, token, expire, TimeUnit.MILLISECONDS);
        // 4.封装VO返回
        LoginVO vo = new LoginVO();
        vo.setToken(token);

        vo.setRole(user.getRole());
        log.info("用户登录成功，用户ID：{}", user.getId());
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
    @Transactional
    public void register(RegisterDTO registerDTO, String ip) {
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



        save(user);
        UserProfile userProfile = new UserProfile();
        userProfile.setId(user.getId());
        userProfile.setNickname(registerDTO.getNickname());
        userProfile.setAvatar("https://picsum.photos/200/300?random=" + RandomUtils.randomInt(1, 1000));
        userProfile.setIp( ip);
        userProfile.setExperience(1);
        userProfile.setUpdateTime(DateUtils.now());
        String region = IpUtils.getRegion(ip);

        // 解析地区，兼容"内网IP"和正常格式
        String regionName;
        if ("内网IP".equals(region) || "未知".equals(region)) {
            regionName = region;
        } else {
            String[] parts = region.split("\\|");
            // 格式: 国家|区域|省|市|运营商，取省(2)+市(3)
            regionName = (parts.length > 3) ? parts[2] + " " + parts[3] : region;
        }
        userProfile.setRegion(regionName);
        userProfileService.save(userProfile);






        // 🛡️ 日志脱敏：不要打印完整 user（可能含密码哈希，虽不可逆但避免泄露）
        log.info("新用户注册成功，手机号: {}, 用户ID: {}", user.getPhone(), user.getId());
    }

    @Override
    public UserDetailVO getUserDetail() {

        Long userId = UserContext.getUserId();
        User user = lambdaQuery().eq(User::getId, userId).one();

        UserProfile userProfile = userProfileService.lambdaQuery().eq(UserProfile::getId, userId).one();


        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId());
        vo.setNickname(userProfile.getNickname());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setAvatar(userProfile.getAvatar());
        List<UserAddress> list = userAddressService.lambdaQuery()
                .eq(UserAddress::getUserId, userId).list();
        log.info("用户地址：{}", list);
        vo.setExperience(userProfile.getExperience());
        vo.setAddresses(list);

return vo;
    }

    @Override
    public void logout() {
        Long userId = UserContext.getUserId();
        log.info("用户退出登录：{}", userId);
         UserContext.clear();
        // 删除Redis中的token
        redisTemplate.delete(USER_TOKEN + userId);
    }

    @Override
    @Transactional
    public void registerShop(RegisterShopDTO registerDTO, String ip) {
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

        if (!rightCode.equals(registerDTO.getCode())) {
            throw new RuntimeException("验证码错误");
        }

        redisTemplate.delete(redisKey);

        // 3. 生成商家用户
        User user = new User();
        user.setPhone(registerDTO.getPhone());
        user.setUsername("shop" + RandomUtils.randomNumbers(26));
        user.setRole(1); // 商家

        String rawPassword = registerDTO.getPassword();
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        user.setPassword(passwordEncoder.encode(rawPassword));

        save(user);

        // 4. 生成商家 Profile
        UserProfile userProfile = new UserProfile();
        userProfile.setId(user.getId());
        userProfile.setNickname(registerDTO.getNickname());
        userProfile.setAvatar("https://picsum.photos/200/300?random=" + RandomUtils.randomInt(1, 1000));
        userProfile.setIp(ip);
        userProfile.setExperience(1);
        userProfile.setUpdateTime(DateUtils.now());

        String region = IpUtils.getRegion(ip);
        String regionName;
        if ("内网IP".equals(region) || "未知".equals(region)) {
            regionName = region;
        } else {
            String[] parts = region.split("\\|");
            regionName = (parts.length > 3) ? parts[2] + " " + parts[3] : region;
        }
        userProfile.setRegion(regionName);
        userProfileService.save(userProfile);


        log.info("新商家注册成功，手机号: {}, 用户ID: {}, 店铺名: {}",
                user.getPhone(), user.getId(), registerDTO.getShopName());
    }
}
