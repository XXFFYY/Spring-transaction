package com.Xie.service;

/**
 * @Description: XXX
 * @author: XieFeiYu
 * @eamil: 32096231@qq.com
 * @date:2022/8/4 18:41
 */
public interface BookService {

    /**
     * 买书
     * @param userId
     * @param bookId
     */
    void buyBook(Integer userId, Integer bookId);
}
