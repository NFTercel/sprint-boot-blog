package hello.controller;

import hello.entity.LoginResult;
import hello.entity.User;
import hello.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import java.util.Map;

@Controller
public class AuthController {
    private UserService userService;
    private AuthenticationManager authenticationManager;

    @Inject
    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/auth")
    @ResponseBody
    public LoginResult auth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User loggerInUser = userService.getUserByUsername(authentication == null ? null : authentication.getName());

        if (loggerInUser == null) {
            return LoginResult.statusIsOkAndLoginIsFalse("用户没有登录");
        } else {
            return LoginResult.successMsg(null, loggerInUser);
        }
    }

    @PostMapping("/auth/register")
    @ResponseBody
    public LoginResult register(@RequestBody Map<String, Object> usernameAndPassword) {
        String username = usernameAndPassword.get("username").toString();
        String password = usernameAndPassword.get("password").toString();

        if (username == null || password == null) {
            return LoginResult.failMsg("用户名或密码为空");
        }
        if (username.length() < 1 || username.length() > 15) {
            return LoginResult.failMsg("用户名不符合格式");
        }
        if (password.length() < 6 || password.length() > 15) {
            return LoginResult.failMsg("密码不符合格式");
        }

        try {
            userService.save(username, password);
        } catch (DuplicateKeyException e) {
            return LoginResult.failMsg("用户已经存在");
        }
        return LoginResult.statusIsOkAndLoginIsFalse("成功!");
    }

    @PostMapping("/auth/login")
    @ResponseBody
    public LoginResult login(@RequestBody Map<String, Object> usernameAndPassword) {
        String username = usernameAndPassword.get("username").toString();
        String password = usernameAndPassword.get("password").toString();

        UserDetails userDetails;
        try {
            userDetails = userService.loadUserByUsername(username);

        } catch (UsernameNotFoundException e) {
            return LoginResult.failMsg("用户不存在");
        }

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());

        try {
            authenticationManager.authenticate(token);
            //保存用户信息的位置
            //Cookie
            SecurityContextHolder.getContext().setAuthentication(token);

            return LoginResult.successMsg("登录成功", userService.getUserByUsername(username));
        } catch (BadCredentialsException e) {
            return LoginResult.failMsg("密码不正确");
        }
    }

    @GetMapping("/auth/logout")
    @ResponseBody
    public LoginResult logout() {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        User loggerInUser = userService.getUserByUsername(userName);

        if (loggerInUser == null) {
            return LoginResult.failMsg("用户没有登录");
        } else {
            SecurityContextHolder.clearContext();
            return LoginResult.statusIsOkAndLoginIsFalse("注销成功");
        }
    }

}
