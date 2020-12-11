package com.offcn.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.promeg.pinyinhelper.Pinyin;
import com.offcn.pojo.TbItem;
import com.offcn.search.service.ItemSearchService;
import org.apache.commons.lang3.StringUtils;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItemSearchServiceImpl implements ItemSearchService {

    @Autowired
    private SolrTemplate solrTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Map<String, Object> search(Map<String, Object> searchMap) {
        Map<String, Object> resultMap = new HashMap<>();

//        //1.创建查询对象
//        Query query = new SimpleQuery();
//        //2.创建查询条件选择器,并设置查询条件  //is  相当于进行分词查询
//        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
//        //3.将选择器放回到查询对象中
//        query.addCriteria(criteria);
//        //4.执行查询
//        ScoredPage<TbItem> page = solrTemplate.queryForPage(query,TbItem.class);
//        //5.取得查询结果集
//        List<TbItem> itemList = page.getContent();
//        resultMap.put("rows",itemList);

        //处理搜索关键字 去除空格
         if (StringUtils.isNotEmpty((String)searchMap.get("keywords"))&&((String)searchMap.get("keywords")).indexOf(" ")>-1){
             String keywords = (String)searchMap.get("keywords");
             keywords = keywords.replace(" ","");   //去除空格
             searchMap.put("keywords",keywords);
         }


        resultMap.putAll( this.searchList(searchMap));

        List<String> categoryList = this.findCategoryList(searchMap);

        resultMap.put("categoryList",categoryList);

        //如果分类查询条件有值,则需要根据分类进行检索品牌和规格,否则默认使用第一个查询条件
        if (StringUtils.isNotEmpty((String) searchMap.get("category"))){
            resultMap.putAll(this.findBrandAndSpecList((String) searchMap.get("category")));
        }else{
            if (categoryList.size()>0){
                resultMap.putAll(this.findBrandAndSpecList(categoryList.get(0)));
            }

        }

        return resultMap;
    }

    @Override
    public void importItem(List<TbItem> itemList) {
        for (TbItem item : itemList){
            System.out.println(item.getTitle()+"---"+item.getPrice());

            //取得规格属性
            Map<String,String> specMap = JSON.parseObject( item.getSpec(), Map.class);
            Map<String,String> pinyinMap = new HashMap<>();
            for (String key:specMap.keySet()){
                //完成拼音转换
                pinyinMap.put(Pinyin.toPinyin(key,"").toLowerCase(),specMap.get(key));
            }
            //重新放回到域
            item.setSpecMap(pinyinMap);
        }
        solrTemplate.saveBeans(itemList);
        solrTemplate.commit();
        System.out.println("导入成功");
    }

    @Override
    public void deleteByGoodsId(List goodsIdList) {
        Query query = new SimpleQuery();
        Criteria criteria = new Criteria("item_goodsid").in(goodsIdList);
        query.addCriteria(criteria);
        solrTemplate.delete(query);
        solrTemplate.commit();
        System.out.println("solr删除成功"+goodsIdList);

    }

    //高亮查询
    private Map<String, Object> searchList(Map<String, Object> searchMap){
        Map<String, Object> resultMap = new HashMap<>();
        //1.1. 创建高亮查询对象
        HighlightQuery query = new SimpleHighlightQuery();
        //1.2.设置高亮显示的字段
        HighlightOptions options = new HighlightOptions();
        options.addField("item_title");
        //1.3.设置高亮显示的属性
        options.setSimplePrefix("<em style='color: red'>");   //前缀
        options.setSimplePostfix("</em>");  //后缀
        query.setHighlightOptions(options);
        //1.4.设置查询条件
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);

        //2.1 根据分类筛选
        if(StringUtils.isNotEmpty((String)searchMap.get("category"))){
            //设置查询条件
            Criteria filterCriteria = new Criteria("item_category").is(searchMap.get("category"));
            //创建过滤查询对象
            FilterQuery filterQuery = new SimpleFacetQuery().addCriteria(filterCriteria);
            query.addFilterQuery(filterQuery);
        }

        //2.2 根据品牌筛选
        if (StringUtils.isNotEmpty((String)searchMap.get("brand"))){
            //设置查询条件
            Criteria filterCriteria = new Criteria("item_brand").is(searchMap.get("brand"));
            //创建过滤查询对象
            FilterQuery filterQuery = new SimpleFacetQuery().addCriteria(filterCriteria);
            query.addFilterQuery(filterQuery);
        }
        //2.3 根据规格筛选
        if (null != searchMap.get("spec")){
            //取得规格对象
            Map<String,String> specMap =(Map<String,String>) searchMap.get("spec");
            //根据规格对象,取得key的列表
            for (String key : searchMap.keySet()){
                //根据key 做拼音转换
                //执行过滤查询
                Criteria filterCriteria = new Criteria("item_spec_" + Pinyin.toPinyin(key,"").toLowerCase()).is(specMap.get(key));
                FilterQuery filterQuery = new SimpleFacetQuery().addCriteria(filterCriteria);
                query.addFilterQuery(filterQuery);
            }

        }

        //2.4价格查询
        if (StringUtils.isNotEmpty((String)searchMap.get("price"))){
            //根据 - 进行字符串拆分  500-1000 str[0]=500  str[1]=1000
            String[] str = ((String) searchMap.get("price")).split("-");
            if (!str[0].equals("0")){
                Criteria filterCriteria = new Criteria("item_price").greaterThanEqual(str[0]);
                FilterQuery filterQuery = new SimpleFacetQuery().addCriteria(filterCriteria);
                query.addFilterQuery(filterQuery);
            }
            if (!str[1].equals("*")){
                Criteria filterCriteria = new Criteria("item_price").lessThan(str[1]);
                FilterQuery filterQuery = new SimpleFacetQuery().addCriteria(filterCriteria);
                query.addFilterQuery(filterQuery);
            }
        }

        //2.5 分页
        Integer pageNo = (Integer) searchMap.get("pageNo");  //当前页码
        if (null==pageNo){
            pageNo = 1;     //默认查询第一页
        }
        Integer pageSize = (Integer) searchMap.get("pageSize");      //每页显示记录数
        if (null==pageSize){
            pageSize = 20;
        }

        query.setOffset((pageNo-1)*pageSize);   //查询起始记录数
        query.setRows(pageSize);                //要查询记录数


        //2.6 排序
        String sortValue = (String) searchMap.get("sort");    //排序规则
        String sortField = (String)searchMap.get("sortField"); //排序字段
        if (StringUtils.isNotEmpty(sortValue)){
            if (sortValue.equals("ASC")){
                Sort sort = new Sort(Sort.Direction.ASC,"item_"+sortField);
                query.addSort(sort);
            }
            if (sortValue.equals("DESC")){
                Sort sort = new Sort(Sort.Direction.DESC,"item_"+sortField);
                query.addSort(sort);
            }
        }


        //1.5.执行高亮显示的查询
        HighlightPage<TbItem> highlightPage = solrTemplate.queryForHighlightPage(query, TbItem.class);
        //1.6.取得高亮显示结果集的入口
        List<HighlightEntry<TbItem>> highlightList = highlightPage.getHighlighted();
        for (HighlightEntry<TbItem> highlightEntry : highlightList){
            TbItem tbItem = highlightEntry.getEntity();
            //注意  一定要对高亮显示的片段做判空操作
            if (highlightEntry.getHighlights().size()>0 && highlightEntry.getHighlights().get(0).getSnipplets().size()>0){
                //1.7.得到高亮显示的关键字
                List<HighlightEntry.Highlight> highlightEntries = highlightEntry.getHighlights();
                List<String> snippletList = highlightEntries.get(0).getSnipplets();
                //1.8.重新设置回SKU对象
                tbItem.setTitle(snippletList.get(0));
            }

        }
        //1.9.返回结果
        resultMap.put("rows",highlightPage.getContent());
        resultMap.put("total",highlightPage.getTotalElements());    //总记录数
        resultMap.put("totalPages",highlightPage.getTotalPages());  //总页数

        return resultMap;
    }

    //查询分类集合
    public List<String> findCategoryList(Map<String, Object> searchMap){
        List<String> categoryList = new ArrayList<>();
        //1.创建查询对象
        Query query = new SimpleQuery();
        //2.设置分组字段
        GroupOptions groupOptions = new GroupOptions().addGroupByField("item_category");
        query.setGroupOptions(groupOptions);
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);
        //3.执行分组查询
        GroupPage<TbItem> groupPage = solrTemplate.queryForGroupPage(query,TbItem.class);
        //4.获得分组结果集 入口
        GroupResult<TbItem> groupResult = groupPage.getGroupResult("item_category");
        //取得分组结果入口页
        Page<GroupEntry<TbItem>> page = groupResult.getGroupEntries();
        List<GroupEntry<TbItem>> groupEntryList = page.getContent();
        for (GroupEntry<TbItem> groupEntry : groupEntryList){
            //取得分组的结果并放入重新定义的集合中
            categoryList.add(groupEntry.getGroupValue());
        }

        return categoryList;
    }

    private Map<String,Object> findBrandAndSpecList(String category){
        Map<String,Object> resultMap = new HashMap<>();
        //1.通过分类名称在缓存中查询模板ID
        Long typeId = (Long) redisTemplate.boundHashOps("itemCat").get(category);
        if (null!=typeId){
            //2.通过模板ID在缓存中查询品牌列表
            List<Map> brandList = (List<Map>) redisTemplate.boundHashOps("brandList").get(typeId);
            resultMap.put("brandList",brandList);

            //3.通过模板ID在缓存中查询规格列表
            List<Map> specList =(List<Map>) redisTemplate.boundHashOps("specList").get(typeId);
            resultMap.put("specList",specList);
        }
        return resultMap;
    }


}



















