package com.ds.factory.controller;

//仓库管理

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ds.factory.constants.BusinessConstants;
import com.ds.factory.constants.ExceptionConstants;
import com.ds.factory.datasource.entities.*;
import com.ds.factory.service.Service.*;
import com.ds.factory.utils.*;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.ds.factory.utils.ResponseJsonUtil.returnJson;

@RestController
@RequestMapping(value = "/warehouse")
public class WarehouseController {
    @Resource
    Product_WarehouseService product_warehouseService;

    @Resource
    LogService logService;
    @Resource
    Expired_FoodService expired_foodService;

    @Resource
    Raw_Materials_WarehouseService raw_materials_warehouseService;

    @Autowired
    AmqpTemplate messageQueue;

    @GetMapping(value = "/getAllProductWarehouse")
    @CrossOrigin
    public String getAllProductWarehouseList(@RequestParam(value = Constants.PAGE_SIZE, required = false) Integer pageSize,
                          @RequestParam(value = Constants.CURRENT_PAGE, required = false) Integer currentPage,
                          @RequestParam(value = Constants.SEARCH, required = false) String search,
                          HttpServletRequest request)throws Exception {
        Map<String, String> parameterMap = new HashMap<String, String>();
        //查询参数
        JSONObject obj= JSON.parseObject(search);
        Set<String> key= obj.keySet();
        for(String keyEach: key){
            parameterMap.put(keyEach,obj.getString(keyEach));
        }
        String stock_no=parameterMap.get("stock_no");
        String product_no=parameterMap.get("product_no");
        String staff_no=parameterMap.get("staff_no");
        String manufacture_date=parameterMap.get("manufacture_date");
        String storage_address=parameterMap.get("storage_address");
        PageQueryInfo queryInfo = new PageQueryInfo();
        Map<String, Object> objectMap = new HashMap<String, Object>();
        if (pageSize == null || pageSize <= 0) {
            pageSize = BusinessConstants.DEFAULT_PAGINATION_PAGE_SIZE;
        }
        if (currentPage == null || currentPage <= 0) {
            currentPage = BusinessConstants.DEFAULT_PAGINATION_PAGE_NUMBER;
        }
        PageHelper.startPage(currentPage,pageSize,true);

        String Jedis_String = "Product_Warehouse--Stock_No:"+stock_no+"; Product_No:"+product_no+
                "; Staff_No:"+staff_no + "; Manufacture_Date:"+manufacture_date +
                "; Storage_Address:"+storage_address;
        List<Product_Warehouse> list =new ArrayList<Product_Warehouse>();
        Jedis jedis=new Jedis("192.168.216.231",6379);//原先是6379
        String jsonString=jedis.get(Jedis_String);
        if(jsonString!=null)
        {
            ObjectMapper objectMapper=new ObjectMapper();
            TypeReference<List<Product_Warehouse>> typeReference =
                    new TypeReference<List<Product_Warehouse>>() {};
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
            list = product_warehouseService.selectByConstraint(stock_no,product_no,staff_no,storage_address,manufacture_date);
            ObjectMapper objectMapper=new ObjectMapper();
            try {
                String jsonString2 = objectMapper.writeValueAsString(list);
                jedis.set(Jedis_String, jsonString2);
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }


        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_PRODUCT_WAREHOUSE,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_SEARCH).append(", id: "+sta.getId()).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        //获取分页查询后的数据
        PageInfo<Product_Warehouse> pageInfo = new PageInfo<>(list);
        objectMap.put("page", queryInfo);
        if (list == null) {
            queryInfo.setRows(new ArrayList<Object>());
            queryInfo.setTotal(BusinessConstants.DEFAULT_LIST_NULL_NUMBER);
            return returnJson(objectMap, "查找不到数据", ErpInfo.OK.code);
        }
        queryInfo.setRows(list);
        queryInfo.setTotal(pageInfo.getTotal());
        String message="";
        for(int i=0;i<list.size();i++) {
            message+=list.get(i);
        }
        messageQueue.convertAndSend("Factory",message.toString());
        return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
    }

