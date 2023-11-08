package com.meteoriqs.foodswing.common.mappings;

import lombok.SneakyThrows;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean(destroyMethod = "shutdown")
    @SneakyThrows
    public MemcachedClient memcachedClient() {
        return new MemcachedClient(
                new ConnectionFactoryBuilder()
                        .setDaemon(true)
                        .setFailureMode(FailureMode.Retry)
                        .build(),
//                AddrUtil.getAddresses("68.178.171.57:1121"));
                AddrUtil.getAddresses("localhost:11211"));
    }
}