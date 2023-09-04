package se.sundsvall.invoicesender.configuration;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

import se.sundsvall.invoicesender.Application;

@Configuration
@EnableFeignClients(basePackageClasses = Application.class)
class FeignConfiguration {

}
