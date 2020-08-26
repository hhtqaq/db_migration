package com.ecjtu.hht.dbmigration.company.service.impl;

import com.ecjtu.hht.dbmigration.company.entity.Company;
import com.ecjtu.hht.dbmigration.company.mapper.CompanyMapper;
import com.ecjtu.hht.dbmigration.company.service.ICompanyService;
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
public class CompanyServiceImpl extends ServiceImpl<CompanyMapper, Company> implements ICompanyService {

}
