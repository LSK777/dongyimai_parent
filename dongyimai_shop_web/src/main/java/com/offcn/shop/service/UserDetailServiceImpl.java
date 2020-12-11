package com.offcn.shop.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.offcn.pojo.TbSeller;
import com.offcn.sellergoods.service.SellerService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.List;

//自定义认证类
public class UserDetailServiceImpl implements UserDetailsService {

    @Reference
    private SellerService sellerService;

    @Override
    public UserDetails loadUserByUsername(String sellerId) throws UsernameNotFoundException {
        //1.获得权限列表
        List<GrantedAuthority> authoritiyList = new ArrayList<>();
        authoritiyList.add(new SimpleGrantedAuthority("ROLE_SELLER"));
        //2.配置验证
        //根据sellerId查询商家对象
        TbSeller tbseller = sellerService.findOne(sellerId);
        if (null!=tbseller){
            //判断审核状态
            if (tbseller.getStatus().equals("1")){
                return new User(sellerId,tbseller.getPassword(),authoritiyList);
            }else{
                return null;
            }
        }else{
            return null;
        }


    }
}
