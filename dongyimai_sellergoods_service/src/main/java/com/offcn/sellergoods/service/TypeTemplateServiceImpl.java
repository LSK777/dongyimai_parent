package com.offcn.sellergoods.service;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.offcn.entity.PageResult;
import com.offcn.mapper.TbSpecificationOptionMapper;
import com.offcn.mapper.TbTypeTemplateMapper;
import com.offcn.pojo.TbSpecificationOption;
import com.offcn.pojo.TbSpecificationOptionExample;
import com.offcn.pojo.TbTypeTemplate;
import com.offcn.pojo.TbTypeTemplateExample;
import com.offcn.pojo.TbTypeTemplateExample.Criteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 服务实现层
 * @author Administrator
 *
 */
@Service
@Transactional
public class TypeTemplateServiceImpl implements TypeTemplateService {

	@Autowired
	private TbTypeTemplateMapper typeTemplateMapper;

	@Autowired
	private TbSpecificationOptionMapper specificationOptionMapper;

	@Autowired
	private RedisTemplate redisTemplate;
	
	/**
	 * 查询全部
	 */
	@Override
	public List<TbTypeTemplate> findAll() {
		return typeTemplateMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);		
		Page<TbTypeTemplate> page=   (Page<TbTypeTemplate>) typeTemplateMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 * 增加
	 */
	@Override
	public void add(TbTypeTemplate typeTemplate) {
		typeTemplateMapper.insert(typeTemplate);		
	}

	
	/**
	 * 修改
	 */
	@Override
	public void update(TbTypeTemplate typeTemplate){
		typeTemplateMapper.updateByPrimaryKey(typeTemplate);
	}	
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	@Override
	public TbTypeTemplate findOne(Long id){
		return typeTemplateMapper.selectByPrimaryKey(id);
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for(Long id:ids){
			typeTemplateMapper.deleteByPrimaryKey(id);
		}		
	}
	
	
		@Override
	public PageResult findPage(TbTypeTemplate typeTemplate, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		
		TbTypeTemplateExample example=new TbTypeTemplateExample();
		Criteria criteria = example.createCriteria();
		
		if(typeTemplate!=null){			
						if(typeTemplate.getName()!=null && typeTemplate.getName().length()>0){
				criteria.andNameLike("%"+typeTemplate.getName()+"%");
			}			if(typeTemplate.getSpecIds()!=null && typeTemplate.getSpecIds().length()>0){
				criteria.andSpecIdsLike("%"+typeTemplate.getSpecIds()+"%");
			}			if(typeTemplate.getBrandIds()!=null && typeTemplate.getBrandIds().length()>0){
				criteria.andBrandIdsLike("%"+typeTemplate.getBrandIds()+"%");
			}			if(typeTemplate.getCustomAttributeItems()!=null && typeTemplate.getCustomAttributeItems().length()>0){
				criteria.andCustomAttributeItemsLike("%"+typeTemplate.getCustomAttributeItems()+"%");
			}	
		}
		
		Page<TbTypeTemplate> page= (Page<TbTypeTemplate>)typeTemplateMapper.selectByExample(example);
		//注意:  放入缓存的方法需要放到分页查询之后执行
		this.saveToRedis();
		System.out.println("将品牌和规格放入缓存成功");
		return new PageResult(page.getTotal(), page.getResult());
	}

	@Override
	public List<Map> findSpecList(Long typeId) {
		//1.根据typeID查询模板对象
		TbTypeTemplate tbTypeTemplate = typeTemplateMapper.selectByPrimaryKey(typeId);
			//2.取得规格名称的信息
			//3.将规格名称的json结构的字符串转为json对象
			List<Map> specList = JSON.parseArray(tbTypeTemplate.getSpecIds(),Map.class);
			//4.遍历规格名称对象,根据规格名称ID查询谷歌选项
			if (!CollectionUtils.isEmpty(specList)){
				for (Map map: specList){
					//Map储存整型数据时,默认数据类型是int
					Long specId = new Long ((Integer)map.get("id"));
					//5.根据规格名称ID查询规格选项
					TbSpecificationOptionExample tbSpecificationOptionExample = new TbSpecificationOptionExample();
					TbSpecificationOptionExample.Criteria criteria = tbSpecificationOptionExample.createCriteria();
					criteria.andSpecIdEqualTo(specId);
					List<TbSpecificationOption> optionList = specificationOptionMapper.selectByExample(tbSpecificationOptionExample);
					//6.将规格选项重新设置回原有规格名称对象中
					map.put("options",optionList);
				}
			}
		return specList;
	}

	private void saveToRedis(){
		//1.查询所有模板列表
		List<TbTypeTemplate> templateList = this.findAll();
		//2.遍历模板列表
		for (TbTypeTemplate tbTypeTemplate : templateList){
			//3.根据模板ID,获得品牌列表对象
			List<Map> brandList = JSON.parseArray(tbTypeTemplate.getBrandIds(),Map.class);
			//4.将品牌列表放入缓存
			redisTemplate.boundHashOps("brandList").put(tbTypeTemplate.getId(),brandList);
			//5.根据模板ID 获得规格列表对象
			//List<Map> specList = JSON.parseArray(tbTypeTemplate.getSpecIds(), Map.class);
			List<Map> specList = this.findSpecList(tbTypeTemplate.getId());
			//6.将规格列表放入缓存
			redisTemplate.boundHashOps("specList").put(tbTypeTemplate.getId(),specList);
		}

	}

}













