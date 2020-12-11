package com.offcn.search.service;


import com.offcn.pojo.TbItem;

import java.util.List;
import java.util.Map;

//SKU检索接口
public interface ItemSearchService {

    public Map<String,Object> search(Map<String,Object> searchMap);

    //导入SKU数据
    public void importItem(List<TbItem> itemList);

    //导入SKU数据
    public void deleteByGoodsId(List goodsIdList);

}









