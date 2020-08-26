# 在线修改MySQL大表的表结构

## 1 抛出问题
&emsp;&emsp;dba的日常工作肯定有一项是ddl变更，ddl变更会锁表，这个可以说是dba心中永远的痛，特别是执行ddl变更，
导致库上大量线程处于“Waiting for meta data lock”状态的时候，下面是解决思路与方案。


## 2 背景知识：

 - 5.6之前：

    5.6 online ddl推出以前，执行ddl主要有两种方式copy方式和inplace方式，inplace方式又称为(fast index creation)。相对于copy方式，
    inplace方式不拷贝数据，因此较快。但是这种方式仅支持添加、删除索引两种方式，而且与copy方式一样需要全程锁表，实用性不是很强。
    
    我们知道在MySQL中如果要执行ALTER TABLE操作，MySQL会通过制作原来表的一个临时副本来工作。

    对于表结构的修改在副本上施行，然后将新表替换原始表，此时会产生锁表，用户可以从原始表读取数据，而用户的更新和写入操作都会被lock，待新表准备好后写入新表。

    这对于在线的数据量较大的表来说是绝对无法容忍的，并且由于这种在线操作时间会很长，此时如果show processlist，会发现有若干的MySQL进程处于lock状态，当这种进程太多超过单台服务器允许的MySQL进程数，其它进程可能会被拒绝连接。
    
 - 5.6之后
 
    online方式实质也包含了copy和inplace方式.

	对于不支持online的ddl操作采用copy方式，比如修改列类型，删除主键，修改字符集等，
    这些操作都会导致记录格式发生变化，无法通过简单的全量+增量的方式实现online；

    对于inplace方式，mysql内部以“是否修改记录格式”为基准也分为两类:
    - rebuild方式: 需要重建表(重新组织记录)，比如optimize table、添加索引、添加/删除列、 修改列NULL/NOT NULL属性等；
    - no-rebuild方式: 另外一类是只需要修改表的元数据，比如删除索引、修改列名、修改列默认值、修改列自增值等。

## 3 online ddl
online ddl主要包括3个阶段，prepare阶段，ddl执行阶段，commit阶段，rebuild方式比no-rebuild方式实质多了一个ddl执行阶段，prepare阶段和commit阶段类似。

 - Prepare阶段：

    创建新的临时frm文件
    
    持有EXCLUSIVE-MDL锁，禁止读写
    
    根据alter类型，确定执行方式(copy,online-rebuild,online-norebuild)
    
    更新数据字典的内存对象
    
    分配row_log对象记录增量
    
    生成新的临时ibd文件
    
 - ddl执行阶段：

    降级EXCLUSIVE-MDL锁，允许读写
    
    扫描old_table的聚集索引每一条记录rec
    
    遍历新表的聚集索引和二级索引，逐一处理
    
    根据rec构造对应的索引项
    
    将构造索引项插入sort_buffer块
    
    将sort_buffer块插入新的索引
    
    处理ddl执行过程中产生的增量(仅rebuild类型需要)
    
 - commit阶段

    升级到EXCLUSIVE-MDL锁，禁止读写
    
    重做最后row_log中最后一部分增量
    
    更新innodb的数据字典表
    
    提交事务(刷事务的redo日志)
    
    修改统计信息
    
    rename临时idb文件，frm文件
    
    变更完成  
    
### 3.1 online ddl常见操作及并发情况：

