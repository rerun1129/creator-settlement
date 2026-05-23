package com.creatorsettlement.domain.repository.sales;

import com.creatorsettlement.domain.model.sales.CancellationRecord;
import com.creatorsettlement.domain.model.sales.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.model.vo.StudentId;

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

    List<SalesRecordView> findSalesView(Optional<CreatorId> creatorId, LocalDateTime from, LocalDateTime toExclusive);

    List<SalesRecordWithId> findByCourseIdAndStudentId(CourseId courseId, StudentId studentId);

    SalesSummary findSalesSummaryByCreatorAndMonth(CreatorId creatorId, YearMonth yearMonth);

    CancellationSummary findCancellationSummaryByCreatorAndMonth(CreatorId creatorId, YearMonth yearMonth);
}
