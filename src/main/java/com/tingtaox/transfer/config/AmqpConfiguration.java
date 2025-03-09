package com.tingtaox.transfer.config;

import static com.tingtaox.transfer.models.Constants.EXCHANGE;
import static com.tingtaox.transfer.models.Constants.TRANSACTION_FAILURE_QUEUE;
import static com.tingtaox.transfer.models.Constants.RETRY_CACHE_EVICTION_QUEUE;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmqpConfiguration {

    @Bean
    public Queue retryQueue() {
        return new Queue(RETRY_CACHE_EVICTION_QUEUE,true);
    }

    @Bean
    public Queue failureQueue() {
        return new Queue(TRANSACTION_FAILURE_QUEUE,true);
    }

    @Bean
    DirectExchange directExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    Binding retryQueueBinding() {
        return BindingBuilder.bind(retryQueue()).to(directExchange()).with(RETRY_CACHE_EVICTION_QUEUE);
    }

    @Bean
    Binding failureQueueBinding() {
        return BindingBuilder.bind(failureQueue()).to(directExchange()).with(TRANSACTION_FAILURE_QUEUE);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
