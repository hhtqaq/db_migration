package com.ecjtu.hht.dbmigration.person.service.impl;

import com.ecjtu.hht.dbmigration.person.entity.Person;
import com.ecjtu.hht.dbmigration.person.mapper.PersonMapper;
import com.ecjtu.hht.dbmigration.person.service.IPersonService;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author hht
 * @since 2020-08-24
 */
@Service
public class PersonServiceImpl extends ServiceImpl<PersonMapper, Person> implements IPersonService {

}
