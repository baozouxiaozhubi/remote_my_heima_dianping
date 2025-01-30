-- 1.参数列表
-- 1.1 优惠券Id
local voucherid = ARGV[1]
-- 1.1 优惠券Id
local userid = ARGV[2]

-- 2.数据KEY列表
-- 2.1 库存KEY
local stockKey = 'seckill:stock:' .. voucherid
-- 2.2 订单KEY
local orderKey = 'seckill:order:' .. voucherid

-- 3. 脚本业务
-- 3.1 判断库存是否小于等于0
if(tonumber(redis.call('get',stockKey))<=0) then
    --库存不足 返回1
    return 1
end
-- 3.2 判断用户是否下单，用SISMEMBER KEY SETNAME，存在返回1不存在返回0
if(redis.call('sismember',orderKey,userid) == 1) then
    --userid在SET中已经存在 说明已经下单返回2
    return 2
end

-- 3.3 扣库存
redis.call('incrby', stockKey, -1)
-- 3.4 下单，即把用户Id保存到集合中
redis.call('add', orderKey, userid)
return 0