package com.Xie.service.impl;

import com.Xie.Dao.BookDao;
import com.Xie.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Description: XXX
 * @author: XieFeiYu
 * @eamil: 32096231@qq.com
 * @date:2022/8/4 18:42
 */
@Service
@Transactional
public class BookServiceImpl implements BookService {

    @Autowired
    private BookDao bookDao;

    @Override

    public void buyBook(Integer userId, Integer bookId) {
        //查询图书价格
        Integer price = bookDao.getPriceByBookId(bookId);
        //更新图书库存
        bookDao.updateStock(bookId);
        //更新用户余额
        bookDao.updateBalance(userId, price);
    }
}

