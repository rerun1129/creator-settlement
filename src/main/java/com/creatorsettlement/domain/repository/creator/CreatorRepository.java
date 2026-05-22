package com.creatorsettlement.domain.repository.creator;

import com.creatorsettlement.domain.model.creator.Creator;
import com.creatorsettlement.domain.model.vo.CreatorId;

public interface CreatorRepository {

    void saveCreator(Creator creator);

    boolean existsByCreatorId(CreatorId creatorId);
}
