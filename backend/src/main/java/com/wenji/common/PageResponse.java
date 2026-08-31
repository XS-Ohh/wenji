package com.wenji.common;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public record PageResponse<T>(List<T> records, long page, long pageSize, long total, long totalPages) {

    public static <S, T> PageResponse<T> from(IPage<S> source, List<T> records) {
        return new PageResponse<>(records, source.getCurrent(), source.getSize(), source.getTotal(), source.getPages());
    }
}

