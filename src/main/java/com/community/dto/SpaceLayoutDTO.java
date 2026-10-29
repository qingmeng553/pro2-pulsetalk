package com.community.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/** 保存主页布局（layout 为 schema，version 为乐观锁版本）。 */
@Data
public class SpaceLayoutDTO {

    private JsonNode layout;

    private Integer version;
}
