package com.community.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;
import java.util.function.Function;

/**
 * 通用分页结果 VO
 *
 * @param <T> 列表元素类型
 */
@Data
public class PageVO<T> {

    /** 当前页数据 */
    private List<T> records;

    /** 总条数 */
    private Long total;

    /** 当前页码 */
    private Long current;

    /** 每页条数 */
    private Long size;

    /** 总页数 */
    private Long pages;

    /**
     * 由 MyBatis-Plus IPage 构造
     */
    public static <T> PageVO<T> of(IPage<T> page) {
        PageVO<T> vo = new PageVO<>();
        vo.setRecords(page.getRecords());
        vo.setTotal(page.getTotal());
        vo.setCurrent(page.getCurrent());
        vo.setSize(page.getSize());
        vo.setPages(page.getPages());
        return vo;
    }

    /**
     * 由 MyBatis-Plus IPage 构造，并对每页元素做类型转换(如 Entity → VO)
     */
    public static <E, T> PageVO<T> of(IPage<E> page, Function<E, T> converter) {
        PageVO<T> vo = new PageVO<>();
        vo.setRecords(page.getRecords().stream().map(converter).toList());
        vo.setTotal(page.getTotal());
        vo.setCurrent(page.getCurrent());
        vo.setSize(page.getSize());
        vo.setPages(page.getPages());
        return vo;
    }
}
