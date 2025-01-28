package com.hsj.hmdp.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hsj.hmdp.pojo.VoucherOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {
    @Update("UPDATE tb_seckill_voucher SET stock = stock- 1 WHERE voucher_id = #{voucherId} AND stock > 0")
    public int stock_minus(@Param("voucherId")Long voucherId);
}
