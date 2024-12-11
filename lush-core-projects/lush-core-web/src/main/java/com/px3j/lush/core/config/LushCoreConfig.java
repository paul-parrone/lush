package com.px3j.lush.core.config;

import com.px3j.lush.core.util.YamlPropertySourceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


@Configuration
@ConfigurationProperties( prefix = "yaml" )
@PropertySource( value = "classpath:lush-config.yml", factory = YamlPropertySourceFactory.class)
@Slf4j( topic = "lush.core.debug")
public class LushCoreConfig {
    public LushCoreConfig() {
        log.debug( "Lush :: LushCoreConfig initialization" );
    }
}
