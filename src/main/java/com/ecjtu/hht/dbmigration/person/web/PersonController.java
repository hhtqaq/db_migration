package com.ecjtu.hht.dbmigration.person.web;


import com.ecjtu.hht.dbmigration.person.entity.Person;
import com.ecjtu.hht.dbmigration.person.service.IPersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author hht
 * @since 2020-08-24
 */
@RestController
public class PersonController {
    @Autowired
    private IPersonService personService;

    /**
     * 查询
     *
     * @param id
     * @return
     */
    @GetMapping("/person/{id}")
    public Person getPerson(@PathVariable(name = "id") Integer id) {
        return personService.selectById(id);
    }

    /**
     * 修改
     *
     * @param id
     * @return
     */
    @PutMapping("/person")
    public boolean updatePerson(@RequestBody Person person) {
        return personService.updateById(person);
    }

    /**
     * 添加
     *
     * @param person
     * @return
     */
    @PostMapping("/person")
    public boolean savePerson(@RequestBody Person person) {
        return personService.insert(person);
    }

    /**
     * 删除
     *
     * @param person
     * @return
     */
    @DeleteMapping("/person/{id}")
    public boolean delPerson(@PathVariable(name = "id") Integer id) {
        return personService.deleteById(id);
    }
}
