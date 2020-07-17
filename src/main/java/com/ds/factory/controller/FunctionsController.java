package com.ds.factory.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ds.factory.constants.BusinessConstants;
import com.ds.factory.constants.ExceptionConstants;
import com.ds.factory.dao.Example.FunctionsExample;
import com.ds.factory.datasource.entities.Client;
import com.ds.factory.datasource.entities.Functions;
import com.ds.factory.datasource.entities.Staff;
import com.ds.factory.datasource.mappers.FunctionsMapper;
import com.ds.factory.exception.BusinessRunTimeException;
import com.ds.factory.exception.DSException;
import com.ds.factory.service.Service.FunctionsService;
import com.ds.factory.service.Service.LogService;
import com.ds.factory.service.Service.UserBusinessService;
import com.ds.factory.utils.*;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ds.factory.utils.ResponseJsonUtil.returnJson;

/**
 * @author gyc_lyz_ztm
 */
@RestController
@RequestMapping(value = "/functions")
public class FunctionsController {
    private Logger logger = LoggerFactory.getLogger(FunctionsController.class);

    @Resource
    private FunctionsMapper functionsMapper;

    @Resource
    LogService logService;

    @Resource
    private FunctionsService functionsService;

    @Resource
    private UserBusinessService userBusinessService;

    @Autowired
    AmqpTemplate messageQueue;

