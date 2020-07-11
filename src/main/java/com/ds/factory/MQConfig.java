package com.ds.factory;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//message queue
@Configuration
public class MQConfig {
    //创建队列
    //org.springframework.amqp.core.Queue
    //@Bean 方法返回的队列给controller使用
    @Bean
    public Queue createQueue() {
        Queue queue=new Queue("Factory");
        return queue;
    }

}
