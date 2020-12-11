package com.offcn.page.service;

public interface ItemPageService {

    //生成商品详情页的静态页面
    public boolean genItemPage(Long goodsId);

    //删除商品详情页
    public boolean deleteItemHtml(Long[] ids);
}
