package com.yixin.light.models.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLikCountDTO implements Serializable {

    private String infoId;
    private Integer value;

    public UserLikCountDTO(String infoId, Integer value) {
        this.infoId = infoId;
        this.value = value;
    }
}
