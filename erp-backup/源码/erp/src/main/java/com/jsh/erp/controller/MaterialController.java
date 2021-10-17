package com.jsh.erp.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.DepotEx;
import com.jsh.erp.datasource.entities.Material;
import com.jsh.erp.datasource.entities.MaterialVo4Unit;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.depotItem.DepotItemService;
import com.jsh.erp.service.material.MaterialService;
import com.jsh.erp.utils.*;
import jxl.Sheet;
import jxl.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;

import static com.jsh.erp.utils.ResponseJsonUtil.returnJson;

/**
 * @author ji|sheng|hua 华夏ERP
 */
@RestController
@RequestMapping(value = "/material")
public class MaterialController {
    private Logger logger = LoggerFactory.getLogger(MaterialController.class);

    @Resource
    private MaterialService materialService;

    @Resource
    private DepotItemService depotItemService;

    @GetMapping(value = "/checkIsExist")
    public String checkIsExist(@RequestParam("id") Long id, @RequestParam("name") String name,
                               @RequestParam("model") String model, @RequestParam("color") String color,
                               @RequestParam("standard") String standard, @RequestParam("mfrs") String mfrs,
                               @RequestParam("otherField1") String otherField1, @RequestParam("otherField2") String otherField2,
                               @RequestParam("otherField3") String otherField3, @RequestParam("unit") String unit,@RequestParam("unitId") Long unitId,
                               HttpServletRequest request)throws Exception {
        Map<String, Object> objectMap = new HashMap<String, Object>();
        int exist = materialService.checkIsExist(id, name, model, color, standard, mfrs,
                otherField1, otherField2, otherField3, unit, unitId);
        if(exist > 0) {
            objectMap.put("status", true);
        } else {
            objectMap.put("status", false);
        }
        return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
    }

    /**
     * 批量设置状态-启用或者禁用
     * @param enabled
     * @param materialIDs
     * @param request
     * @return
     */
    @PostMapping(value = "/batchSetEnable")
    public String batchSetEnable(@RequestParam("enabled") Boolean enabled,
                                 @RequestParam("materialIDs") String materialIDs,
                                 HttpServletRequest request)throws Exception {
        Map<String, Object> objectMap = new HashMap<String, Object>();
        int res = materialService.batchSetEnable(enabled, materialIDs);
        if(res > 0) {
            return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
        } else {
            return returnJson(objectMap, ErpInfo.ERROR.name, ErpInfo.ERROR.code);
        }
    }

    /**
     * 根据id来查询商品名称
     * @param id
     * @param request
     * @return
     */
    @GetMapping(value = "/findById")
    public BaseResponseInfo findById(@RequestParam("id") Long id, HttpServletRequest request) throws Exception{
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            List<MaterialVo4Unit> list = materialService.findById(id);
            res.code = 200;
            res.data = list;
        } catch(Exception e){
            e.printStackTrace();
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }

    /**
     * 根据meId来查询商品名称
     * @param meId
     * @param request
     * @return
     */
    @GetMapping(value = "/findByIdWithBarCode")
    public BaseResponseInfo findByIdWithBarCode(@RequestParam("meId") Long meId, HttpServletRequest request) throws Exception{
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            MaterialVo4Unit mu = new MaterialVo4Unit();
            List<MaterialVo4Unit> list = materialService.findByIdWithBarCode(meId);
            if(list!=null && list.size()>0) {
                mu = list.get(0);
            }
            res.code = 200;
            res.data = mu;
        } catch(Exception e){
            e.printStackTrace();
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }

