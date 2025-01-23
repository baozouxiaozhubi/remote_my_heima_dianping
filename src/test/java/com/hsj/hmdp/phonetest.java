package com.hsj.hmdp;

import com.hsj.hmdp.utils.RegexUtils;
import org.junit.jupiter.api.Test;


public class phonetest {
    @Test
    public void pt()
    {
        String phone="18050285529";
        System.out.println(RegexUtils.isPhoneInvalid(phone));
    }
}
