 //控制层 
app.controller('goodsController' ,function($scope,$controller,$location,goodsService,uploadService,itemCatService,typeTemplateService){
	
	$controller('baseController',{$scope:$scope});//继承
	
    //读取列表数据绑定到表单中  
	$scope.findAll=function(){
		goodsService.findAll().success(
			function(response){
				$scope.list=response;
			}			
		);
	}    
	
	//分页
	$scope.findPage=function(page,rows){			
		goodsService.findPage(page,rows).success(
			function(response){
				$scope.list=response.rows;	
				$scope.paginationConf.totalItems=response.total;//更新总记录数
			}			
		);
	}
	
	//查询实体 
	$scope.findOne=function(){
		//从地址路由接受参数
		var id = $location.search()['id'];
		if (id==null){
			return;
		}
		goodsService.findOne(id).success(
			function(response){
				$scope.entity= response;
				//富本文编辑器数据回显
				editor.html($scope.entity.goodsDesc.introduction);
				//图片列表回显
				$scope.entity.goodsDesc.itemImages = JSON.parse($scope.entity.goodsDesc.itemImages);
				//扩展属性的回显
				$scope.entity.goodsDesc.customAttributeItems = JSON.parse($scope.entity.goodsDesc.customAttributeItems);
				//规格回显
				$scope.entity.goodsDesc.specificationItems = JSON.parse($scope.entity.goodsDesc.specificationItems);
				//SKU中的规格选项回显
				for (var i=0;i<$scope.entity.itemList.length;i++){
					$scope.entity.itemList[i].spec  = JSON.parse($scope.entity.itemList[i].spec);
				}

			}
		);				
	}

	//判断规格复选框是否选中  specName 规格名称 optionValue  规格选项
	$scope.checkAttributeValue = function(specName,optionValue){

		var items = $scope.entity.goodsDesc.specificationItems;
		var object = $scope.searchObjectByKey(items,'attributeName',specName);
		if (object==null){  //没有规格对象
			return false;
		}else{
			//判断规格选项是否存在
			if (object.attributeValue.indexOf(optionValue)>-1){
				return true;
			}else {
				return false;
			}
		}
	}

	
	//保存 
	$scope.save=function(){
        //获取富文本编辑器的内容并设置
        $scope.entity.goodsDesc.introduction = editor.html();
		var serviceObject;//服务层对象  				
		if($scope.entity.goods.id!=null){//如果有ID
			serviceObject=goodsService.update( $scope.entity ); //修改  
		}else{
			serviceObject=goodsService.add( $scope.entity  );//增加 
		}				
		serviceObject.success(
			function(response){
				if(response.success){
					//重新查询 
		        	//$scope.reloadList();//重新加载
                    location.href='goods.html';
				}else{
					alert(response.message);
				}
			}		
		);				
	}
	
	 
	//批量删除 
	$scope.dele=function(){			
		//获取选中的复选框			
		goodsService.dele( $scope.selectIds ).success(
			function(response){
				if(response.success){
					$scope.reloadList();//刷新列表
					$scope.selectIds=[];
				}						
			}		
		);				
	}
	
	$scope.searchEntity={};//定义搜索对象 
	
	//搜索
	$scope.search=function(page,rows){			
		goodsService.search(page,rows,$scope.searchEntity).success(
			function(response){
				$scope.list=response.rows;	
				$scope.paginationConf.totalItems=response.total;//更新总记录数
			}			
		);
	}

	// //添加商品
	// $scope.add = function () {
	// 	//获取富文本编辑器的内容并设置
	// 	 $scope.entity.goodsDesc.introduction = editor.html();
	// 	goodsService.add($scope.entity).success(
	// 		function (response) {
	// 		if (response.success){
	// 			//清空表单
	// 			//$scope.entity = {};
	// 			$scope.entity = {'goods':{},'goodsDesc':{'itemImages':[],'specificationItems':[]}};
	// 			//清空富文本编辑器
	// 			editor.html('');
	// 		}else{
	// 			alert(response.message);
	// 		}
	// 	})
	// }

	//初始化上传图片表单对象的数据结构
	$scope.item_image_entity={};

	$scope.uploadFile = function () {
		uploadService.uploadFile().success(
			function (response) {
			if (response.success){
				//将图片回显
				$scope.item_image_entity.url = response.message;
			}else{
				alert(response.message);
			}
		}).error(function () {
			alert('上传图片发生错误');
		})
	}

	//初始化图片列表的数据结构
	$scope.entity = {'goods':{},'goodsDesc':{'itemImages':[],'specificationItems':[]}};
	//添加图片列表
	$scope.add_image_entity = function () {
		$scope.entity.goodsDesc.itemImages.push($scope.item_image_entity);
	}

	//删除图片列表
	$scope.delete_image_entity = function (index) {
		$scope.entity.goodsDesc.itemImages.splice(index,1);
	}

	//查询一级分类的下拉列表
	$scope.selectItemCat1List = function () {
		itemCatService.findByParentId(0).success(
			function (response) {
				$scope.itemCat1List = response;
		})
	}

	//查询二级分类的下拉列表
	$scope.$watch('entity.goods.category1Id',function (newValue, oldValue) {
		//如果newValue有值,则说明数据发生变化
		if (newValue){
			itemCatService.findByParentId(newValue).success(
				function (response) {
				$scope.itemCat2List = response;
			})
		}
	})

	//查询三级分类的下拉列表
	$scope.$watch('entity.goods.category2Id',function (newValue, oldValue) {
		//如果newValue有值,则说明数据发生变化
		if (newValue){
			itemCatService.findByParentId(newValue).success(
				function (response) {
					$scope.itemCat3List = response;
				})
		}
	})

	//查询模板ID
	$scope.$watch('entity.goods.category3Id',function (newValue, oldValue) {
		if (newValue){
			itemCatService.findOne(newValue).success(
				function (response) {
				$scope.entity.goods.typeTemplateId = response.typeId; //模板ID
			})
		}
	})

	//根据模板ID查询模板对象,并显示品牌下拉列表.同时还要取得扩展属性的信息
	$scope.$watch('entity.goods.typeTemplateId',function (newValue, oldValue) {
		if (newValue) {
			typeTemplateService.findOne(newValue).success(
				function (response) {
					$scope.typeTemplate = response;
					$scope.typeTemplate.brandIds = JSON.parse($scope.typeTemplate.brandIds);  //将json结构的字符串转换成json对象使用
					//获得扩展信息属性
					if ($location.search()['id']==null){
					$scope.entity.goodsDesc.customAttributeItems = JSON.parse($scope.typeTemplate.customAttributeItems);}
				})
			//查询规格多选框
			typeTemplateService.findSpecList(newValue).success(
				function (response) {
					$scope.specList = response;
				})
		}
	})

	//选中/反选规格选 项构造规格的数据结构
	//$event 选中/反选 ,name 规格名称 ,value 规格选项
	$scope.updateSpecAttribute = function ($event,name,value) {
		// 1.判断选中的规格对象是否在specificationItems中存在
		var object = $scope.searchObjectByKey($scope.entity.goodsDesc.specificationItems,"attributeName",name);
		// 2.如果不存在,则向specificationItems集合中执行push,放入规格对象
		if (object==null){
			$scope.entity.goodsDesc.specificationItems.push({"attributeName":name,"attributeValue":[value]});
		}else {
			// 3.如果存在,则判断复选框 选中/反选
			if ($event.target.checked){
				// 4.复选框选中 则attributeValue中push 规格选项的值
				object.attributeValue.push(value);
			}else {
				// 5.复选框反选 则attributeValue中splice 规格选项的值
				object.attributeValue.splice(object.attributeValue.indexOf(value),1);
				// 6.判断attributeValue中有没有元素,则specificationItems执行splice 移除规格对象
				if (object.attributeValue.length==0){
					$scope.entity.goodsDesc.specificationItems.splice($scope.entity.goodsDesc.specificationItems.indexOf(object),1);
				}
			}

		}

	}


	//
	$scope.createItemList = function () {
		//初始化SKU集合的数据结构
		$scope.entity.itemList = [{'price':0,'num':9999,'status':'0','isDefault':'0','spec':{}}];
		//2.遍历规格对象的集合
		var items = $scope.entity.goodsDesc.specificationItems;
		for (var i = 0;i<items.length;i++){

			//添加列
			$scope.entity.itemList = addColumn($scope.entity.itemList,items[i].attributeName,items[i].attributeValue);
		}
	}

	//itemList SKU对象集合  columnName 规格名称  columnValue 规格选项集合
	addColumn = function (itemList,columnName,columnValue) {
		var newList = [];
		for (var i = 0;i<itemList.length;i++){
			var oldRow = itemList[i];
			for (var j=0;j<columnValue.length;j++){
				var newRow = JSON.parse(JSON.stringify(oldRow)); //深克隆
				newRow.spec[columnName] = columnValue[j];
				newList.push(newRow);
			}
		}
		return newList;
	}

	//初始化一个状态值数组, 0未审核 1审核通过 2驳回 3关闭
	$scope.status = ['未审核','审核通过','驳回','关闭'];

	//初始化分类的数组
	$scope.categoryList = [];

	$scope.findCategoryList = function () {
		itemCatService.findAll().success(
			function (response) {
			for (var i=0;i<response.length;i++){
				$scope.categoryList[response[i].id] = response[i].name;
			}
		})
	}

});





























