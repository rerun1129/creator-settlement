package com.creatorsettlement.configuration;

import com.creatorsettlement.domain.repository.course.CourseRepository;
import com.creatorsettlement.domain.repository.creator.CreatorRepository;
import com.creatorsettlement.domain.repository.fee.FeePolicyRepository;
import com.creatorsettlement.domain.repository.sales.SalesRepository;
import com.creatorsettlement.domain.repository.settlement.SettlementRepository;
import com.creatorsettlement.domain.service.fee.FeePolicyDomainService;
import com.creatorsettlement.domain.service.sales.CancellationRegistrationPolicy;
import com.creatorsettlement.domain.service.sales.RefundPolicy;
import com.creatorsettlement.domain.service.sales.SaleRegistrationPolicy;
import com.creatorsettlement.domain.service.settlement.CreatorRangePayoutCalculator;
import com.creatorsettlement.domain.service.settlement.MonthlySettlementCalculator;
import com.creatorsettlement.domain.service.settlement.PendingSettlementResolver;
import com.creatorsettlement.domain.service.settlement.RequiredSettlementResolver;
import com.creatorsettlement.domain.service.settlement.SettlementAmountCalculator;
import com.creatorsettlement.domain.service.settlement.SettlementMonthClosurePolicy;
import com.creatorsettlement.domain.service.settlement.SettlementRangePayoutAssembler;
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

    @Bean
    public FeePolicyDomainService feePolicyDomainService(FeePolicyRepository feePolicyRepository) {
        return new FeePolicyDomainService(feePolicyRepository);
    }

    @Bean
    public CreatorRangePayoutCalculator creatorRangePayoutCalculator(SettlementAmountCalculator settlementAmountCalculator) {
        return new CreatorRangePayoutCalculator(settlementAmountCalculator);
    }

    @Bean
    public PendingSettlementResolver pendingSettlementResolver(SalesRepository salesRepository, FeePolicyDomainService feePolicyDomainService, MonthlySettlementCalculator monthlySettlementCalculator) {
        return new PendingSettlementResolver(salesRepository, feePolicyDomainService, monthlySettlementCalculator);
    }

    @Bean
    public RequiredSettlementResolver requiredSettlementResolver(SettlementRepository settlementRepository) {
        return new RequiredSettlementResolver(settlementRepository);
    }

    @Bean
    public SettlementMonthClosurePolicy settlementMonthClosurePolicy() {
        return new SettlementMonthClosurePolicy();
    }

    @Bean
    public SettlementRangePayoutAssembler settlementRangePayoutAssembler(SalesRepository salesRepository, CreatorRepository creatorRepository, CreatorRangePayoutCalculator creatorRangePayoutCalculator, FeePolicyDomainService feePolicyDomainService) {
        return new SettlementRangePayoutAssembler(salesRepository, creatorRepository, creatorRangePayoutCalculator, feePolicyDomainService);
    }
}
