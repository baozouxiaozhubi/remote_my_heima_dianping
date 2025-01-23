package com.hsj.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
//用于从前端接受登陆数据的类，账户是手机号，密码可以是验证码也可以是密码
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;
}
