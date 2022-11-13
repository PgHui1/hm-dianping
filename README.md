# hm-dianping
黑马发布的redis教程
https://www.bilibili.com/video/BV1cr4y1671t

## 6、秒杀优化
### 6.1 秒杀优化-异步秒杀思路
将下单资格的判断在redis中完成,因此需要做以下改造:
在添加新的优惠券的时候，将该优惠券的库存保存到redis中，
业务开始时先查询redis中商品库存，如果大于0，执行之后的流程
之后的流程需要判断用户是否购买过，这里采用set存储
业务流程如下
![image](https://user-images.githubusercontent.com/55320213/201508685-f92868e3-9251-40bd-b680-4df65faadb70.png)

