package com.ecjtu.hht.dbmigration.person.mapper;

import com.ecjtu.hht.dbmigration.person.entity.Person;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author hht
 * @since 2020-08-24
 */
@Mapper
public interface PersonMapper extends BaseMapper<Person> {

}
