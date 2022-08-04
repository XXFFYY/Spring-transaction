package com.Xie.Dao;

/**
 * @Description: XXX
 * @author: XieFeiYu
 * @eamil: 32096231@qq.com
 * @date:2022/8/4 18:43
 */
public interface BookDao {
    /**
     * 根据图书id查询图书价格
     * @param bookId
     * @return
     */
    Integer getPriceByBookId(Integer bookId);

    /**
     * 更新图书库存
     * @param bookId
     */
    void updateStock(Integer bookId);

    /**
     * 更新用户余额
     * @param userId
     * @param price
     */
    void updateBalance(Integer userId, Integer price);
}
