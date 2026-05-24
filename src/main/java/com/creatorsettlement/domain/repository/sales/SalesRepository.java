package com.creatorsettlement.domain.repository.sales;

import com.creatorsettlement.domain.model.sales.CancellationRecord;
import com.creatorsettlement.domain.model.sales.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.domain.repository.sales.dto.CancellationSummary;
import com.creatorsettlement.domain.repository.sales.dto.MonthlyCancellationAggregate;
import com.creatorsettlement.domain.repository.sales.dto.MonthlySalesAggregate;
import com.creatorsettlement.domain.repository.sales.dto.SalesRecordView;
import com.creatorsettlement.domain.repository.sales.dto.SalesRecordWithId;
import com.creatorsettlement.domain.repository.sales.dto.SalesSummary;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface SalesRepository {

    void saveSalesRecord(SalesRecord salesRecord);

    boolean existsById(SalesRecordId salesRecordId);

    Optional<SalesRecord> findById(SalesRecordId salesRecordId);

    List<CancellationRecord> findCancellationsBySalesRecordId(SalesRecordId salesRecordId);

    void saveCancellationRecord(CancellationRecord cancellationRecord);

    List<SalesRecordView> findAllSalesView(LocalDateTime from, LocalDateTime toExclusive);

    List<SalesRecordView> findSalesView(CreatorId creatorId, LocalDateTime from, LocalDateTime toExclusive);

    List<SalesRecordWithId> findByCourseIdAndStudentId(CourseId courseId, StudentId studentId);

    SalesSummary findSalesSummaryByCreatorAndMonth(CreatorId creatorId, YearMonth yearMonth);

    CancellationSummary findCancellationSummaryByCreatorAndMonth(CreatorId creatorId, YearMonth yearMonth);

    List<MonthlySalesAggregate> findMonthlySalesAggregates(LocalDateTime from, LocalDateTime toExclusive);

    List<MonthlyCancellationAggregate> findMonthlyCancellationAggregates(LocalDateTime from, LocalDateTime toExclusive);
}
