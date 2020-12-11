package com.offcn.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.offcn.cart.service.CartService;
import com.offcn.entity.Result;
import com.offcn.group.Cart;
import com.offcn.utils.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

//购物车的控制器
@RestController
@RequestMapping("/cart")
public class CartController {

    @Reference
    private CartService cartService;

    @RequestMapping("/findCartList")  //查询购物车的方法
    public List<Cart> findCartList(HttpServletRequest request, HttpServletResponse response){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String cartListStr = CookieUtil.getCookieValue(request, "cartList", "UTF-8");
        if (StringUtils.isEmpty(cartListStr)) {
            cartListStr = "[]";      //如果cookie中数据为空,则初始化数据结构
        }
        List cartList_cookie = JSON.parseArray(cartListStr, Cart.class);
        if (username.equals("anonymousUser")) {   //未登录
            //从cookie中得到购物车列表

            return cartList_cookie;
        }else{  //已登录 从缓存中获取购物车集合
            //1.从cookie中读取购物车集合

            //2.向redis中合并购物车
            List<Cart> cartList_redis = cartService.findCartListFromRedis(username);
            cartList_redis = cartService.margeCartList(cartList_cookie, cartList_redis);
            cartService.saveCartListToRedis(username,cartList_redis);
            //3.清空cookie
            CookieUtil.deleteCookie(request,response,"cartList");

            return cartList_redis;
        }

    }

    @RequestMapping("/addGoodsToCartList")   //添加购物车的方法
    @CrossOrigin(origins = "http://localhost:9105")
    public Result addGoodsToCartList(Long itemId, Integer num,HttpServletRequest request, HttpServletResponse response){
        try {
            //允许跨域请求传递
           // response.setHeader("Access-Control-Allow-Origin","http://localhost:9105");
            //允许跨域请求携带参数
           // response.setHeader("Access-Control-Allow-Credentials","true");

            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            //1.从cookie中取得购物车集合
            List<Cart> cartList = this.findCartList(request,response);
            //2.向购物车集合中添加商品
            cartList = cartService.addGoodsToCartList(cartList, itemId, num);
            if (username.equals("anonymousUser")) {  //未登录

                //3.重新更新cookie
                CookieUtil.setCookie(request,response,"cartList",JSON.toJSONString(cartList),24*3600,"UTF-8");
            } else{
                    cartService.saveCartListToRedis(username,cartList);
            }

            return new Result(true,"添加购物车成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false,"添加购物车异常");
        }
    }


}










