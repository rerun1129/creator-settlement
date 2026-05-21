package com.creatorsettlement.configuration;

import com.creatorsettlement.domain.repository.SalesRepository;
import com.creatorsettlement.domain.service.RefundPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainBeanConfiguration {

    @Bean
    public RefundPolicy refundPolicy(SalesRepository salesRepository) {
        return new RefundPolicy(salesRepository);
    }
}