参考：[Mysql online DDL特性（二）https://blog.csdn.net/finalkof1983/article/details/88379731](https://blog.csdn.net/finalkof1983/article/details/88379731 "Mysql online DDL特性（二）")

### 3.2 相关问题
1.如何实现数据完整性

使用online ddl后，用户心中一定有一个疑问，一边做ddl，一边做dml，表中的数据不会乱吗？这里面关键部件是row_log。
row_log记录了ddl变更过程中新产生的dml操作，并在ddl执行的最后将其应用到新的表中，保证数据完整性。

2.online与数据一致性如何兼得

实际上，online ddl并非整个过程都是online，在prepare阶段和commit阶段都会持有MDL-Exclusive锁，禁止读写；
而在整个ddl执行阶段，允许读写。由于prepare和commit阶段相对于ddl执行阶段时间特别短，因此基本可以认为是全程online的。
Prepare阶段和commit阶段的禁止读写，主要是为了保证数据一致性。Prepare阶段需要生成row_log对象和修改内存的字典；
Commit阶段，禁止读写后，重做最后一部分增量，然后提交，保证数据一致。
###  3.3 实验

 实验如下：

 1. 创建一个person表，一个创建create_person的存储过程
 
        CREATE TABLE `person` (
           `id` int(10) NOT NULL AUTO_INCREMENT,
           `name` varchar(50) DEFAULT NULL,
           `age` int(2) DEFAULT NULL,
           `sex` varchar(2) DEFAULT NULL,
           `company_id` int(10) DEFAULT NULL,
           PRIMARY KEY (`id`)
         ) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8;
             
 2. 使用存储过程创建80000条数据
 
        CREATE DEFINER=`root`@`localhost` PROCEDURE `create_person`(IN `num`  int)
        BEGIN
          DECLARE i int DEFAULT 0;
            WHILE i<num DO
                insert into person VALUES((SELECT a.count from (select MAX(id) as count from person) as a)+1,CONCAT("zhangsan",(SELECT a.count from (select MAX(id) as count from person) as a)+1),18,"男",1);
                set i=i+1;
          END WHILE;
        END
        
 3. 对person表执行添加列操作 (数据量大到发送请求时还没有添加完成),同时使用postman对person表发送添加请求。
		
        [SQL] alter table person add column `test1` varchar(30);
        受影响的行: 0
        时间: 7.151s
    _添加列耗费时间7秒左右，rows affected =0 （说明没有锁表） 请求时间71ms  结论：没有影响，已经优化_
    
 4. 对person表执行删除列操作 (数据量大到发送请求时还没有添加完成),同时使用postman对person表发送添加请求。
 
        [SQL] alter table person drop  `test1`;
        受影响的行: 0
        时间: 6.337s
    _删除列耗费时间7秒左右， rows affected =0 （说明没有锁表）请求时间131ms   结论：没有影响，已经优化_
    
 5. 对person表执行某个字段添加普通索引操作 (数据量大到发送请求时还没有添加完成),同时使用postman对person表发送添加请求。
 
        [SQL] ALTER table person  ADD INDEX psn_name_index(`name`)
        
        受影响的行: 0
        时间: 1.931s
    _添加索引耗费时间2秒左右，rows affected =0  （说明没有锁表） 请求时间42ms  结论：没有影响，已经优化_
    
 6. 对person表执行某个字段删除普通索引操作 (数据量大到发送请求时还没有添加完成),同时使用postman对person表发送添加请求。    
 
        [SQL] DROP INDEX psn_name_index ON person;
        受影响的行: 0
        时间: 0.149s

### 3.4 结论

1.  如果当前系统中，对于修改的表执行插入语句是指定字段名称的，如  **insert A('name','xx') values ('zhangsan','xxxx')** 
   那么添加列可以直接使用online ddl不会对系统造成延迟影响。删除插入列直接程序就出异常了，尽管他不会有延迟问题。可以采用trigger方案。
   
2.  如果当前系统中，对于修改的表执行插入所有语句没有指定字段名称的，如 **insert A values ('zhangsan','xxxx')**  那么如果添加删除列，程序就出异常了。可以采用trigger方案。

3.  对于添加删除索引可以直接使用ddl，mysql5.6之后没有影响。

4.  对于如修改列类型，删除主键，修改字符集，ddl会锁表，无法进行写表。可以采用trigger方案。

5.  在对大表做DDL操作的时候，或许不知道此操作是否会发生copying data操作（会锁表），此时可以先建一个同结构的表tmp,
导入少量数据然后对tmp做DDL，操作，查看rows affected返回值，如果确认会copying data，那就需要酌情考虑别的方法了。

## 4 Trigger方案

触发器方案的过程：创建一张已经修改了的新表，在旧表上写insert,update,delete触发器，把对旧表的操作，同步到新表。只要新表有数据就知道了当前的id值，在同时插入旧表的历史数据到新表，接着rename，从而无缝衔接。

### 4.1 Trigger方案实现实验

1. 按需求创建新表

        [SQL] CREATE TABLE `person_temp` (
          `id` int(10) NOT NULL AUTO_INCREMENT,
          `name` varchar(50) DEFAULT NULL,
          `age` int(2) DEFAULT NULL,
          `sex` varchar(2) DEFAULT NULL,
          `company_id` int(10) DEFAULT NULL,
          `testadd` varchar(30) DEFAULT NULL COMMENT '测试列',
          PRIMARY KEY (`id`)
        ) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8;
        
        受影响的行: 0
        时间: 0.431s

2. 针对原始表创建触发器

        CREATE TRIGGER `trigger_insert` AFTER INSERT ON `person` FOR EACH ROW begin
           insert into person_temp(id,`name`,age,sex,company_id,testadd) 
               values(new.id,new.`name`,new.age,new.sex,new.company_id,'test');
        end;
        
3. 对于原始表的更新操作都会被触发器更新到新表中,获取第一个id
        
        85076  |  张三测试添加列了  |  18  |  男  |  1  |  test
        
4. 把原始表中的数据复制到新表中
       
       [SQL] INSERT INTO person_temp(id,`name`,age,sex,company_id)
       SELECT old.id,old.`name`,old.age,old.sex,old.company_id FROM person old where old.id<85076
       
       受影响的行: 84081
       时间: 1.905s
    
5. 停机，将新表替换旧表，删除旧表，重命名新表，同时更新后台，做到无缝衔接

        [SQL] rename table person_temp to person;
        受影响的行: 0
        时间: 0.098s


参考文章：

[MYSQL 在线DDL操作是否copying data](https://www.cnblogs.com/zuoxingyu/archive/2013/03/28/2986715.html "MYSQL 在线DDL操作是否copying data")

[Mysql online DDL特性（一）](https://blog.csdn.net/finalkof1983/article/details/88355314 "Mysql online DDL特性（一）")