package com.creatorsettlement.infrastructure.persistence;

import com.creatorsettlement.domain.model.creator.Creator;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.repository.creator.CreatorRepository;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryCreatorRepository implements CreatorRepository {

    private final ConcurrentMap<CreatorId, Creator> creatorsById = new ConcurrentHashMap<>();

    @Override
    public void saveCreator(Creator creator) {
        creatorsById.put(creator.creatorId(), creator);
    }

    @Override
    public boolean existsByCreatorId(CreatorId creatorId) {
        return creatorsById.containsKey(creatorId);
    }

    @Override
    public List<CreatorId> findAllCreatorIds() {
        throw new UnsupportedOperationException("단계 3 어플리케이션 GREEN에서 구현");
    }
}
