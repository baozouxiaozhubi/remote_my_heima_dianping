package com.hsj.hmdp.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsj.hmdp.pojo.VoucherOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {

    @Update("UPDATE tb_seckill_voucher SET stock = stock- 1 WHERE voucher_id = #{voucherId}")
    int stock_minus(@Param("voucherId") Long voucherId);

    //可能会出现多个线程同时读数据库读到stock>0，因此需要加上行级锁
    //SELECT stock FROM tb_seckill_voucher WHERE voucher_id = #{voucherId} FOR UPDATE;
    @Select("SELECT stock FROM tb_seckill_voucher WHERE voucher_id = #{voucherId} FOR UPDATE")
    int select_Stock_For_Update(@Param("voucherId") Long voucherId);
}