    /**
     * 查找商品信息-下拉框
     * @param mpList
     * @param request
     * @return
     */
    @GetMapping(value = "/findBySelect")
    public JSONObject findBySelect(@RequestParam(value = "q", required = false) String q,
                                  @RequestParam("mpList") String mpList,
                                  @RequestParam(value = "depotId", required = false) Long depotId,
                                  @RequestParam("page") Integer currentPage,
                                  @RequestParam("rows") Integer pageSize,
                                  HttpServletRequest request) throws Exception{
        JSONObject object = new JSONObject();
        try {
            Long tenantId = Long.parseLong(request.getSession().getAttribute("tenantId").toString());
            List<MaterialVo4Unit> dataList = materialService.findBySelectWithBarCode(q, (currentPage-1)*pageSize, pageSize);
            String[] mpArr = mpList.split(",");
            int total = materialService.findBySelectWithBarCodeCount(q);
            object.put("total", total);
            JSONArray dataArray = new JSONArray();
            //存放数据json数组
            if (null != dataList) {
                for (MaterialVo4Unit material : dataList) {
                    JSONObject item = new JSONObject();
                    item.put("Id", material.getMeId()); //商品扩展表的id
                    String ratio; //比例
                    if (material.getUnitid() == null || material.getUnitid().equals("")) {
                        ratio = "";
                    } else {
                        ratio = material.getUnitName();
                        ratio = ratio.substring(ratio.indexOf("("));
                    }
                    //品名/型号/扩展信息/包装
                    String MaterialName = "";
                    String mBarCode = "";
                    if(material.getmBarCode()!=null) {
                        mBarCode = material.getmBarCode();
                        MaterialName = MaterialName + mBarCode + "_";
                    }
                    item.put("mBarCode", mBarCode);
                    MaterialName = MaterialName + " " + material.getName()
                            + ((material.getStandard() == null || material.getStandard().equals("")) ? "" : "(" + material.getStandard() + ")")
                            + ((material.getModel() == null || material.getModel().equals("")) ? "" : "(" + material.getModel() + ")");
                    String expand = ""; //扩展信息
                    for (int i = 0; i < mpArr.length; i++) {
                        if (mpArr[i].equals("制造商")) {
                            expand = expand + ((material.getMfrs() == null || material.getMfrs().equals("")) ? "" : "(" + material.getMfrs() + ")");
                        }
                        if (mpArr[i].equals("自定义1")) {
                            expand = expand + ((material.getOtherfield1() == null || material.getOtherfield1().equals("")) ? "" : "(" + material.getOtherfield1() + ")");
                        }
                        if (mpArr[i].equals("自定义2")) {
                            expand = expand + ((material.getOtherfield2() == null || material.getOtherfield2().equals("")) ? "" : "(" + material.getOtherfield2() + ")");
                        }
                        if (mpArr[i].equals("自定义3")) {
                            expand = expand + ((material.getOtherfield3() == null || material.getOtherfield3().equals("")) ? "" : "(" + material.getOtherfield3() + ")");
                        }
                    }
                    MaterialName = MaterialName + expand + ((material.getCommodityUnit() == null || material.getCommodityUnit().equals("")) ? "" : "(" + material.getCommodityUnit() + ")") + ratio;
                    item.put("MaterialName", MaterialName);
                    item.put("name", material.getName());
                    item.put("expand", expand);
                    item.put("model", material.getModel());
                    item.put("standard", material.getStandard());
                    item.put("unit", material.getCommodityUnit() + ratio);
                    if(depotId!=null&& StringUtil.isNotEmpty(q)) {
                        BigDecimal stock = depotItemService.getStockByParam(depotId,material.getId(),null,null,tenantId);
                        item.put("stock", stock);
                    }
                    dataArray.add(item);
                }
            }
            object.put("rows", dataArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return object;
    }


    /**
     * 查找商品信息-统计排序
     * @param request
     * @return
     */
    @GetMapping(value = "/findByOrder")
    public BaseResponseInfo findByOrder(HttpServletRequest request)throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            List<Material> dataList = materialService.findByOrder();
            String mId = "";
            if (null != dataList) {
                for (Material material : dataList) {
                    mId = mId + material.getId() + ",";
                }
            }
            if (mId != "") {
                mId = mId.substring(0, mId.lastIndexOf(","));
            }
            map.put("mIds", mId);
            res.code = 200;
            res.data = map;
        } catch(Exception e){
            e.printStackTrace();
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }

    /**
     * 根据商品id查找商品信息
     * @param meId
     * @param request
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/getMaterialByMeId")
    public JSONObject getMaterialByMeId(@RequestParam(value = "meId", required = false) Long meId,
                                        @RequestParam("mpList") String mpList,
                                        HttpServletRequest request) throws Exception{
        JSONObject item = new JSONObject();
        try {
            String[] mpArr = mpList.split(",");
            List<MaterialVo4Unit> materialList = materialService.getMaterialByMeId(meId);
            if(materialList!=null && materialList.size()!=1) {
                return item;
            } else if(materialList.size() == 1) {
                MaterialVo4Unit material = materialList.get(0);
                item.put("Id", material.getMeId()); //商品扩展表的id
                String ratio; //比例
                if (material.getUnitid() == null || material.getUnitid().equals("")) {
                    ratio = "";
                } else {
                    ratio = material.getUnitName();
                    ratio = ratio.substring(ratio.indexOf("("));
                }
                //品名/型号/扩展信息/包装
                String MaterialName = "";
                MaterialName = MaterialName + material.getmBarCode() + "_" + material.getName()
                        + ((material.getStandard() == null || material.getStandard().equals("")) ? "" : "(" + material.getStandard() + ")");
                String expand = ""; //扩展信息
                for (int i = 0; i < mpArr.length; i++) {
                    if (mpArr[i].equals("颜色")) {
                        expand = expand + ((material.getColor() == null || material.getColor().equals("")) ? "" : "(" + material.getColor() + ")");
                    }
                    if (mpArr[i].equals("制造商")) {
                        expand = expand + ((material.getMfrs() == null || material.getMfrs().equals("")) ? "" : "(" + material.getMfrs() + ")");
                    }
                    if (mpArr[i].equals("自定义1")) {
                        expand = expand + ((material.getOtherfield1() == null || material.getOtherfield1().equals("")) ? "" : "(" + material.getOtherfield1() + ")");
                    }
                    if (mpArr[i].equals("自定义2")) {
                        expand = expand + ((material.getOtherfield2() == null || material.getOtherfield2().equals("")) ? "" : "(" + material.getOtherfield2() + ")");
                    }
                    if (mpArr[i].equals("自定义3")) {
                        expand = expand + ((material.getOtherfield3() == null || material.getOtherfield3().equals("")) ? "" : "(" + material.getOtherfield3() + ")");
                    }
                }
                MaterialName = MaterialName + expand + ((material.getUnit() == null || material.getUnit().equals("")) ? "" : "(" + material.getUnit() + ")") + ratio;
                item.put("MaterialName", MaterialName);
                item.put("name", material.getName());
                item.put("expand", expand);
                item.put("model", material.getModel());
                item.put("standard", material.getStandard());
                item.put("unit", material.getUnit() + ratio);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return item;
    }

    /**
     * 生成excel表格
     * @param name
     * @param model
     * @param categoryIds
     * @param request
     * @param response
     * @return
     */
    @GetMapping(value = "/exportExcel")
    public void exportExcel(@RequestParam("name") String name,
                                        @RequestParam("model") String model,
                                        @RequestParam("categoryIds") String categoryIds,
                                        HttpServletRequest request, HttpServletResponse response) {
        try {
            List<MaterialVo4Unit> dataList = materialService.findByAll(StringUtil.toNull(name), StringUtil.toNull(model),
                    StringUtil.toNull(categoryIds));
            String[] names = {"品名", "类型", "型号", "安全存量", "单位", "零售价", "最低售价", "预计采购价", "批发价", "备注", "状态"};
            String title = "商品信息";
            List<String[]> objects = new ArrayList<String[]>();
            if (null != dataList) {
                for (MaterialVo4Unit m : dataList) {
                    String[] objs = new String[11];
                    objs[0] = m.getName();
                    objs[1] = m.getCategoryName();
                    objs[2] = m.getModel();
                    objs[3] = m.getSafetystock() == null? "" : m.getSafetystock().toString();
                    objs[4] = m.getUnit();
                    objs[5] = m.getRetailprice() == null ? "" : m.getRetailprice().toString();
                    objs[6] = m.getLowprice() == null ? "" : m.getLowprice().toString();
                    objs[7] = m.getPresetpriceone() == null ? "" : m.getPresetpriceone().toString();
                    objs[8] = m.getPresetpricetwo() == null ? "" : m.getPresetpricetwo().toString();
                    objs[9] = m.getRemark();
                    objs[10] = m.getEnabled() ? "启用" : "禁用";
                    objects.add(objs);
                }
            }
            File file = ExcelUtils.exportObjectsWithoutTitle(title, names, title, objects);
            ExportExecUtil.showExec(file, file.getName(), response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * excel表格导入产品（含初始库存）
     * @param materialFile
     * @param request
     * @param response
     * @return
     */
    @PostMapping(value = "/importExcel")
    public void importExcel(MultipartFile materialFile,
                            HttpServletRequest request, HttpServletResponse response) throws Exception{
        BaseResponseInfo info = new BaseResponseInfo();
        Map<String, Object> data = new HashMap<String, Object>();
        String message = "成功";
        try {
            Sheet src = null;
            //文件合法性校验
            try {
                Workbook workbook = Workbook.getWorkbook(materialFile.getInputStream());
                src = workbook.getSheet(0);
            } catch (Exception e) {
                message = "导入文件不合法，请检查";
                data.put("message", message);
                info.code = 400;
                info.data = data;
            }
            info = materialService.importExcel(src);
        } catch (Exception e) {
            e.printStackTrace();
            message = "导入失败";
            info.code = 500;
            data.put("message", message);
            info.data = data;
        }
        response.sendRedirect("../pages/materials/material.html");
    }

    public BigDecimal parseBigDecimalEx(String str)throws Exception{
        if(!StringUtil.isEmpty(str)) {
            return  new BigDecimal(str);
        } else {
            return null;
        }
    }
    @RequestMapping(value = "/getMaterialEnableSerialNumberList")
    public String getMaterialEnableSerialNumberList(@RequestParam(value = Constants.PAGE_SIZE, required = false) Integer pageSize,
                               @RequestParam(value = Constants.CURRENT_PAGE, required = false) Integer currentPage,
                               @RequestParam(value = Constants.SEARCH, required = false) String search)throws Exception {
        Map<String, Object> parameterMap = new HashMap<String, Object>();
        //查询参数
        JSONObject obj=JSON.parseObject(search);
        Set<String> key= obj.keySet();
        for(String keyEach: key){
            parameterMap.put(keyEach,obj.getString(keyEach));
        }
        PageQueryInfo queryInfo = new PageQueryInfo();
        Map<String, Object> objectMap = new HashMap<String, Object>();
        if (pageSize == null || pageSize <= 0) {
            pageSize = BusinessConstants.DEFAULT_PAGINATION_PAGE_SIZE;
        }
        if (currentPage == null || currentPage <= 0) {
            currentPage = BusinessConstants.DEFAULT_PAGINATION_PAGE_NUMBER;
        }
        PageHelper.startPage(currentPage,pageSize,true);
        List<Material> list = materialService.getMaterialEnableSerialNumberList(parameterMap);
        //获取分页查询后的数据
        PageInfo<Material> pageInfo = new PageInfo<>(list);
        objectMap.put("page", queryInfo);
        if (list == null) {
            queryInfo.setRows(new ArrayList<Object>());
            queryInfo.setTotal(BusinessConstants.DEFAULT_LIST_NULL_NUMBER);
            return returnJson(objectMap, "查找不到数据", ErpInfo.OK.code);
        }
        queryInfo.setRows(list);
        queryInfo.setTotal(pageInfo.getTotal());
        return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
    }
    /**
     * create by: qiankunpingtai
     * website：https://qiankunpingtai.cn
     * description:
     *  批量删除商品信息
     * create time: 2019/3/29 11:15
     * @Param: ids
     * @return java.lang.Object
     */
    @RequestMapping(value = "/batchDeleteMaterialByIds")
    public Object batchDeleteMaterialByIds(@RequestParam("ids") String ids,@RequestParam(value="deleteType",
            required =false,defaultValue= BusinessConstants.DELETE_TYPE_NORMAL)String deleteType) throws Exception {
        JSONObject result = ExceptionConstants.standardSuccess();
        int i=0;
        if(BusinessConstants.DELETE_TYPE_NORMAL.equals(deleteType)){
            i= materialService.batchDeleteMaterialByIdsNormal(ids);
        }else if(BusinessConstants.DELETE_TYPE_FORCE.equals(deleteType)){
            i= materialService.batchDeleteMaterialByIds(ids);
        }else{
            logger.error("异常码[{}],异常提示[{}],参数,ids[{}],deleteType[{}]",
                    ExceptionConstants.DELETE_REFUSED_CODE,ExceptionConstants.DELETE_REFUSED_MSG,ids,deleteType);
            throw new BusinessRunTimeException(ExceptionConstants.DELETE_REFUSED_CODE,
                    ExceptionConstants.DELETE_REFUSED_MSG);
        }
        if(i<1){
            logger.error("异常码[{}],异常提示[{}],参数,ids[{}]",
                    ExceptionConstants.MATERIAL_DELETE_FAILED_CODE,ExceptionConstants.MATERIAL_DELETE_FAILED_MSG,ids);
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_DELETE_FAILED_CODE,
                    ExceptionConstants.MATERIAL_DELETE_FAILED_MSG);
        }
        return result;
    }

    @GetMapping(value = "/getMaxBarCode")
    public BaseResponseInfo getMaxBarCode() throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<String, Object>();
        String barCode = materialService.getMaxBarCode();
        map.put("barCode", barCode);
        res.code = 200;
        res.data = map;
        return res;
    }
}
