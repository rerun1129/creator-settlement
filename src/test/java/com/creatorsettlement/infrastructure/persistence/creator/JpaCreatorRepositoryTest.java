package com.creatorsettlement.infrastructure.persistence.creator;

import com.creatorsettlement.domain.model.creator.Creator;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.repository.creator.CreatorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("jpa-test")
@Import(JpaCreatorRepository.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaCreatorRepositoryTest {

    @Autowired
    private CreatorRepository sut;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("saveCreator 호출 시 creator 테이블에 행이 INSERT된다")
    void saveCreator_inserts_row() {
        // given
        Creator creator = Creator.of(CreatorId.of(0L), "크리에이터 A");

        // when
        sut.saveCreator(creator);
        em.flush();
        em.clear();

        // then
        Long count = em.getEntityManager()
                .createQuery("select count(c) from CreatorJpaEntity c", Long.class)
                .getSingleResult();
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("existsByCreatorId는 DB에 존재하는 id에 대해 true를 반환한다")
    void existsByCreatorId_returnsTrue_whenIdExists() {
        // given
        CreatorJpaEntity entity = em.persistAndFlush(CreatorJpaEntity.of("크리에이터 A"));
        em.clear();

        // when
        boolean result = sut.existsByCreatorId(CreatorId.of(entity.getId()));

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByCreatorId는 DB에 존재하지 않는 id에 대해 false를 반환한다")
    void existsByCreatorId_returnsFalse_whenIdMissing() {
        // when
        boolean result = sut.existsByCreatorId(CreatorId.of(999L));

        // then
        assertThat(result).isFalse();
    }
}
