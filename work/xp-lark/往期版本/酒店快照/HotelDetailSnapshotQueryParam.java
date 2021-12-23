package com.xiaopeng.xplarkgoods.service.pojo.param;

import lombok.Data;

/**
 * @author pansf
 * create at 2021-08-16 10:29
 */
@Data
public class HotelDetailSnapshotQueryParam {
    /**
     * spu_hotel的快照查询参数
     */
    private SpuHotelSnapshotQueryParam spuHotelSnapshotQueryParam;
    /**
     * spu的快照查询参数
     */
    private SpuSnapshotQueryParam spuSnapshotQueryParam;
    /**
     * spu_detail快照查询参数
     */
}
