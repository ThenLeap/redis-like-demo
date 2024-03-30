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
public class Video extends Model<Video> {


    @ApiModelProperty("$column.comment")
    private String id;

    @ApiModelProperty("点赞数")
    private Integer likesNumber;

    @ApiModelProperty("评论数")
    private Integer commentsNumber;

    @ApiModelProperty("分享数")
    private Integer shareNumber;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("创建者")
    private String createUser;

    @ApiModelProperty("更新时间")
    private Date updateTime;

    @ApiModelProperty("更新者")
    private String updateUser;

}

