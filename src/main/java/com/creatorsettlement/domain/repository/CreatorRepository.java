package com.creatorsettlement.domain.repository;

import com.creatorsettlement.domain.model.creator.Creator;
import com.creatorsettlement.domain.model.vo.CreatorId;

public interface CreatorRepository {

    void saveCreator(Creator creator);

    boolean existsByCreatorId(CreatorId creatorId);
}
