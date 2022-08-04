package com.Xie.controller;

import com.Xie.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * @Description: XXX
 * @author: XieFeiYu
 * @eamil: 32096231@qq.com
 * @date:2022/8/4 18:40
 */
@Controller
public class BookController {

    @Autowired
    private BookService bookService;

    public void buyBook(Integer userId , Integer bookId){
        bookService.buyBook(userId, bookId);
    }

    }

