package com.jsh.erp.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.*;
import com.jsh.erp.datasource.vo.DepotItemStockWarningCount;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.MaterialExtend.MaterialExtendService;
import com.jsh.erp.service.depotItem.DepotItemService;
import com.jsh.erp.service.material.MaterialService;
import com.jsh.erp.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.jsh.erp.utils.ResponseJsonUtil.returnJson;

/**
 * @author ji-sheng-hua 华夏erp
 */
@RestController
@RequestMapping(value = "/depotItem")
public class DepotItemController {
    private Logger logger = LoggerFactory.getLogger(DepotItemController.class);

    @Resource
    private DepotItemService depotItemService;

    @Resource
    private MaterialService materialService;

    @Resource
    private MaterialExtendService materialExtendService;

    /**
     * 只根据商品id查询单据列表
     * @param mId
     * @param request
     * @return
     */
    @GetMapping(value = "/findDetailByTypeAndMaterialId")
    public String findDetailByTypeAndMaterialId(
            @RequestParam(value = Constants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(value = Constants.CURRENT_PAGE, required = false) Integer currentPage,
            @RequestParam("materialId") String mId, HttpServletRequest request)throws Exception {
        Map<String, String> parameterMap = ParamUtils.requestToMap(request);
        parameterMap.put("mId", mId);
        PageQueryInfo queryInfo = new PageQueryInfo();
        Map<String, Object> objectMap = new HashMap<String, Object>();
        if (pageSize != null && pageSize <= 0) {
            pageSize = 10;
        }
        String offset = ParamUtils.getPageOffset(currentPage, pageSize);
        if (StringUtil.isNotEmpty(offset)) {
            parameterMap.put(Constants.OFFSET, offset);
        }
        List<DepotItemVo4DetailByTypeAndMId> list = depotItemService.findDetailByTypeAndMaterialIdList(parameterMap);
        JSONArray dataArray = new JSONArray();
        if (list != null) {
            for (DepotItemVo4DetailByTypeAndMId d: list) {
                JSONObject item = new JSONObject();
                item.put("Number", d.getNumber()); //商品编号
                item.put("Type", d.getNewtype()); //进出类型
                item.put("BasicNumber", d.getBnum()); //数量
                item.put("OperTime", d.getOtime()); //时间
                dataArray.add(item);
            }
        }
        objectMap.put("page", queryInfo);
        if (list == null) {
            queryInfo.setRows(new ArrayList<Object>());
            queryInfo.setTotal(BusinessConstants.DEFAULT_LIST_NULL_NUMBER);
            return returnJson(objectMap, "查找不到数据", ErpInfo.OK.code);
        }
        queryInfo.setRows(dataArray);
        queryInfo.setTotal(depotItemService.findDetailByTypeAndMaterialIdCounts(parameterMap));
        return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
    }

    /**
     * 根据商品id和仓库id查询库存数量
     * @param depotId
     * @param mId
     * @param request
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/findStockNumById")
    public BaseResponseInfo findStockNumById(
            @RequestParam("depotId") Long depotId,
            @RequestParam("mId") Long mId,
            HttpServletRequest request) throws Exception{
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            Long tenantId = Long.parseLong(request.getSession().getAttribute("tenantId").toString());
            map.put("stock", depotItemService.getStockByParam(depotId,mId,null,null,tenantId));
            res.code = 200;
            res.data = map;
        } catch (Exception e) {
            e.printStackTrace();
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }

    /**
     * 查询计量单位信息
     *
     * @return
     */
    public String findUnitName(Long mId)throws Exception {
        String unitName = "";
        try {
            unitName = materialService.findUnitName(mId);
            if (unitName != null) {
                unitName = unitName.substring(1, unitName.length() - 1);
                if (unitName.equals("null")) {
                    unitName = "";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return unitName;
    }

    @GetMapping(value = "/getDetailList")
    public BaseResponseInfo getDetailList(@RequestParam("headerId") Long headerId,
                              @RequestParam("mpList") String mpList,
                              HttpServletRequest request)throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<String, Object>();
        Long tenantId = null;
        if(request.getSession().getAttribute("tenantId")!=null) {
            tenantId = Long.parseLong(request.getSession().getAttribute("tenantId").toString());
        }
        try {
            List<DepotItemVo4WithInfoEx> dataList = new ArrayList<DepotItemVo4WithInfoEx>();
            if(headerId != 0) {
                dataList = depotItemService.getDetailList(headerId);
            }
            String[] mpArr = mpList.split(",");
            JSONObject outer = new JSONObject();
            outer.put("total", dataList.size());
            //存放数据json数组
            JSONArray dataArray = new JSONArray();
            if (null != dataList) {
                for (DepotItemVo4WithInfoEx diEx : dataList) {
                    JSONObject item = new JSONObject();
                    item.put("Id", diEx.getId());
                    item.put("MaterialExtendId", diEx.getMaterialExtendId() == null ? "" : diEx.getMaterialExtendId());
                    String ratio; //比例
                    if (diEx.getUnitId() == null || diEx.getUnitId().equals("")) {
                        ratio = "";
                    } else {
                        ratio = diEx.getUName();
                        ratio = ratio.substring(ratio.indexOf("("));
                    }
                    //品名/型号/扩展信息/包装
                    String MaterialName = diEx.getBarCode() + "_" + ((diEx.getMName() == null || diEx.getMName().equals("")) ? "" : diEx.getMName())
                            + ((diEx.getMStandard() == null || diEx.getMStandard().equals("")) ? "" : "(" + diEx.getMStandard() + ")")
                            + ((diEx.getMModel() == null || diEx.getMModel().equals("")) ? "" : "(" + diEx.getMModel() + ")");
                    String materialOther = getOtherInfo(mpArr, diEx);
                    MaterialName = MaterialName + materialOther + ((diEx.getUName() == null || diEx.getUName().equals("")) ? "" : "(" + diEx.getUName() + ")") + ratio;
                    item.put("MaterialName", MaterialName == null ? "" : MaterialName);
                    item.put("Stock", depotItemService.getStockByParam(diEx.getDepotid(),diEx.getMaterialid(),null,null,tenantId));
                    item.put("Unit", diEx.getMunit());
                    item.put("OperNumber", diEx.getOpernumber());
                    item.put("BasicNumber", diEx.getBasicnumber());
                    //统计该商品已分批出库的总数量-用于订单
                    item.put("finishNumber", depotItemService.getFinishNumber(diEx.getMaterialid(),diEx.getHeaderid()));
                    item.put("UnitPrice", diEx.getUnitprice());
                    item.put("TaxUnitPrice", diEx.getTaxunitprice());
                    item.put("AllPrice", diEx.getAllprice());
                    item.put("Remark", diEx.getRemark());
                    item.put("Img", diEx.getImg());
                    item.put("DepotId", diEx.getDepotid() == null ? "" : diEx.getDepotid());
                    item.put("DepotName", diEx.getDepotid() == null ? "" : diEx.getDepotName());
                    item.put("AnotherDepotId", diEx.getAnotherdepotid() == null ? "" : diEx.getAnotherdepotid());
                    item.put("AnotherDepotName", diEx.getAnotherdepotid() == null ? "" : diEx.getAnotherDepotName());
                    item.put("TaxRate", diEx.getTaxrate());
                    item.put("TaxMoney", diEx.getTaxmoney());
                    item.put("TaxLastMoney", diEx.getTaxlastmoney());
                    item.put("OtherField1", diEx.getOtherfield1());
                    item.put("OtherField2", diEx.getOtherfield2());
                    item.put("OtherField3", diEx.getOtherfield3());
                    item.put("OtherField4", diEx.getOtherfield4());
                    item.put("OtherField5", diEx.getOtherfield5());
                    item.put("MType", diEx.getMtype());
                    item.put("op", 1);
                    dataArray.add(item);
                }
            }
            outer.put("rows", dataArray);
            res.code = 200;
            res.data = outer;
        } catch (Exception e) {
            e.printStackTrace();
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }


    /**
     * 获取扩展信息
     *
     * @return
     */
    public String getOtherInfo(String[] mpArr, DepotItemVo4WithInfoEx diEx)throws Exception {
        String materialOther = "";
        for (int i = 0; i < mpArr.length; i++) {
            if (mpArr[i].equals("制造商")) {
                materialOther = materialOther + ((diEx.getMMfrs() == null || diEx.getMMfrs().equals("")) ? "" : "(" + diEx.getMMfrs() + ")");
            }
            if (mpArr[i].equals("自定义1")) {
                materialOther = materialOther + ((diEx.getMOtherField1() == null || diEx.getMOtherField1().equals("")) ? "" : "(" + diEx.getMOtherField1() + ")");
            }
            if (mpArr[i].equals("自定义2")) {
                materialOther = materialOther + ((diEx.getMOtherField2() == null || diEx.getMOtherField2().equals("")) ? "" : "(" + diEx.getMOtherField2() + ")");
            }
            if (mpArr[i].equals("自定义3")) {
                materialOther = materialOther + ((diEx.getMOtherField3() == null || diEx.getMOtherField3().equals("")) ? "" : "(" + diEx.getMOtherField3() + ")");
            }
        }
        return materialOther;
    }

    /**
     * 查找所有的明细
     * @param currentPage
     * @param pageSize
     * @param projectId
     * @param monthTime
     * @param headIds
     * @param materialIds
     * @param mpList
     * @param request
     * @return
     */
    @PostMapping(value = "/findByAll")
    public BaseResponseInfo findByAll(@RequestParam("currentPage") Integer currentPage,
                                      @RequestParam("pageSize") Integer pageSize,
                                      @RequestParam("depotId") Long depotId,
                                      @RequestParam("monthTime") String monthTime,
                                      @RequestParam("headIds") String headIds,
                                      @RequestParam("materialIds") String materialIds,
                                      @RequestParam("mpList") String mpList,
                                      HttpServletRequest request)throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<String, Object>();
        Long tenantId = Long.parseLong(request.getSession().getAttribute("tenantId").toString());
        try {
            List<DepotItemVo4WithInfoEx> dataList = depotItemService.findByAll(headIds, materialIds, (currentPage-1)*pageSize, pageSize);
            String[] mpArr = mpList.split(",");
            int total = depotItemService.findByAllCount(headIds, materialIds);
            map.put("total", total);
            //存放数据json数组
            JSONArray dataArray = new JSONArray();
            if (null != dataList) {
                List<Long> idList = new ArrayList<Long>();
                for (DepotItemVo4WithInfoEx m : dataList) {
                    idList.add(m.getMId());
                }
                List<MaterialExtend> meList = materialExtendService.getListByMIds(idList);
                for (DepotItemVo4WithInfoEx diEx : dataList) {
                    JSONObject item = new JSONObject();
                    Long mId = diEx.getMId();
                    String timeA = monthTime+"-01 00:00:00";
                    String timeB = monthTime+"-31 23:59:59";
                    item.put("MaterialName", diEx.getMName());
                    item.put("MaterialModel", diEx.getMModel());
                    //扩展信息
                    String materialOther = getOtherInfo(mpArr, diEx);
                    item.put("MaterialOther", materialOther);
                    item.put("MaterialColor", diEx.getMColor());
                    item.put("unitName", getUName(diEx.getMaterialUnit(), diEx.getUName()));

                    item.put("prevSum", depotItemService.getStockByParam(depotId,mId,null,timeA,tenantId));
                    item.put("InSum", depotItemService.getInNumByParam(depotId,mId,timeA,timeB,tenantId));
                    item.put("OutSum", depotItemService.getOutNumByParam(depotId,mId,timeA,timeB,tenantId));
                    BigDecimal thisSum = depotItemService.getStockByParam(depotId,mId,null,null,tenantId);
                    item.put("thisSum", thisSum);
                    for(MaterialExtend me:meList) {
                        if(me.getMaterialId().longValue() == diEx.getMId().longValue()) {
                            if(me.getPurchaseDecimal()!=null) {
                                item.put("UnitPrice", me.getPurchaseDecimal());
                                item.put("thisAllPrice", thisSum.multiply(me.getPurchaseDecimal()));
                            }
                        }
                    }
                    dataArray.add(item);
                }
            }
            map.put("rows", dataArray);
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
     * 导出excel表格
     * @param currentPage
     * @param pageSize
     * @param projectId
     * @param monthTime
     * @param headIds
     * @param materialIds
     * @param request
     * @param response
     * @return
     */
    @GetMapping(value = "/exportExcel")
    public void exportExcel(@RequestParam("currentPage") Integer currentPage,
                            @RequestParam("pageSize") Integer pageSize,
                            @RequestParam("depotId") Long depotId,
                            @RequestParam("monthTime") String monthTime,
                            @RequestParam("headIds") String headIds,
                            @RequestParam("materialIds") String materialIds,
                            HttpServletRequest request, HttpServletResponse response) {
        Long tenantId = Long.parseLong(request.getSession().getAttribute("tenantId").toString());
        try {
            List<DepotItemVo4WithInfoEx> dataList = depotItemService.findByAll(headIds, materialIds, (currentPage-1)*pageSize, pageSize);
            //存放数据json数组
            String[] names = {"名称", "型号", "单位", "单价", "上月结存数量", "入库数量", "出库数量", "本月结存数量", "结存金额"};
            String title = "库存报表";
            List<String[]> objects = new ArrayList<String[]>();
            if (null != dataList) {
                for (DepotItemVo4WithInfoEx diEx : dataList) {
                    Long mId = diEx.getMId();
                    String timeA = monthTime+"-01 00:00:00";
                    String timeB = monthTime+"-31 23:59:59";
                    String[] objs = new String[9];
                    objs[0] = diEx.getMName().toString();
                    objs[1] = diEx.getMModel().toString();
                    objs[2] = diEx.getMaterialUnit().toString();
                    objs[3] = getUnitPrice(diEx.getPresetPriceOne(), diEx.getPriceStrategy()).toString();
                    objs[4] = depotItemService.getStockByParam(depotId,mId,null,timeA,tenantId).toString();
                    objs[5] = depotItemService.getInNumByParam(depotId,mId,timeA,timeB,tenantId).toString();
                    objs[6] = depotItemService.getOutNumByParam(depotId,mId,timeA,timeB,tenantId).toString();
                    BigDecimal thisSum = depotItemService.getStockByParam(depotId,mId,null,null,tenantId);
                    objs[7] = thisSum.toString();
                    objs[8] = thisSum.multiply(getUnitPrice(diEx.getPresetPriceOne(), diEx.getPriceStrategy())).toString();
                    objects.add(objs);
                }
            }
            File file = ExcelUtils.exportObjectsWithoutTitle(title, names, title, objects);
            ExportExecUtil.showExec(file, file.getName() + "-" + monthTime, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 统计总计金额
     * @param pid
     * @param monthTime
     * @param headIds
     * @param materialIds
     * @param request
     * @return
     */
    @PostMapping(value = "/totalCountMoney")
    public BaseResponseInfo totalCountMoney(@RequestParam("depotId") Long depotId,
                                                        @RequestParam("monthTime") String monthTime,
                                                        @RequestParam("headIds") String headIds,
                                                        @RequestParam("materialIds") String materialIds,
                                                        HttpServletRequest request) throws Exception{
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<String, Object>();
        Long tenantId = Long.parseLong(request.getSession().getAttribute("tenantId").toString());
        try {
            List<DepotItemVo4WithInfoEx> dataList = depotItemService.findByAll(headIds, materialIds, null, null);
            BigDecimal thisAllPrice = BigDecimal.ZERO;
            if (null != dataList) {
                for (DepotItemVo4WithInfoEx diEx : dataList) {
                    Long mId = diEx.getMId();
                    BigDecimal thisSum = depotItemService.getStockByParam(depotId,mId,null,null,tenantId);
                    BigDecimal unitPrice = getUnitPrice(diEx.getPresetPriceOne(), diEx.getPriceStrategy());
                    thisAllPrice = thisAllPrice.add(thisSum.multiply(unitPrice));
                }
            }
            map.put("totalCount", thisAllPrice);
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
     * 进货统计
     * @param currentPage
     * @param pageSize
     * @param monthTime
     * @param headIds
     * @param materialIds
     * @param mpList
     * @param request
     * @return
     */
    @PostMapping(value = "/buyIn")
    public BaseResponseInfo buyIn(@RequestParam("currentPage") Integer currentPage,
                                      @RequestParam("pageSize") Integer pageSize,
                                      @RequestParam("monthTime") String monthTime,
                                      @RequestParam("headIds") String headIds,
                                      @RequestParam("materialIds") String materialIds,
                                      @RequestParam("mpList") String mpList,
                                      HttpServletRequest request)throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            List<DepotItemVo4WithInfoEx> dataList = depotItemService.findByAll(headIds, materialIds, (currentPage-1)*pageSize, pageSize);
            String[] mpArr = mpList.split(",");
            int total = depotItemService.findByAllCount(headIds, materialIds);
            map.put("total", total);
            //存放数据json数组
            JSONArray dataArray = new JSONArray();
            if (null != dataList) {
                for (DepotItemVo4WithInfoEx diEx : dataList) {
                    JSONObject item = new JSONObject();
                    BigDecimal InSum = depotItemService.buyOrSale("入库", "采购", diEx.getMId(), monthTime, "number");
                    BigDecimal OutSum = depotItemService.buyOrSale("出库", "采购退货", diEx.getMId(), monthTime, "number");
                    BigDecimal InSumPrice = depotItemService.buyOrSale("入库", "采购", diEx.getMId(), monthTime, "price");
                    BigDecimal OutSumPrice = depotItemService.buyOrSale("出库", "采购退货", diEx.getMId(), monthTime, "price");
                    item.put("MaterialName", diEx.getMName());
                    item.put("MaterialModel", diEx.getMModel());
                    //扩展信息
                    String materialOther = getOtherInfo(mpArr, diEx);
                    item.put("MaterialOther", materialOther);
                    item.put("MaterialColor", diEx.getMColor());
                    item.put("MaterialUnit", diEx.getMaterialUnit());
                    item.put("UName", diEx.getUName());
                    item.put("InSum", InSum);
                    item.put("OutSum", OutSum);
                    item.put("InSumPrice", InSumPrice);
                    item.put("OutSumPrice", OutSumPrice);
                    dataArray.add(item);
                }
            }
            map.put("rows", dataArray);
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
     * 销售统计
     * @param currentPage
     * @param pageSize
     * @param monthTime
     * @param headIds
     * @param materialIds
     * @param mpList
     * @param request
     * @return
     */
    @PostMapping(value = "/saleOut")
    public BaseResponseInfo saleOut(@RequestParam("currentPage") Integer currentPage,
                                  @RequestParam("pageSize") Integer pageSize,
                                  @RequestParam("monthTime") String monthTime,
                                  @RequestParam("headIds") String headIds,
                                  @RequestParam("materialIds") String materialIds,
                                  @RequestParam("mpList") String mpList,
                                  HttpServletRequest request)throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            List<DepotItemVo4WithInfoEx> dataList = depotItemService.findByAll(headIds, materialIds, (currentPage-1)*pageSize, pageSize);
            String[] mpArr = mpList.split(",");
            int total = depotItemService.findByAllCount(headIds, materialIds);
            map.put("total", total);
            //存放数据json数组
            JSONArray dataArray = new JSONArray();
            if (null != dataList) {
                for (DepotItemVo4WithInfoEx diEx : dataList) {
                    JSONObject item = new JSONObject();
                    BigDecimal OutSumRetail = depotItemService.buyOrSale("出库", "零售", diEx.getMId(), monthTime,"number");
                    BigDecimal OutSum = depotItemService.buyOrSale("出库", "销售", diEx.getMId(), monthTime,"number");
                    BigDecimal InSumRetail = depotItemService.buyOrSale("入库", "零售退货", diEx.getMId(), monthTime,"number");
                    BigDecimal InSum = depotItemService.buyOrSale("入库", "销售退货", diEx.getMId(), monthTime,"number");
                    BigDecimal OutSumRetailPrice = depotItemService.buyOrSale("出库", "零售", diEx.getMId(), monthTime,"price");
                    BigDecimal OutSumPrice = depotItemService.buyOrSale("出库", "销售", diEx.getMId(), monthTime,"price");
                    BigDecimal InSumRetailPrice = depotItemService.buyOrSale("入库", "零售退货", diEx.getMId(), monthTime,"price");
                    BigDecimal InSumPrice = depotItemService.buyOrSale("入库", "销售退货", diEx.getMId(), monthTime,"price");
                    BigDecimal OutInSumPrice = (OutSumRetailPrice.add(OutSumPrice)).subtract(InSumRetailPrice.add(InSumPrice));
                    item.put("MaterialName", diEx.getMName());
                    item.put("MaterialModel", diEx.getMModel());
                    //扩展信息
                    String materialOther = getOtherInfo(mpArr, diEx);
                    item.put("MaterialOther", materialOther);
                    item.put("MaterialColor", diEx.getMColor());
                    item.put("MaterialUnit", diEx.getMaterialUnit());
                    item.put("UName", diEx.getUName());
                    item.put("OutSum", OutSumRetail.add(OutSum));
                    item.put("InSum", InSumRetail.add(InSum));
                    item.put("OutSumPrice", OutSumRetailPrice.add(OutSumPrice));
                    item.put("InSumPrice", InSumRetailPrice.add(InSumPrice));
                    item.put("OutInSumPrice",OutInSumPrice);//实际销售金额
                    dataArray.add(item);
                }
            }
            map.put("rows", dataArray);
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
     * 获取单位
     * @param materialUnit
     * @param uName
     * @return
     */
    public String getUName(String materialUnit, String uName) {
        String unitName = null;
        if(!StringUtil.isEmpty(materialUnit)) {
            unitName = materialUnit;
        } else if(!StringUtil.isEmpty(uName)) {
            unitName = uName.substring(0,uName.indexOf(","));
        }
        return unitName;
    }

    /**
     * 获取单价
     * @param presetPriceOne
     * @param priceStrategy
     * @return
     */
    public BigDecimal getUnitPrice(BigDecimal presetPriceOne, String priceStrategy) {
        BigDecimal unitPrice = BigDecimal.ZERO;
        if(presetPriceOne != null) {
            DecimalFormat df = new DecimalFormat("#.00");
            unitPrice = new BigDecimal(df.format(presetPriceOne));
        } else {
            JSONArray priceArr = JSONArray.parseArray(priceStrategy);
            if(priceArr!=null && priceArr.get(0)!=null) {
                JSONObject priceObj = JSONObject.parseObject(priceArr.get(0).toString());
                BigDecimal basicPresetPriceOne = priceObj.getJSONObject("basic").getBigDecimal("PresetPriceOne");
                if(basicPresetPriceOne!=null) {
                    unitPrice = basicPresetPriceOne;
                }
            }
        }
        return unitPrice;
    }

    /**
     * create by: qiankunpingtai
     * website：https://qiankunpingtai.cn
     * description:
     *  批量删除单据明细信息
     * create time: 2019/3/29 11:15
     * @Param: ids
     * @return java.lang.Object
     */
    @RequestMapping(value = "/batchDeleteDepotItemByIds")
    public Object batchDeleteDepotItemByIds(@RequestParam("ids") String ids) throws Exception {
        JSONObject result = ExceptionConstants.standardSuccess();
        int i= depotItemService.batchDeleteDepotItemByIds(ids);
        if(i<1){
            logger.error("异常码[{}],异常提示[{}],参数,ids[{}]",
                    ExceptionConstants.DEPOT_ITEM_DELETE_FAILED_CODE,ExceptionConstants.DEPOT_ITEM_DELETE_FAILED_MSG,ids);
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_ITEM_DELETE_FAILED_CODE,
                    ExceptionConstants.DEPOT_ITEM_DELETE_FAILED_MSG);
        }
        return result;
    }
    /**
     * 库存预警报表
     * @param currentPage
     * @param pageSize
     * @return
     */
    @GetMapping(value = "/findStockWarningCount")
    public BaseResponseInfo findStockWarningCount(@RequestParam("currentPage") Integer currentPage,
                                                  @RequestParam("pageSize") Integer pageSize,  @RequestParam("projectId") Integer pid )throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            List<DepotItemStockWarningCount> resList = new ArrayList<DepotItemStockWarningCount>();
            List<DepotItemStockWarningCount> list = depotItemService.findStockWarningCount((currentPage-1)*pageSize, pageSize,pid);
            int total = depotItemService.findStockWarningCountTotal(pid);
            map.put("total", total);
            map.put("rows", list);
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
     * 导出库存预警excel表格
     * @param currentPage
     * @param pageSize
     * @param projectId
     * @param monthTime
     * @param request
     * @param response
     * @return
     */
    @GetMapping(value = "/exportWarningExcel")
    public BaseResponseInfo exportWarningExcel(@RequestParam("currentPage") Integer currentPage,
                                        @RequestParam("pageSize") Integer pageSize,
                                        @RequestParam("projectId") Integer projectId,
                                        HttpServletRequest request, HttpServletResponse response)throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<String, Object>();
        String message = "成功";
        try {
            List<DepotItemStockWarningCount> dataList = depotItemService.findStockWarningCount((currentPage - 1) * pageSize, pageSize, projectId);
            //存放数据json数组
            Integer pid = projectId;
            String[] names = {"名称", "型号", "扩展信息", "单位", "入库数量", "出库数量", "库存数量", "安全库存量", "临界库存量"};
            String title = "库存预警报表";
            List<String[]> objects = new ArrayList<String[]>();
            if (null != dataList) {
                for (DepotItemStockWarningCount diEx : dataList) {
                    String[] objs = new String[9];
                    objs[0] = diEx.getMaterialName().toString();
                    objs[1] = diEx.getMaterialModel().toString();
                    objs[2] = diEx.getMaterialOther().toString();
                    objs[3] = diEx.getMaterialUnit().toString();
                    objs[4] = diEx.getBasicInNumber().toString();
                    objs[5] = diEx.getBasicOutNumber() == null ? "0" : diEx.getBasicOutNumber().toString();
                    objs[6] = diEx.getBasicNumber() == null ? "0" : diEx.getBasicNumber().toString();
                    objs[7] = diEx.getSafetystock() == null ? "0" : diEx.getSafetystock().toString();
                    objs[8] = diEx.getBasicLinjieNumber() == null ? "0" : diEx.getBasicLinjieNumber().toString();
                    objects.add(objs);
                }
            }
            File file = ExcelUtils.exportObjectsWithoutTitle(title+pid, names, title, objects);
            ExportExecUtil.showExec(file, file.getName(), response);
            res.code = 200;
        } catch (Exception e) {
            e.printStackTrace();
            message = "导出失败";
            res.code = 500;
        }
        return res;
    }

    /**
     * 统计采购或销售的总金额
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/buyOrSalePrice")
    public BaseResponseInfo buyOrSalePrice(HttpServletRequest request, HttpServletResponse response)throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<String, Object>();
        String message = "成功";
        try {
            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM");
            String dateString = formatter.format(date);
            List<String> list = Tools.getSixMonth(dateString);
            map.put("monthList", list);
            List<BigDecimal> buyPriceList = new ArrayList<BigDecimal>();
            for(String month: list) {
                BigDecimal outPrice = depotItemService.inOrOutPrice("入库", "采购", month);
                BigDecimal inPrice = depotItemService.inOrOutPrice("出库", "采购退货", month);
                buyPriceList.add(outPrice.subtract(inPrice));
            }
            map.put("buyPriceList", buyPriceList);
            List<BigDecimal> salePriceList = new ArrayList<BigDecimal>();
            for(String month: list) {
                BigDecimal outPrice = depotItemService.inOrOutPrice("出库", "销售", month);
                BigDecimal inPrice = depotItemService.inOrOutPrice("入库", "销售退货", month);
                salePriceList.add(outPrice.subtract(inPrice));
            }
            map.put("salePriceList", salePriceList);
            res.code = 200;
            res.data = map;
        } catch (Exception e) {
            e.printStackTrace();
            message = "统计失败";
            res.code = 500;
        }
        return res;
    }
}
