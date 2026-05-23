package com.creatorsettlement.configuration;

import com.creatorsettlement.domain.repository.course.CourseRepository;
import com.creatorsettlement.domain.repository.sales.SalesRepository;
import com.creatorsettlement.domain.service.sales.CancellationRegistrationPolicy;
import com.creatorsettlement.domain.service.sales.RefundPolicy;
import com.creatorsettlement.domain.service.sales.SaleRegistrationPolicy;
import com.creatorsettlement.domain.service.settlement.MonthlySettlementCalculator;
import com.creatorsettlement.domain.service.settlement.SettlementAmountCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainBeanConfiguration {

    @Bean
    public RefundPolicy refundPolicy(SalesRepository salesRepository) {
        return new RefundPolicy(salesRepository);
    }

    @Bean
    public SaleRegistrationPolicy saleRegistrationPolicy(CourseRepository courseRepository, SalesRepository salesRepository) {
        return new SaleRegistrationPolicy(courseRepository, salesRepository);
    }

    @Bean
    public CancellationRegistrationPolicy cancellationRegistrationPolicy(SalesRepository salesRepository) {
        return new CancellationRegistrationPolicy(salesRepository);
    }

    @Bean
    public MonthlySettlementCalculator monthlySettlementCalculator() {
        return new MonthlySettlementCalculator();
    }

    @Bean
    public SettlementAmountCalculator settlementAmountCalculator() {
        return new SettlementAmountCalculator();
    }
}
