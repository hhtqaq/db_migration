package com.ecjtu.hht.dbmigration.company.entity;

import com.baomidou.mybatisplus.activerecord.Model;

import java.io.Serializable;


import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.Version;

import com.baomidou.mybatisplus.enums.IdType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 *
 * </p>
 *
 * @author hht
 * @since 2020-08-24
 */
@Data
@Accessors(chain = true)
public class Company {

    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String name;


}
