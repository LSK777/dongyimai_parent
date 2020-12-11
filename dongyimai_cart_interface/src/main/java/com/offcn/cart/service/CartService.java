package com.offcn.cart.service;

import com.offcn.group.Cart;

import java.util.List;

//购物车接口
public interface CartService {

    //添加SKU商品到购物车集合
    //srcCartList 原购物车集合  itemId SKUid   num 购买数量
    public List<Cart> addGoodsToCartList(List<Cart> srcCartList,Long itemId,Integer num);

    //从缓存中获得当前登录人的购物车列表
    public List<Cart> findCartListFromRedis(String username);

    //根据当前登录人保存购物车集合到缓存
    public void saveCartListToRedis(String username,List<Cart> cartList);

    //合并购物车
    public List<Cart> margeCartList(List<Cart> cartList_cookie,List<Cart> cartList_redis);
}















