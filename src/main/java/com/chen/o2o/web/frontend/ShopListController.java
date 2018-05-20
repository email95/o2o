package com.chen.o2o.web.frontend;

import com.chen.o2o.dto.ShopExecution;
import com.chen.o2o.entity.Area;
import com.chen.o2o.entity.Shop;
import com.chen.o2o.entity.ShopCategory;
import com.chen.o2o.service.AreaService;
import com.chen.o2o.service.ShopCategoryService;
import com.chen.o2o.service.ShopService;
import com.chen.o2o.util.HttpServletRequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/frontend")
public class ShopListController {
    @Autowired
    private AreaService areaService;
    @Autowired
    private ShopCategoryService shopCategoryService;
    @Autowired
    private ShopService shopService;

    @RequestMapping(value = "/listshopspageinfo",method = RequestMethod.GET)
    @ResponseBody
    private Map<String,Object> listShopsPageInfo(HttpServletRequest request){
        Map<String,Object> modelMap = new HashMap();
        //试着从前端请求中回去parentId
        long parentId = HttpServletRequestUtil.getLong(request,"parentId");
        List<ShopCategory> shopCategoryList = null;
        if(parentId!=-1){
            //如果parentId存在，则取出该一级shopCategory下的二级ShopCategory列表

            try {
                ShopCategory shopCategoryCondition = new ShopCategory();
                ShopCategory parent = new ShopCategory();
                parent.setShopCategoryId(parentId);
                shopCategoryList = shopCategoryService.getShopCategoryList(shopCategoryCondition);
            } catch (Exception e) {
                modelMap.put("success",false);
                modelMap.put("errMsg",e.getMessage());
            }
        }else{

            //如果parentId不存在，则取出所有一级shopCategory(用户在首页选择的是全部商店列表)
            try {
                shopCategoryList = shopCategoryService.getShopCategoryList(null);
            } catch (Exception e) {
                modelMap.put("success",false);
                modelMap.put("errMsg",e.getMessage());
            }

        }
        modelMap.put("shopCategoryList",shopCategoryList);
        try {
            //获取区域列表
            List<Area> areaList = areaService.getAreaList();
            modelMap.put("areaList",areaList);
            modelMap.put("success",true);
        } catch (Exception e) {
            modelMap.put("success",false);
            modelMap.put("errMsg",e.getMessage());
        }
        return modelMap;
    }

    @RequestMapping(value = "/listshops" ,method = RequestMethod.GET)
    @ResponseBody
    private Map<String,Object> listShops(HttpServletRequest request){
        Map<String ,Object> modelMap = new HashMap();
        //获取页码
        int pageIndex = HttpServletRequestUtil.getInt(request,"pageIndex");
        //获取一页所需要显示的数据条数
        int pageSize = HttpServletRequestUtil.getInt(request,"pageSize");
        //非空判断
        if((pageIndex>-1)&&(pageSize>-1)){
            //试着获取一级类别id
            long parentId = HttpServletRequestUtil.getLong(request,"parentId");
            //试着获取二级类别Id
            long shopCategoryId = HttpServletRequestUtil.getLong(request,"shopCategoryId");
            //试着获取区域id
            int areaId = HttpServletRequestUtil.getInt(request,"areaId");
            //试着获取模糊查询的名字
            String shopName = HttpServletRequestUtil.getString(request,"shopName");
            //获取组合之后的查询条件
            Shop shopCondition = compactShopCondition4Search(parentId,shopCategoryId,areaId,shopName);
            //根据查询条件和分页信息获取店铺列表，并返回总数
            ShopExecution shopExecution = shopService.getShopList(shopCondition,pageIndex,pageSize);
            modelMap.put("success",true);
            modelMap.put("count",shopExecution.getCount());
            modelMap.put("shopList",shopExecution.getShopList());

        }else{
            modelMap.put("success",false);
            modelMap.put("errMsg","empty pageSize or pageIndex");
        }
        return modelMap;

    }

    /**
     * 组合查询条件，并将条件封装到ShopCondition对象返回
     * @param parentId
     * @param shopCategoryId
     * @param areaId
     * @param shopName
     * @return
     */
    private Shop compactShopCondition4Search(long parentId, long shopCategoryId, int areaId, String shopName) {
        Shop shopCondition = new Shop();
        if(parentId!=-1L){
            //查询某一个一级ShopCategory下面所有二级ShopCategory里面的店铺列表
            ShopCategory childCategory = new ShopCategory();
            ShopCategory parentCategory = new ShopCategory();
            parentCategory.setShopCategoryId(parentId);
            childCategory.setParent(parentCategory);
            shopCondition.setShopCategory(childCategory);
        }
        if(shopCategoryId != -1L){
            //查询某个二级ShopCategory下面的店铺列表
            ShopCategory shopCategory = new ShopCategory();
            shopCategory.setShopCategoryId(shopCategoryId);
            shopCondition.setShopCategory(shopCategory);
        }
        if(areaId!= -1L){
            //查询位于某个区域Id下的店铺列表
            Area area = new Area();
            area.setAreaId(areaId);
            shopCondition.setArea(area);
        }
        if(shopName !=null){
            //查询名字包含shopName的店铺列表
            shopCondition.setShopName(shopName);
        }
        shopCondition.setEnableStatus(1);
        return shopCondition;
    }

}
