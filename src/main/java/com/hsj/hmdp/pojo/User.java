package com.hsj.hmdp.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data //get set方法 tostring方法
@AllArgsConstructor //有参构造器
@NoArgsConstructor //无参构造器
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_user")
public class User {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id; //设置自增主键
    private String phone; //电话号码
    private String password; //密码
    private String nick_name; //昵称
    private String icon;//头像-对应的URL
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;//创建时间-自动填充
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;//更新时间-自动填充
}
