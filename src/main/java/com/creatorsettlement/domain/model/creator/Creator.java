package com.creatorsettlement.domain.model.creator;

import com.creatorsettlement.domain.model.vo.CreatorId;

public record Creator(CreatorId creatorId, String name) {

    public static Creator of(CreatorId creatorId, String name) {
        return new Creator(creatorId, name);
    }
}