    @GetMapping(value = "/getAllMaterialsWarehouse")
    @CrossOrigin
    public String getAllMaterialsList(@RequestParam(value = Constants.PAGE_SIZE, required = false) Integer pageSize,
                          @RequestParam(value = Constants.CURRENT_PAGE, required = false) Integer currentPage,
                          @RequestParam(value = Constants.SEARCH, required = false) String search,
                          HttpServletRequest request)throws Exception {
        Map<String, String> parameterMap = new HashMap<String, String>();
        //查询参数
        JSONObject obj= JSON.parseObject(search);
        Set<String> key= obj.keySet();
        for(String keyEach: key){
            parameterMap.put(keyEach,obj.getString(keyEach));
        }
        String no=parameterMap.get("no");
        String material_no=parameterMap.get("material_no");
        String storage_address=parameterMap.get("storage_address");
        String product_date=parameterMap.get("product_date");
        String staff_no_storage=parameterMap.get("staff_no_storage");
        PageQueryInfo queryInfo = new PageQueryInfo();
        Map<String, Object> objectMap = new HashMap<String, Object>();

        if (pageSize == null || pageSize <= 0) {
            pageSize = BusinessConstants.DEFAULT_PAGINATION_PAGE_SIZE;
        }
        if (currentPage == null || currentPage <= 0) {
            currentPage = BusinessConstants.DEFAULT_PAGINATION_PAGE_NUMBER;
        }
        PageHelper.startPage(currentPage,pageSize,true);

        String Jedis_String = "Raw_Materials_Warehouse--Stock_No:"+no+"; Material_no:"+material_no+
                "; Staff_No:"+staff_no_storage + "; Manufacture_Date:"+product_date +
                "; Storage_Address:"+storage_address;
        List<Raw_Materials_Warehouse> list =new ArrayList<Raw_Materials_Warehouse>();
        Jedis jedis=new Jedis("192.168.216.231",6379);//原先是6379
        String jsonString=jedis.get(Jedis_String);
        if(jsonString!=null)
        {
            ObjectMapper objectMapper=new ObjectMapper();
            TypeReference<List<Raw_Materials_Warehouse>> typeReference =
                    new TypeReference<List<Raw_Materials_Warehouse>>() {};
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
            list = raw_materials_warehouseService.selectByConstraint(no,material_no,storage_address,staff_no_storage,product_date);
            ObjectMapper objectMapper=new ObjectMapper();
            try {
                String jsonString2 = objectMapper.writeValueAsString(list);
                jedis.set(Jedis_String, jsonString2);
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }


        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_MATERIALS_WAREHOUSE,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_SEARCH).append(", id: "+sta.getId()).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        //获取分页查询后的数据
        PageInfo<Raw_Materials_Warehouse> pageInfo = new PageInfo<>(list);
        objectMap.put("page", queryInfo);
        if (list == null) {
            queryInfo.setRows(new ArrayList<Object>());
            queryInfo.setTotal(BusinessConstants.DEFAULT_LIST_NULL_NUMBER);
            return returnJson(objectMap, "查找不到数据", ErpInfo.OK.code);
        }
        queryInfo.setRows(list);
        queryInfo.setTotal(pageInfo.getTotal());
        String message="";
        for(int i=0;i<list.size();i++) {
            message+=list.get(i);
        }
        messageQueue.convertAndSend("Factory",message.toString());
        return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
    }

