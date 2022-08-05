package com.Xie.test;

import com.Xie.controller.BookController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @Description:
 * 声明式事务的配置步骤
 * 1.在spring配置文件中配置事务管理器
 * 2.开启事务的注解驱动
 * 3.在需要事务的方法上使用@Transactional注解
 * @Transactional注解标识的位置：
 * 1.方法上
 * 2.类上，则表示该类中的所有方法都是事务方法
 * @author: XieFeiYu
 * @eamil: 32096231@qq.com
 * @date:2022/8/4 19:40
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:tx-xml.xml")
public class TxByXMLTest {

    @Autowired
    private BookController bookController;

    @Test
    public void testBuyBook(){
        bookController.buyBook(1, 1);
        //bookController.checkout(1, new Integer[]{1,2});
    }
}
