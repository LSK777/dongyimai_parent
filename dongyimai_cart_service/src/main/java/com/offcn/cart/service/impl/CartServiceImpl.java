package com.offcn.cart.service.impl;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.config.annotation.Service;
import com.offcn.cart.service.CartService;
import com.offcn.group.Cart;
import com.offcn.mapper.TbItemMapper;
import com.offcn.pojo.TbItem;
import com.offcn.pojo.TbOrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private TbItemMapper itemMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public List<Cart> addGoodsToCartList(List<Cart> srcCartList, Long itemId, Integer num) {
        //1.根据itemId查询SKU信息
        TbItem item = itemMapper.selectByPrimaryKey(itemId);
        if (item==null){  //对商品判断是否为空
            throw  new RuntimeException("该商品不存在");
        }
        if (!item.getStatus().equals("1")){  //判断商品审核状态
            throw  new RuntimeException("该商品未经审核");
        }
        String sellerId = item.getSellerId();  //商家ID
        String sellerName = item.getSeller();  //商家名称
        //2.构建购物车对象
        Cart cart = this.searchCartBySellerId(srcCartList,sellerId);
        if (cart==null){  //如果购物车对象为空,则新建购物车对象
            cart = new Cart();
            cart.setSellerId(sellerId);
            cart.setSellerName(sellerName);
            TbOrderItem orderItem = this.createOrderItem(item,num);  //获得订单详情对象
            List<TbOrderItem> orderItemList = new ArrayList<>();
            orderItemList.add(orderItem);
            cart.setOrderItemList(orderItemList);
            //重新放入原购物车集合
            srcCartList.add(cart);

        }else {  //如果购物车对象存在
            //判断该商品是否在该商家的购物车存在
            TbOrderItem orderItem = this.searchOrderItemByItemId(cart.getOrderItemList(),itemId);
            if (orderItem==null){  //如果不存在,则重新创建orderItem对象
                orderItem = this.createOrderItem(item,num);
                cart.getOrderItemList().add(orderItem);  //重新放入购物车
            }else{  // 如果存在,则修改购买数量
                orderItem.setNum(orderItem.getNum()+num);   //修改数量
                orderItem.setTotalFee(new BigDecimal(orderItem.getNum()).multiply(item.getPrice()));  //重新计算总金额

                //判断订单详情集合中元素是否为0,如果为0,则在购物车中移除
                if (orderItem.getNum()==0){
                    cart.getOrderItemList().remove(orderItem);
                }
                //判断商检的购物车中的订单详情集合中的元素是否为0,如果为0,则将该购物车移除
                if (cart.getOrderItemList().size()==0){
                    srcCartList.remove(cart);
                }
            }

        }

        return srcCartList;
    }

    @Override
    public List<Cart> findCartListFromRedis(String username) {
        List<Cart> cartList =(List<Cart>) redisTemplate.boundHashOps("cartList").get(username);
        if (CollectionUtils.isEmpty(cartList)){
            cartList = new ArrayList<>();
        }

        return cartList;
    }

    @Override
    public void saveCartListToRedis(String username, List<Cart> cartList) {
        redisTemplate.boundHashOps("cartList").put(username,cartList);
        System.out.println("向缓存中添加购物车成功");
    }

    @Override
    public List<Cart> margeCartList(List<Cart> cartList_cookie, List<Cart> cartList_redis) {
        //遍历cookie的购物车集合
        for (Cart cart:cartList_cookie){
            for (TbOrderItem orderItem:cart.getOrderItemList()){
                //执行添加购物车
                cartList_redis = this.addGoodsToCartList(cartList_redis,orderItem.getItemId(),orderItem.getNum());
            }
        }
        return cartList_redis;
    }

    //根据sellerID判断购物车集合中是否存在该商家的购物车对象
    private Cart searchCartBySellerId(List<Cart> cartList,String sellerId){
        for (Cart cart:cartList){
            if (sellerId.equals(cart.getSellerId())){  //如果sellerID相等,说明购物车对象存在
                return cart;
            }
        }
        return null;
    }

    //创建订单详情对象
    private TbOrderItem createOrderItem(TbItem item,Integer num){
        if (num<1){
            throw new RuntimeException("购买数量不合法");
        }
        TbOrderItem orderItem = new TbOrderItem();
        orderItem.setItemId(item.getId());         //SKU id
        orderItem.setGoodsId(item.getGoodsId());  //SPU id
        orderItem.setNum(num);                    //购买数量
        orderItem.setPrice(item.getPrice());      //商品单价
        orderItem.setTotalFee(new BigDecimal(num).multiply(item.getPrice()));  //总价格
        orderItem.setTitle(item.getTitle());     //商品标题
        orderItem.setPicPath(item.getImage());   //商品图片
        orderItem.setSellerId(item.getSellerId());//商家ID
        return orderItem;
    }

    //根据SKUid判断该商家的订单详情集合中是否存在该商品
    private TbOrderItem searchOrderItemByItemId(List<TbOrderItem> orderItemList,Long itemId){
        for (TbOrderItem orderItem:orderItemList){
            if (orderItem.getItemId().longValue()==itemId.longValue()){
                return orderItem;
            }
        }
        return null;
    }

}











