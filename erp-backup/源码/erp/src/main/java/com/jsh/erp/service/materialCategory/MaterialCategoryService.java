package com.jsh.erp.service.materialCategory;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.*;
import com.jsh.erp.datasource.mappers.MaterialCategoryMapper;
import com.jsh.erp.datasource.mappers.MaterialCategoryMapperEx;
import com.jsh.erp.datasource.mappers.MaterialMapperEx;
import com.jsh.erp.datasource.vo.TreeNode;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.service.log.LogService;
import com.jsh.erp.service.user.UserService;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class MaterialCategoryService {
    private Logger logger = LoggerFactory.getLogger(MaterialCategoryService.class);

    @Resource
    private MaterialCategoryMapper materialCategoryMapper;
    @Resource
    private MaterialCategoryMapperEx materialCategoryMapperEx;
    @Resource
    private UserService userService;
    @Resource
    private LogService logService;
    @Resource
    private MaterialMapperEx materialMapperEx;

    public MaterialCategory getMaterialCategory(long id)throws Exception {
        MaterialCategory result=null;
        try{
            result=materialCategoryMapper.selectByPrimaryKey(id);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<MaterialCategory> getMaterialCategory()throws Exception {
        MaterialCategoryExample example = new MaterialCategoryExample();
        List<MaterialCategory> list=null;
        try{
            list=materialCategoryMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<MaterialCategory> getAllList(Long parentId)throws Exception {
        List<MaterialCategory> list=null;
        try{
            list = getMCList(parentId);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<MaterialCategory> getMCList(Long parentId)throws Exception {
        List<MaterialCategory> res= new ArrayList<MaterialCategory>();
        List<MaterialCategory> list=null;
        MaterialCategoryExample example = new MaterialCategoryExample();
        example.createCriteria().andParentidEqualTo(parentId).andIdNotEqualTo(1l);
        example.setOrderByClause("id");
        list=materialCategoryMapper.selectByExample(example);
        if(list!=null && list.size()>0) {
            res.addAll(list);
            for(MaterialCategory mc : list) {
                List<MaterialCategory> mcList = getMCList(mc.getId());
                if(mcList!=null && mcList.size()>0) {
                    res.addAll(mcList);
                }
            }
        }
        return res;
    }

    public List<MaterialCategory> select(String name, Integer parentId, int offset, int rows) throws Exception{
        List<MaterialCategory> list=null;
        try{
            list=materialCategoryMapperEx.selectByConditionMaterialCategory(name, parentId, offset, rows);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public Long countMaterialCategory(String name, Integer parentId) throws Exception{
        Long result=null;
        try{
            result=materialCategoryMapperEx.countsByMaterialCategory(name, parentId);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertMaterialCategory(String beanJson, HttpServletRequest request)throws Exception {
        MaterialCategory materialCategory = JSONObject.parseObject(beanJson, MaterialCategory.class);
        int result=0;
        try{
            result=materialCategoryMapper.insertSelective(materialCategory);
            logService.insertLog("????????????", BusinessConstants.LOG_OPERATION_TYPE_ADD, request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateMaterialCategory(String beanJson, Long id, HttpServletRequest request) throws Exception{
        MaterialCategory materialCategory = JSONObject.parseObject(beanJson, MaterialCategory.class);
        materialCategory.setId(id);
        int result=0;
        try{
            result=materialCategoryMapper.updateByPrimaryKeySelective(materialCategory);
            logService.insertLog("????????????",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(id).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteMaterialCategory(Long id, HttpServletRequest request)throws Exception {
        int result=0;
        try{
            result=materialCategoryMapper.deleteByPrimaryKey(id);
            logService.insertLog("????????????",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_DELETE).append(id).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteMaterialCategory(String ids, HttpServletRequest request)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        MaterialCategoryExample example = new MaterialCategoryExample();
        example.createCriteria().andIdIn(idList);
        int result=0;
        try{
            result=materialCategoryMapper.deleteByExample(example);
            logService.insertLog("????????????", "????????????,id???:" + ids, request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int checkIsNameExist(Long id, String name)throws Exception {
        return 0;
    }

    public List<MaterialCategory> findById(Long id)throws Exception {
        MaterialCategoryExample example = new MaterialCategoryExample();
        example.createCriteria().andIdEqualTo(id);
        List<MaterialCategory> list=null;
        try{
            list=materialCategoryMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }
    /**
     * create by: cjl
     * description:
     *???????????????????????????
     * create time: 2019/2/19 14:30
     * @Param:
     * @return java.util.List<com.jsh.erp.datasource.vo.TreeNode>
     */
    public List<TreeNode> getMaterialCategoryTree(Long id) throws Exception{
        List<TreeNode> list=null;
        try{
            list=materialCategoryMapperEx.getNodeTree(id);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
       return list;
    }
    /**
     * create by: cjl
     * description:
     *  ????????????????????????
     * create time: 2019/2/19 16:30
     * @Param: mc
     * @return void
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int addMaterialCategory(MaterialCategory mc) throws Exception {
        logService.insertLog("????????????",
                BusinessConstants.LOG_OPERATION_TYPE_ADD,
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        if(mc==null){
            return 0;
        }
        if(mc.getParentid()==null){
            //???????????????????????????id???????????????????????????????????????????????????
            mc.setParentid(BusinessConstants.MATERIAL_CATEGORY_ROOT_PARENT_ID);
        }
        //???????????????????????????????????????
        checkMaterialCategorySerialNo(mc);
        //??????????????????????????????????????????
        mc.setStatus(BusinessConstants.MATERIAL_CATEGORY_STATUS_ENABLE);
        Date date=new Date();
        User userInfo=userService.getCurrentUser();
        //????????????
        mc.setCreateTime(date);
        //?????????
        mc.setCreator(userInfo==null?null:userInfo.getId());
        //????????????
        mc.setUpdateTime(date);
        //?????????
        mc.setUpdater(userInfo==null?null:userInfo.getId());
        int result=0;
        try{
            result=materialCategoryMapperEx.addMaterialCategory(mc);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteMaterialCategoryByIds(String ids) throws Exception {
        logService.insertLog("????????????",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_DELETE).append(ids).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        //????????????
        Date updateDate =new Date();
        //?????????
        User userInfo=userService.getCurrentUser();
        Long updater=userInfo==null?null:userInfo.getId();
        String strArray[]=ids.split(",");
        if(strArray.length<1){
            return 0;
        }
        int result=0;
        try{
            result=materialCategoryMapperEx.batchDeleteMaterialCategoryByIds(updateDate,updater,strArray);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
       return result;
    }
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int editMaterialCategory(MaterialCategory mc) throws Exception{
        logService.insertLog("????????????",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(mc.getId()).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        if(mc.getParentid()==null){
            //???????????????????????????id???????????????????????????????????????????????????
            mc.setParentid(BusinessConstants.MATERIAL_CATEGORY_ROOT_PARENT_ID);
        }

        //???????????????????????????????????????
        checkMaterialCategorySerialNo(mc);
        //????????????
        mc.setUpdateTime(new Date());
        //?????????
        User userInfo=userService.getCurrentUser();
        mc.setUpdater(userInfo==null?null:userInfo.getId());
        int result=0;
        try{
            result= materialCategoryMapperEx.editMaterialCategory(mc);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }
    /**
     * ?????????????????????????????????????????????????????????
     * */
    public void  checkMaterialCategorySerialNo(MaterialCategory mc)throws Exception {
        if(mc==null){
            return;
        }
        if(StringUtil.isEmpty(mc.getSerialNo())){
            return;
        }
        //??????????????????????????????????????????
        List<MaterialCategory> mList=null;
        try{
            mList= materialCategoryMapperEx.getMaterialCategoryBySerialNo(mc.getSerialNo());
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        if(mList==null||mList.size()<1){
            //???????????????????????????????????????
            return;
        }
        if(mList.size()>1){
            //??????????????????????????????1??????????????????
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_CATEGORY_SERIAL_ALREADY_EXISTS_CODE,
                    ExceptionConstants.MATERIAL_CATEGORY_SERIAL_ALREADY_EXISTS_MSG);
        }
        if(mc.getId()==null){
            //???????????????????????????
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_CATEGORY_SERIAL_ALREADY_EXISTS_CODE,
                    ExceptionConstants.MATERIAL_CATEGORY_SERIAL_ALREADY_EXISTS_MSG);
        }
        /**
         * ???????????????equals?????????
         * */
        if(mc.getId().equals(mList.get(0).getId())){
            //???????????????????????????id??????
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_CATEGORY_SERIAL_ALREADY_EXISTS_CODE,
                    ExceptionConstants.MATERIAL_CATEGORY_SERIAL_ALREADY_EXISTS_MSG);
        }
    }

    /**
     * create by: qiankunpingtai
     * website???https://qiankunpingtai.cn
     * description:
     *  ???????????????????????????????????????????????????????????????
     * create time: 2019/4/11 9:26
     * @Param: ids
     * @return int
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteMaterialCategoryByIdsNormal(String ids) throws Exception {
        /**
         * ??????
         * 1????????????	jsh_material
         * 2??????????????????	jsh_materialcategory
         * ?????????????????????
         * */
        int deleteTotal=0;
        if(StringUtils.isEmpty(ids)){
            return deleteTotal;
        }
        String [] idArray=ids.split(",");
        /**
         * ???????????????	jsh_material
         * */
        List<Material> materialList=null;
        try{
            materialList= materialMapperEx.getMaterialListByCategoryIds(idArray);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        if(materialList!=null&&materialList.size()>0){
            logger.error("?????????[{}],????????????[{}],??????,CategoryIds[{}]",
                    ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,ExceptionConstants.DELETE_FORCE_CONFIRM_MSG,ids);
            throw new BusinessRunTimeException(ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,
                    ExceptionConstants.DELETE_FORCE_CONFIRM_MSG);
        }
        /**
         * ?????????????????????	jsh_materialcategory
         * */
        List<MaterialCategory> materialCategoryList=null;
        try{
            materialCategoryList= materialCategoryMapperEx.getMaterialCategoryListByCategoryIds(idArray);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        if(materialCategoryList!=null&&materialCategoryList.size()>0){
            logger.error("?????????[{}],????????????[{}],??????,CategoryIds[{}]",
                    ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,ExceptionConstants.DELETE_FORCE_CONFIRM_MSG,ids);
            throw new BusinessRunTimeException(ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,
                    ExceptionConstants.DELETE_FORCE_CONFIRM_MSG);
        }
        /**
         * ??????????????????????????????
         * */
        deleteTotal= batchDeleteMaterialCategoryByIds(ids);
        return deleteTotal;
    }

    /**
     * ????????????????????????
     * @param name
     */
    public Long getCategoryIdByName(String name){
        Long categoryId = 0l;
        MaterialCategoryExample example = new MaterialCategoryExample();
        example.createCriteria().andNameEqualTo(name).andStatusNotEqualTo(BusinessConstants.DELETE_TYPE_FORCE);
        List<MaterialCategory> list = materialCategoryMapper.selectByExample(example);
        if(list!=null && list.size()>0) {
            categoryId = list.get(0).getId();
        }
        return categoryId;
    }
}
