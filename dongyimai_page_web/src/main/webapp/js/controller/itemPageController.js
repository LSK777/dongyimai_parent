app.controller('itemPageController',function ($scope,$http) {

    //点击+ 购买数量
    $scope.addNum = function (num) {
        $scope.num = $scope.num + num;
        //格式校验
        if ($scope.num<1){
            $scope.num = 1;
        }
    }

    //初始化规格列表的数据结构
    $scope.specification = {};
    $scope.selectSpecification = function (key, value) {
        $scope.specification[key] = value;
        searchSku();
    }

    //判定规格是否选中
    $scope.isSelect = function (key,value){
        if($scope.specification[key]==value){
            return true;
        }else{
            return false;
        }
    }

    $scope.loadSku = function () {
        $scope.sku = skuList[0];    //默认赋值第一个SKU信息
        $scope.specification = JSON.parse(JSON.stringify($scope.sku.spec));  //深克隆
    }

    searchSku = function (){
        //遍历SKU集合
        for(var i=0;i<skuList.length;i++){
            //根据选中的规格和SKU中的spec对象做比对，如果相等，则将该SKU的数据取出
            if(matchObject($scope.specification,skuList[i].spec)){
                $scope.sku =   skuList[i];
                return;
            }
        }
        //skuList  一个满足条件的数据都没有,默认赋值空值
        $scope.sku={'id':0,'title':'---','price':0,'spec':{}};
    }

    //比较两个MAP集合是否相等
    matchObject = function (map1,map2){
        for(var k in map1){
            if(map1[k]!=map2[k]){
                return false;
            }
        }
        for(var k in map2){
            if(map2[k]!=map1[k]){
                return false;
            }
        }
        return  true;
    }

    //加入购物车
    $scope.addItemCart = function (){
        //alert($scope.sku.id);
        $http.get('http://localhost:9108/cart/addGoodsToCartList.do?itemId='+$scope.sku.id+'&num='+$scope.num,{'withCredentials':true}).success(
            function (response) {
            if (response.success){
                location.href="http://localhost:9108/cart.html";
            }else{
                alert(response.message);
            }
        })
    }

})







