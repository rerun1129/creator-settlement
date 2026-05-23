package com.creatorsettlement.infrastructure.persistence.creator;

import com.creatorsettlement.domain.model.creator.Creator;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.repository.creator.CreatorRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaCreatorRepository implements CreatorRepository {

    private final CreatorJpaDataRepository dataRepository;

    public JpaCreatorRepository(CreatorJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public void saveCreator(Creator creator) {
        dataRepository.save(CreatorMapper.toEntity(creator));
    }

    @Override
    public boolean existsByCreatorId(CreatorId creatorId) {
        return dataRepository.existsById(creatorId.value());
    }

    @Override
    public List<CreatorId> findAllCreatorIds() {
        return dataRepository.findAllIds().stream().map(CreatorId::of).toList();
    }
}
