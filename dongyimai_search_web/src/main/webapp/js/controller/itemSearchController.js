app.controller('itemSearchController',function ($scope, $location,itemSearchService) {
    
    $scope.search = function () {
        $scope.searchMap.pageNo = parseInt($scope.searchMap.pageNo);  //确认将当前页码转换成数值型才能使用

        itemSearchService.search($scope.searchMap).success(
            function (response) {
            $scope.resultMap = response;
            buildPageLabel();
        })
    }

    //初始化查询条件对象的数据结构
    $scope.searchMap = {'keywords':'','category':'','brand':'','spec':{},'price':'','pageNo':1,'pageSize':20,'sort':'','sortField':''};

    //添加查询条件  key 查询条件  value 查询的值
    $scope.addSearchItem = function (key, value) {
        if (key == 'category' || key == 'brand'||key=='price'){
            $scope.searchMap[key] = value;
        }else{
            $scope.searchMap.spec[key] = value;
        }
        //重置查询起始页码为1
        $scope.searchMap.pageNo=1;
        //执行查询
        $scope.search();
    }

    //撤销查询条件
    $scope.removeSearchItem = function (key) {
        if (key == 'category' || key == 'brand'||key=='price'){
            $scope.searchMap[key] = '';
        }else{
            //移除JSON对象中的属性
            delete $scope.searchMap.spec[key];
        }
        //重置查询起始页码为1
        $scope.searchMap.pageNo=1;
        //执行查询
        $scope.search();
    }

    //构建页码集合
    buildPageLabel = function () {
        $scope.pageLabel = [];
        var firstPage = 1;                            //起始页码元素
        var lastPage = $scope.resultMap.totalPages;  //结束页码元素
        var maxPage = $scope.resultMap.totalPages;   //最大页码
        $scope.firstDot = true;
        $scope.lastDot = true;

        if (maxPage>5){
            if ($scope.searchMap.pageNo<3){
                lastPage = 5;                               //当前页码小于前3页时,需要固定结束页码元素
                $scope.firstDot = false;
            }else if ($scope.searchMap.pageNo>lastPage-2){
                firstPage = maxPage-4;                     //当前页码大于结束页-2时,需要固定起始页码元素
                $scope.lastDot = false;
            }else {
                firstPage = $scope.searchMap.pageNo-2;
                lastPage = $scope.searchMap.pageNo+2;
            }
        }else{
            $scope.firstDot = false;
            $scope.lastDot = false;
        }

        for(var i= firstPage;i<=lastPage;i++){
            $scope.pageLabel.push(i);
        }
    }

    //提交页码查询
    $scope.queryByPage = function (pageNo) {
        //页码格式校验
        if (pageNo<1 || pageNo>$scope.resultMap.totalPages){
            return;
        }
        //对当前页码进行赋值
        $scope.searchMap.pageNo = pageNo;
        //执行查询
        $scope.search();
    }

    //判断当前页是否是第一页
    $scope.isTopPage = function () {
        if ($scope.searchMap.pageNo==1){
            return true;
        }else{
            return false;
        }
    }

    //判断搜索页码是否是当前页
    $scope.isPage = function (page) {
        if (parseInt($scope.searchMap.pageNo)==parseInt(page)){
            return true;
        }else{
            return false;
        }
    }

    //排序查询
    $scope.sortSearch = function (sortField, sort) {
        $scope.searchMap.sortField = sortField;
        $scope.searchMap.sort = sort;
        //执行查询
        $scope.search();
    }

    $scope.keywordsIsBrand = function () {
        for (var i=0;i<$scope.resultMap.brandList.length;i++){
            if ($scope.searchMap.keywords.indexOf($scope.resultMap.brandList[i].text)>-1){
                return true;
            }
        }
        return false;
    }

    $scope.loadKeywords = function () {
        $scope.searchMap.keywords = $location.search()['keywords'];
        //执行查询
        $scope.search();
    }

})




















