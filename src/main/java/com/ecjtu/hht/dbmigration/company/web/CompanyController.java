package com.ecjtu.hht.dbmigration.company.web;


import com.ecjtu.hht.dbmigration.company.entity.Company;
import com.ecjtu.hht.dbmigration.company.service.ICompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author hht
 * @since 2020-08-24
 */
@RestController
public class CompanyController {

    @Autowired
    private ICompanyService companyService;

    /**
     * 查询
     *
     * @param id
     * @return
     */
    @GetMapping("/company/{id}")
    public Company getCompany(@PathVariable(name = "id") Integer id) {
        return companyService.selectById(id);
    }

    /**
     * 修改
     *
     * @param id
     * @return
     */
    @PutMapping("/company")
    public boolean updateCompany(@RequestBody Company company) {
        return companyService.updateById(company);
    }

    /**
     * 添加
     *
     * @param company
     * @return
     */
    @PostMapping("/company")
    public boolean saveCompany(@RequestBody Company company) {
        return companyService.insert(company);
    }

    /**
     * 删除
     *
     * @param company
     * @return
     */
    @DeleteMapping("/company/{id}")
    public boolean delCompany(@PathVariable(name = "id") Integer id) {
        return companyService.deleteById(id);
    }
}
