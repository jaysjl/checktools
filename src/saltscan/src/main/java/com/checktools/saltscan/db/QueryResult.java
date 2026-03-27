package com.checktools.saltscan.db;

import java.util.List;

/**
 * 查询结果容器 - 包含数据和元信息
 */
public class QueryResult {
    private final List<String> data;
    private final boolean fromBlobColumn;
    
    public QueryResult(List<String> data, boolean fromBlobColumn) {
        this.data = data;
        this.fromBlobColumn = fromBlobColumn;
    }
    
    public List<String> getData() {
        return data;
    }
    
    public boolean isFromBlobColumn() {
        return fromBlobColumn;
    }
}