    @PostMapping(value = "/findMenu")
    @CrossOrigin
    public JSONArray findMenu(@RequestParam(value="pNumber") String pNumber,
                              @RequestParam(value="hasFunctions") String hasFunctions,
                              HttpServletRequest request)throws Exception {
        //存放数据json数组
        JSONArray dataArray = new JSONArray();
        try {
            String fc = hasFunctions; //当前用户所拥有的功能列表，格式如：[1][2][5]
            List<Functions> dataList = functionsService.getRoleFunctions(pNumber);
            if (null != dataList) {
                for (Functions functions : dataList) {
                    JSONObject item = new JSONObject();
                    item.put("id", functions.getId());
                    List<Functions> dataList1 = functionsService.getRoleFunctions(functions.getNumber());
                    JSONArray dataArray1 = new JSONArray();
                    if (dataList1.size() != 0) {
                        item.put("text", functions.getName()); //是目录就没链接
                        item.put("icon", functions.getIcon());
                        for (Functions functions1 : dataList1) {
                            item.put("state", "open");   //如果不为空，节点展开
                            JSONObject item1 = new JSONObject();
                            List<Functions> dataList2 = functionsService.getRoleFunctions(functions1.getNumber());
                            if (fc.indexOf("[" + functions1.getId().toString() + "]") != -1 || dataList2.size() != 0) {
                                item1.put("id", functions1.getId());
                                JSONArray dataArray2 = new JSONArray();
                                if (dataList2.size() != 0) {
                                    item1.put("text", functions1.getName());//是目录就没链接
                                    item1.put("icon", functions1.getIcon());
                                    for (Functions functions2 : dataList2) {
                                        item1.put("state", "closed");   //如果不为空，节点不展开
                                        JSONObject item2 = new JSONObject();
                                        List<Functions> dataList3 = functionsService.getRoleFunctions(functions2.getNumber());
                                        if (fc.indexOf("[" + functions2.getId().toString() + "]") != -1 || dataList3.size() != 0) {
                                            item2.put("id", functions2.getId());
                                            JSONArray dataArray3 = new JSONArray();
                                            if (dataList3.size() != 0) {
                                                item2.put("text", functions2.getName());//是目录就没链接
                                                item2.put("icon", functions2.getIcon());
                                                for (Functions functions3 : dataList3) {
                                                    item2.put("state", "closed");   //如果不为空，节点不展开
                                                    JSONObject item3 = new JSONObject();
                                                    item3.put("id", functions3.getId());
                                                    item3.put("text", functions3.getName());
                                                    item3.put("icon", functions3.getIcon());
                                                    dataArray3.add(item3);
                                                    item2.put("children", dataArray3);
                                                }
                                            } else {
                                                //不是目录，有链接
                                                item2.put("text", functions2.getName());
                                                item2.put("icon", functions2.getIcon());
                                                item2.put("url", functions2.getUrl());
                                                dataArray2.add(item2);
                                                item1.put("children", dataArray2);
                                            }
                                        } else {
                                            //不是目录，有链接
                                        }
                                    }
                                } else {
                                    //不是目录，有链接
                                    item1.put("text", functions1.getName());
                                    item1.put("icon", functions1.getIcon());
                                    item1.put("url", functions1.getUrl());
                                    dataArray1.add(item1);
                                    item.put("children", dataArray1);
                                }
                            } else {
                                //不是目录，有链接
                            }
                        }
                    } else {
                        //不是目录，有链接
                        item.put("text", functions.getName());
                        item.put("icon", functions.getIcon());
                        item.put("url", functions.getUrl());
                    }
                    dataArray.add(item);
                }
            }

            Staff sta=(Staff)request.getSession().getAttribute("user");
            logService.insertLog(BusinessConstants.LOG_MODULE_NAME_FUNCTIONS,
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_SEARCH).append(", id: "+sta.getId()).toString(),
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());

        } catch (DataAccessException e) {
            logger.error(">>>>>>>>>>>>>>>>>>>查找应用异常", e);
        }
        return dataArray;
    }

    @GetMapping(value = "/list")
    @CrossOrigin
    public String list(@RequestParam(value = Constants.PAGE_SIZE, required = false) Integer pageSize,
                          @RequestParam(value = Constants.CURRENT_PAGE, required = false) Integer currentPage,
                          @RequestParam(value = Constants.SEARCH, required = false) String search,
                          HttpServletRequest request)throws Exception {
        Map<String, String> parameterMap = ParamUtils.requestToMap(request);
        parameterMap.put(Constants.SEARCH, search);
        JSONObject obj= JSON.parseObject(search);

        String name=obj.getString("name")==null?"":obj.getString("name").trim();
        String no=obj.getString("no")==null?"":obj.getString("no").trim();
        PageQueryInfo queryInfo = new PageQueryInfo();
        Map<String, Object> objectMap = new HashMap<String, Object>();
        if (pageSize != null && pageSize <= 0) {
            pageSize = 10;
        }
        String offset = ParamUtils.getPageOffset(currentPage, pageSize);
        if (StringUtil.isNotEmpty(offset)) {
            parameterMap.put(Constants.OFFSET, offset);
        }
        PageHelper.startPage(currentPage,pageSize,true);

        String Jedis_String = "Functions--No:"+no+"; Name:"+name;
        List<Functions> list =new ArrayList<Functions>();
        Jedis jedis=new Jedis("192.168.216.130",6379);//原先是6379
        String jsonString=jedis.get(Jedis_String);
        if(jsonString!=null)
        {
            ObjectMapper objectMapper=new ObjectMapper();
            TypeReference<List<Functions>> typeReference =
                    new TypeReference<List<Functions>>() {};
            byte[] data=jsonString.getBytes();
            try {
                list=objectMapper.readValue(data, typeReference);
                System.out.println("***** ***** ***** ***** ***** *****");
            } catch (JsonParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (JsonMappingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else
        {
            System.out.println("##### ##### ##### ##### ##### #####");
            list = functionsService.selectByConstrain(name,no);
            ObjectMapper objectMapper=new ObjectMapper();
            try {
                String jsonString2 = objectMapper.writeValueAsString(list);
                jedis.set(Jedis_String, jsonString2);
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        PageInfo<Functions> pageInfo = new PageInfo<>(list);
        objectMap.put("page", queryInfo);
        if (list == null) {
            queryInfo.setRows(new ArrayList<Object>());
            queryInfo.setTotal(BusinessConstants.DEFAULT_LIST_NULL_NUMBER);
            return returnJson(objectMap, "查找不到数据", ErpInfo.OK.code);
        }
        queryInfo.setRows(list);
        queryInfo.setTotal(pageInfo.getTotal());

        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_FUNCTIONS,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_SEARCH).append(", id: "+sta.getId()).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        String message="";
        for(int i=0;i<list.size();i++) {
            message+=list.get(i);
        }
        messageQueue.convertAndSend("Factory",message.toString());
        return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
    }


    @PostMapping(value = "/findRoleFunctions")
    @CrossOrigin
    public JSONArray findRoleFunctions(@RequestParam("UBType") String type, @RequestParam("UBKeyId") String keyId,
                                       HttpServletRequest request)throws Exception {
        JSONArray arr = new JSONArray();
        try {
            List<Functions> dataListFun = functionsService.findRoleFunctions("0");
            //开始拼接json数据
            JSONObject outer = new JSONObject();
            outer.put("id", 1);
            outer.put("text", "功能列表");
            outer.put("state", "open");
            //存放数据json数组
            JSONArray dataArray = new JSONArray();
            if (null != dataListFun) {
                //根据条件从列表里面移除"系统管理"
                List<Functions> dataList = new ArrayList<Functions>();
                for (Functions fun : dataListFun) {
                    //从session中获取租户id
                    String loginName = null;
                    Object userInfo = request.getSession().getAttribute("user");
                    if(userInfo != null) {
                        Staff user = (Staff) userInfo;
                        loginName = user.getLoginame();
                    }
                    if(("admin").equals(loginName)) {
                        dataList.add(fun);
                    } else {
                        if(!("系统管理").equals(fun.getName())) {
                            dataList.add(fun);
                        }
                    }
                }

                //筛选功能列表
                for (Functions functions : dataList) {
                    JSONObject item = new JSONObject();
                    item.put("id", functions.getId());
                    item.put("text", functions.getName());

                    //勾选判断1
                    Boolean flag = false;
                    try {
                        flag = userBusinessService.checkIsUserBusinessExist(type, keyId, "[" + functions.getId().toString() + "]");
                    } catch (Exception e) {
                        logger.error(">>>>>>>>>>>>>>>>>设置角色对应的功能：类型" + type + " KeyId为： " + keyId + " 存在异常！");
                    }
                    if (flag == true) {
                        item.put("checked", true);
                    }
                    //结束

                    List<Functions> dataList1 = functionsService.findRoleFunctions(functions.getNumber());
                    JSONArray dataArray1 = new JSONArray();
                    if (null != dataList1) {

                        for (Functions functions1 : dataList1) {
                            item.put("state", "open");   //如果不为空，节点不展开
                            JSONObject item1 = new JSONObject();
                            item1.put("id", functions1.getId());
                            item1.put("text", functions1.getName());

                            //勾选判断2
                            //Boolean flag = false;
                            try {
                                flag = userBusinessService.checkIsUserBusinessExist(type, keyId, "[" + functions1.getId().toString() + "]");
                            } catch (Exception e) {
                                logger.error(">>>>>>>>>>>>>>>>>设置角色对应的功能：类型" + type + " KeyId为： " + keyId + " 存在异常！");
                            }
                            if (flag == true) {
                                item1.put("checked", true);
                            }
                            //结束

                            List<Functions> dataList2 = functionsService.findRoleFunctions(functions1.getNumber());
                            JSONArray dataArray2 = new JSONArray();
                            if (null != dataList2) {

                                for (Functions functions2 : dataList2) {
                                    item1.put("state", "closed");   //如果不为空，节点不展开
                                    JSONObject item2 = new JSONObject();
                                    item2.put("id", functions2.getId());
                                    item2.put("text", functions2.getName());

                                    //勾选判断3
                                    //Boolean flag = false;
                                    try {
                                        flag = userBusinessService.checkIsUserBusinessExist(type, keyId, "[" + functions2.getId().toString() + "]");
                                    } catch (Exception e) {
                                        logger.error(">>>>>>>>>>>>>>>>>设置角色对应的功能：类型" + type + " KeyId为： " + keyId + " 存在异常！");
                                    }
                                    if (flag == true) {
                                        item2.put("checked", true);
                                    }
                                    //结束

                                    List<Functions> dataList3 = functionsService.findRoleFunctions(functions2.getNumber());
                                    JSONArray dataArray3 = new JSONArray();
                                    if (null != dataList3) {

                                        for (Functions functions3 : dataList3) {
                                            item2.put("state", "closed");   //如果不为空，节点不展开
                                            JSONObject item3 = new JSONObject();
                                            item3.put("id", functions3.getId());
                                            item3.put("text", functions3.getName());

                                            //勾选判断4
                                            //Boolean flag = false;
                                            try {
                                                flag = userBusinessService.checkIsUserBusinessExist(type, keyId, "[" + functions3.getId().toString() + "]");
                                            } catch (Exception e) {
                                                logger.error(">>>>>>>>>>>>>>>>>设置角色对应的功能：类型" + type + " KeyId为： " + keyId + " 存在异常！");
                                            }
                                            if (flag == true) {
                                                item3.put("checked", true);
                                            }
                                            //结束

                                            dataArray3.add(item3);
                                            item2.put("children", dataArray3);
                                        }
                                    }

                                    dataArray2.add(item2);
                                    item1.put("children", dataArray2);
                                }
                            }

                            dataArray1.add(item1);
                            item.put("children", dataArray1);
                        }

                    }
                    dataArray.add(item);
                }
                outer.put("children", dataArray);
                arr.add(outer);
            }

            Staff sta=(Staff)request.getSession().getAttribute("user");
            logService.insertLog(BusinessConstants.LOG_MODULE_NAME_ROLE,
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_SEARCH).append(", id: "+sta.getId()).toString(),
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return arr;
    }


    @GetMapping(value = "/findByIds")
    @CrossOrigin
    public BaseResponseInfo findByIds(@RequestParam("functionsIds") String functionsIds,
                                      HttpServletRequest request)throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            List<Functions> dataList = functionsService.findByIds(functionsIds);
            JSONObject outer = new JSONObject();
            outer.put("total", dataList.size());
            //存放数据json数组
            JSONArray dataArray = new JSONArray();
            if (null != dataList) {
                for (Functions functions : dataList) {
                    JSONObject item = new JSONObject();
                    item.put("Id", functions.getId());
                    item.put("Name", functions.getName());
                    item.put("PushBtn", functions.getPushbtn());
                    item.put("op", 1);
                    dataArray.add(item);
                }
            }
            outer.put("rows", dataArray);
            res.code = 200;
            res.data = outer;

            Staff sta=(Staff)request.getSession().getAttribute("user");
            logService.insertLog(BusinessConstants.LOG_MODULE_NAME_FUNCTIONS,
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_SEARCH).append(", id: "+sta.getId()).toString(),
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());

        } catch (Exception e) {
            e.printStackTrace();
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }


    @RequestMapping(value = "/batchDeleteFunctionsByIds")
    @CrossOrigin
    public Object batchDeleteFunctionsByIds(@RequestParam("ids") String ids, HttpServletRequest request) throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        FunctionsExample example = new FunctionsExample();
        example.createCriteria().andIdIn(idList);
        JSONObject result = ExceptionConstants.standardSuccess();
        try{
            functionsMapper.deleteByExample(example);
        }catch(Exception e){
            DSException.writeFail(logger, e);
        }

        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_FUNCTIONS,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_BATCH_delete).append(", id: "+sta.getId()).toString()+
                "删除信息ID组："+ids,
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        return result;
    }


    @PostMapping("/add")
    @ResponseBody
    @CrossOrigin
    public Object add(@RequestParam("info") String beanJson, HttpServletRequest request)throws Exception{
        JSONObject obj = JSONObject.parseObject(beanJson, JSONObject.class);
        String a=obj.get("Enabled").toString();
        Functions depot=new Functions();
        depot.setNumber(obj.get("Number").toString());
        depot.setPnumber(obj.get("PNumber").toString());
        depot.setName(obj.get("Name").toString());
        depot.setSort(obj.get("Sort").toString());
        depot.setUrl(obj.get("URL").toString());
        if (obj.get("Enabled").toString().equals("on")){
            depot.setEnabled(true);
        }else {
            depot.setEnabled(false);
        }

        JSONObject result = ExceptionConstants.standardSuccess();
        try{
            String[] PNumber=depot.getPnumber().split("；");
            if(PNumber.length==1)
                functionsMapper.insertSelective(depot);
            else
            {
                for(int i=0;i<PNumber.length;i++)
                {
                    depot.setPnumber(PNumber[i]);
                    functionsMapper.insertSelective(depot);
                }
            }

            Staff sta=(Staff)request.getSession().getAttribute("user");
            logService.insertLog(BusinessConstants.LOG_MODULE_NAME_FUNCTIONS,
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(", id: "+sta.getId()).toString()+
                    "添加信息："+depot,
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());

        }catch(Exception e){
            DSException.writeFail(logger, e);
        }
        return result;
    }


    @PostMapping("/update")
    @ResponseBody
    @CrossOrigin
    public Object update(@RequestParam("info") String beanJson,@RequestParam("id") Long id, HttpServletRequest request)throws Exception{
        JSONObject obj = JSONObject.parseObject(beanJson, JSONObject.class);
        String a=obj.get("Enabled").toString();
        Functions depot=new Functions();
        depot.setNumber(obj.get("Number").toString());
        depot.setPnumber(obj.get("PNumber").toString());
        depot.setName(obj.get("Name").toString());
        depot.setSort(obj.get("Sort").toString());
        depot.setUrl(obj.get("URL").toString());
        depot.setId(id);
        if (obj.get("Enabled").toString().equals("on")){
            depot.setEnabled(true);
        }else {
            depot.setEnabled(false);
        }

        JSONObject result = ExceptionConstants.standardSuccess();
        try{
            functionsMapper.updateByPrimaryKeySelective(depot);

            Staff sta=(Staff)request.getSession().getAttribute("user");
            logService.insertLog(BusinessConstants.LOG_MODULE_NAME_FUNCTIONS,
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(", id: "+sta.getId()).toString()+
                            "修改信息："+depot,
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        }catch(Exception e){
            DSException.writeFail(logger, e);
        }
        return result;
    }

}
