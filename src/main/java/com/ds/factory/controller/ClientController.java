package com.ds.factory.controller;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ds.factory.constants.ExceptionConstants;
import com.ds.factory.datasource.entities.Client;
import com.ds.factory.datasource.entities.Product_Warehouse;
import com.ds.factory.datasource.entities.Staff;
import com.ds.factory.exception.BusinessParamCheckingException;
import com.ds.factory.service.Service.ClientService;
import com.ds.factory.service.Service.LogService;
import com.ds.factory.service.Service.StaffService;
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
import org.springframework.web.bind.annotation.*;
import com.ds.factory.constants.BusinessConstants;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import redis.clients.jedis.Jedis;
import sun.swing.StringUIClientPropertyKey;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

import static com.ds.factory.utils.ResponseJsonUtil.returnJson;


@RestController
@RequestMapping(value = "/client")
public class ClientController {
    private Logger logger = LoggerFactory.getLogger(UserController.class);

    @Resource
    LogService logService;

    @Resource
    private ClientService clientService;

    private static String message = "成功";
    private static final String HTTP = "http://";
    private static final String CODE_OK = "200";

    @Autowired
    AmqpTemplate messageQueue;

    @GetMapping(value = "/getAllStaff")
    @CrossOrigin
    public String getList(@RequestParam(value = Constants.PAGE_SIZE, required = false) Integer pageSize,
                          @RequestParam(value = Constants.CURRENT_PAGE, required = false) Integer currentPage,
                          @RequestParam(value = Constants.SEARCH, required = false) String search,
                          HttpServletRequest request)throws Exception {

        Map<String, String> parameterMap = ParamUtils.requestToMap(request);
        parameterMap.put(Constants.SEARCH, search);
        JSONObject obj= JSON.parseObject(search);

        String no=obj.getString("no")==null?"":obj.getString("no").trim();
        String name=obj.getString("name")==null?"":obj.getString("name").trim();
        String type=obj.getString("type")==null?"":obj.getString("type").trim();

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

        String Jedis_String = "Client--No:"+no+"; Name:"+name+"; Type:"+type;
        List<Client> list = clientService.selectBy_partName_and_Type(no,name,type);

        PageInfo<Client> pageInfo = new PageInfo<>(list);
        objectMap.put("page", queryInfo);
        if (list == null) {
            queryInfo.setRows(new ArrayList<Object>());
            queryInfo.setTotal(BusinessConstants.DEFAULT_LIST_NULL_NUMBER);
            return returnJson(objectMap, "查找不到数据", ErpInfo.OK.code);
        }
        queryInfo.setRows(list);
        queryInfo.setTotal(pageInfo.getTotal());

        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_client,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_SEARCH).append(", id: "+sta.getId()).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        String message="";
        for(int i=0;i<list.size();i++) {
            message+=list.get(i);
        }
        messageQueue.convertAndSend("Factory",message.toString());
        return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
    }


    @PostMapping("/batchDeleteClientByIds")
    @ResponseBody
    @CrossOrigin
    public Object batchDeleteClientByIds(@RequestParam("ids") String ids,HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        String[] id=ids.split(",");
        for(int i=0;i<id.length;i++)
        {
            clientService.deleteByPrimaryKey(id[i].trim());
        }

        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_client,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_BATCH_delete).append(", id: "+sta.getId()).toString() +
                "删除信息ID组："+ids,
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());

        messageQueue.convertAndSend("Factory",ids.toString());
        return result;
    }


    @PostMapping("/add")
    @ResponseBody
    @CrossOrigin
    public Object add(@RequestParam("info") String beanJson, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        Client client= JSON.parseObject(beanJson, Client.class);
        clientService.insert_Client_details(client);
        client.setPassword("“密保信息，管理员无权查看，初始密码默认123456”");
        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_client,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(", id: "+sta.getId()).toString() +
                "添加信息："+client,
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        messageQueue.convertAndSend("Factory",client.toString());
        return result;
    }


    @PostMapping("/update")
    @ResponseBody
    @CrossOrigin
    public Object update(@RequestParam("info") String beanJson,@RequestParam("id") Long id, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        Client client= JSON.parseObject(beanJson, Client.class);
        String Client_no=(id+"").trim();

        switch (Client_no.length()){
            case 1: Client_no="00000"+Client_no;break;
            case 2: Client_no="0000"+Client_no;break;
            case 3: Client_no="000"+Client_no;break;
            case 4: Client_no="00"+Client_no;break;
            case 5: Client_no="0"+Client_no;break;
            case 6: break;
        }
        client.setClient_no(Client_no);
        clientService.updateByPrimaryKeySelective(client);

        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_client,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(", id: "+sta.getId()).toString() +
                "修改信息："+client,
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        messageQueue.convertAndSend("Factory",client.toString());
        return result;
    }

}
