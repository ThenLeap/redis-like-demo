package com.yixin.light.models.entity;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserLikes extends Model<UserLikes> {


    @ApiModelProperty("点赞信息ID")
    private String id;

    @ApiModelProperty("点赞对象id")
    private String infoId;

    @ApiModelProperty("时间")
    private Date createTime;

    @ApiModelProperty("点赞人ID")
    private String likeUserId;

    @ApiModelProperty("$column.comment")
    private Date updateTime;

    @ApiModelProperty("0 取消 1 点赞")
    private Integer status;

}

