package com.hsj.hmdp.dto;

import lombok.Data;

import java.util.List;

//用来实现滚动分页的返回值
@Data
public class ScrollResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
