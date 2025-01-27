package com.hsj.hmdp.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsj.hmdp.pojo.Voucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VoucherMapper extends BaseMapper<Voucher> {

    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}