    @GetMapping(value = "/getAllExpired")
    @CrossOrigin
    public String getAllExpiredList(@RequestParam(value = Constants.PAGE_SIZE, required = false) Integer pageSize,
                                      @RequestParam(value = Constants.CURRENT_PAGE, required = false) Integer currentPage,
                                      @RequestParam(value = Constants.SEARCH, required = false) String search,
                                      HttpServletRequest request)throws Exception {
        Map<String, String> parameterMap = new HashMap<String, String>();
        //查询参数
        JSONObject obj= JSON.parseObject(search);
        Set<String> key= obj.keySet();
        for(String keyEach: key){
            parameterMap.put(keyEach,obj.getString(keyEach));
        }
        String no=parameterMap.get("no");
        String name=parameterMap.get("name");
        Date date=obj.getDate("date")==null||(obj.getDate("date")+"").compareTo("")==0?null:obj.getDate("date");
        String type=parameterMap.get("type");
        PageQueryInfo queryInfo = new PageQueryInfo();
        Map<String, Object> objectMap = new HashMap<String, Object>();

        if (pageSize == null || pageSize <= 0) {
            pageSize = BusinessConstants.DEFAULT_PAGINATION_PAGE_SIZE;
        }
        if (currentPage == null || currentPage <= 0) {
            currentPage = BusinessConstants.DEFAULT_PAGINATION_PAGE_NUMBER;
        }
        PageHelper.startPage(currentPage,pageSize,true);
        List<Expired_Food> list = expired_foodService.selectByConstraint(no,type,name,date);

        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_EXPIRED,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_SEARCH).append(", id: "+sta.getId()).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        //获取分页查询后的数据
        PageInfo<Expired_Food> pageInfo = new PageInfo<>(list);
        objectMap.put("page", queryInfo);
        if (list == null) {
            queryInfo.setRows(new ArrayList<Object>());
            queryInfo.setTotal(BusinessConstants.DEFAULT_LIST_NULL_NUMBER);
            return returnJson(objectMap, "查找不到数据", ErpInfo.OK.code);
        }
        queryInfo.setRows(list);
        queryInfo.setTotal(pageInfo.getTotal());
        String message="";
        for(int i=0;i<list.size();i++) {
            message+=list.get(i);
        }
        messageQueue.convertAndSend("Factory",message.toString());
        return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
    }

    @PostMapping("/add")
    @ResponseBody
    @CrossOrigin
    public Object add(@RequestParam("info") String beanJson, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        Raw_Materials_Warehouse raw= JSON.parseObject(beanJson, Raw_Materials_Warehouse.class);
        raw_materials_warehouseService.insertRaw_Materials_Warehouse(raw);

        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_MATERIALS_WAREHOUSE,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(", id: "+sta.getId()).toString()+
                "添加信息："+raw,
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        messageQueue.convertAndSend("Factory",raw.toString());
        return result;
    }

    @PostMapping("/batchDeleteRawMaterialsByIds")
    @ResponseBody
    @CrossOrigin
    public Object batchDeleteRawMaterialsByIds(@RequestParam("ids") String ids, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        String[] id=ids.split(",");
        for(int i=0;i<id.length;i++)
        {
            raw_materials_warehouseService.deleteByPrimaryKey(id[i].trim());
        }

        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_MATERIALS_WAREHOUSE,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_DELETE).append(", id: "+sta.getId()).toString()
                +"删除信息ID组："+ids, ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        messageQueue.convertAndSend("Factory",ids.toString());
        return result;
    }

    @PostMapping("/batchDeleteProductWarehouseByIds")
    @ResponseBody
    @CrossOrigin
    public Object batchDeleteProductWarehouseByIds(@RequestParam("ids") String ids, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        String[] id=ids.split(",");
        for(int i=0;i<id.length;i++)
        {
            product_warehouseService.deleteByPrimaryKey(id[i].trim());
        }

        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_PRODUCT_WAREHOUSE,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_DELETE).append(", id: "+sta.getId()).toString()
                        +"删除信息ID组："+ids, ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        messageQueue.convertAndSend("Factory",ids.toString());
        return result;
    }


    @PostMapping("/batchDeleteExpiredFoodByIds")
    @ResponseBody
    @CrossOrigin
    public Object batchDeleteExpiredFoodByIds(@RequestParam("ids") String ids, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        String[] id=ids.split(",");
        for(int i=0;i<id.length;i++)
        {
            expired_foodService.deleteByKey(id[i].trim());
        }

        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_EXPIRED,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_DELETE).append(", id: "+sta.getId()).toString()
                        +"删除信息ID组："+ids, ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        messageQueue.convertAndSend("Factory",ids.toString());
        return result;
    }

    @PostMapping("/Productadd")
    @ResponseBody
    @CrossOrigin
    public Object Productadd(@RequestParam("info") String beanJson, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        Product_Warehouse raw= JSON.parseObject(beanJson, Product_Warehouse.class);
        product_warehouseService.insertProduct_Warehouse(raw);
        //        log
        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_PRODUCT_WAREHOUSE,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(", id: "+sta.getId()).toString()+
                "添加信息："+raw, ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        messageQueue.convertAndSend("Factory",raw.toString());
        return result;
    }

    @PostMapping("/update")
    @ResponseBody
    @CrossOrigin
    public Object update(@RequestParam("info") String beanJson,@RequestParam("id") Long id,
                         @RequestParam(value = "Product_no", required = false) String Product_no,
                         @RequestParam(value = "prodate", required = false) Date prodate, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        Product_Warehouse raw= JSON.parseObject(beanJson, Product_Warehouse.class);
        raw.setStock_no(id.toString());
        raw.setProduct_no(Product_no);
        raw.setManufacture_date(prodate);
        product_warehouseService.updateProduct_Warehouse(raw);
        //        log
        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_PRODUCT_WAREHOUSE,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(", id: "+sta.getId()).toString()+"修改信息："+raw,
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        messageQueue.convertAndSend("Factory",raw.toString());
        return result;
    }

    @PostMapping("/updateMaterials")
    @ResponseBody
    @CrossOrigin
    public Object updateMaterials(@RequestParam("info") String beanJson,@RequestParam("id") String id,
                         @RequestParam(value = "Product_no", required = false) String Product_no,
                         @RequestParam(value = "prodate", required = false) Date prodate, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        Raw_Materials_Warehouse rmw=JSON.parseObject(beanJson, Raw_Materials_Warehouse.class);
        rmw.setStock_no(id.toString());
        raw_materials_warehouseService.updateRaw_Materials_Warehouse(rmw);
        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_PRODUCT_WAREHOUSE,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(", id: "+sta.getId()).toString()+"修改信息："+rmw,
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        messageQueue.convertAndSend("Factory",rmw.toString());
        return result;
    }


    @PostMapping("/updateExpired")
    @ResponseBody
    @CrossOrigin
    public Object updateExpired(@RequestParam("info") String beanJson,@RequestParam("id") String id,
                                  @RequestParam(value = "Product_no", required = false) String Product_no,
                                  @RequestParam(value = "prodate", required = false) Date prodate, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        Expired_Food rmw=JSON.parseObject(beanJson, Expired_Food.class);
        rmw.setFood_no(id.toString());
        expired_foodService.updateByPrimaryKey(rmw);
        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_EXPIRED,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(", id: "+sta.getId()).toString()+"修改信息："+rmw,
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        messageQueue.convertAndSend("Factory",rmw.toString());
        return result;
    }


    @PostMapping("/updateproductwarehouse")
    @ResponseBody
    @CrossOrigin
    public Object updateproductwarehouse(@RequestParam("info") String beanJson,@RequestParam("id") String id,
                                @RequestParam(value = "Product_no", required = false) String Product_no,
                                @RequestParam(value = "prodate", required = false) Date prodate, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        Product_Warehouse rmw=JSON.parseObject(beanJson, Product_Warehouse.class);
        rmw.setStock_no(id.toString());
        product_warehouseService.updateProduct_Warehouse(rmw);
        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_PRODUCT_WAREHOUSE,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(", id: "+sta.getId()).toString()
                +"修改信息："+rmw, ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        messageQueue.convertAndSend("Factory",rmw.toString());
        return result;
    }
}
