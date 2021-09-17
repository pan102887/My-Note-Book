package com.xiaopeng.xplarkgoods.boot.service.hotel;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.collect.Lists;
import com.xiaopeng.xplarkcommon.core.constants.GoodsClassify;
import com.xiaopeng.xplarkcommon.core.pagination.XpLarkPage;
import com.xiaopeng.xplarkcommon.core.util.CommonUtils;
import com.xiaopeng.xplarkcommon.core.util.ExceptionUtil;
import com.xiaopeng.xplarkgoods.boot.convertor.HotelConverter;
import com.xiaopeng.xplarkgoods.boot.dao.hotel.SkuHotelDao;
import com.xiaopeng.xplarkgoods.boot.dao.hotel.SkuHotelSnapshotDao;
import com.xiaopeng.xplarkgoods.boot.dao.hotel.SpuHotelDao;
import com.xiaopeng.xplarkgoods.boot.dao.hotel.SpuHotelSnapshotDao;
import com.xiaopeng.xplarkgoods.boot.manage.GoodsService;
import com.xiaopeng.xplarkgoods.boot.pojo.dto.GoodsDto;
import com.xiaopeng.xplarkgoods.boot.pojo.entity.Sku;
import com.xiaopeng.xplarkgoods.boot.pojo.entity.Spu;
import com.xiaopeng.xplarkgoods.boot.pojo.entity.SpuDetail;
import com.xiaopeng.xplarkgoods.boot.pojo.entity.hotel.SkuHotel;
import com.xiaopeng.xplarkgoods.boot.pojo.entity.hotel.SkuHotelSnapshot;
import com.xiaopeng.xplarkgoods.boot.pojo.entity.hotel.SpuHotel;
import com.xiaopeng.xplarkgoods.boot.pojo.entity.hotel.SpuHotelSnapshot;
import com.xiaopeng.xplarkgoods.boot.pojo.param.GoodsCreateParam;
import com.xiaopeng.xplarkgoods.boot.pojo.param.GoodsUpdateParam;
import com.xiaopeng.xplarkgoods.boot.pojo.param.SpuQueryParam;
import com.xiaopeng.xplarkgoods.boot.pojo.result.GoodsCreateResultDto;
import com.xiaopeng.xplarkgoods.boot.pojo.result.GoodsUpdateResultDto;
import com.xiaopeng.xplarkgoods.boot.service.SkuService;
import com.xiaopeng.xplarkgoods.boot.service.SpuDetailService;
import com.xiaopeng.xplarkgoods.boot.service.SpuService;
import com.xiaopeng.xplarkgoods.service.pojo.dto.SpuDetailDto;
import com.xiaopeng.xplarkgoods.service.pojo.dto.SpuDto;
import com.xiaopeng.xplarkgoods.service.pojo.dto.hotel.*;
import com.xiaopeng.xplarkgoods.service.pojo.param.HotelCreateParam;
import com.xiaopeng.xplarkgoods.service.pojo.param.HotelUpdateParam;
import com.xiaopeng.xplarkgoods.service.pojo.result.HotelCreateResultDto;
import com.xiaopeng.xplarkgoods.service.pojo.result.HotelUpdateResultDto;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.Time;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 酒店服务实现类
 *
 * @author oumd
 * @version 1.0
 * @date 2021-6-7 17:05
 */
@Service
public class HotelService {

    @Resource
    SpuHotelDao spuHotelDao;

    @Resource
    SkuHotelDao skuHotelDao;

    @Resource(name = "spuHotelServiceImpl")
    SpuService spuHotelServiceImpl;

    @Resource
    SpuService spuServiceImpl;

    @Resource
    SpuDetailService spuDetailService;

    @Resource
    SkuService skuService;

    @Resource
    GoodsService goodsService;

    @Resource
    SkuHotelSnapshotDao skuHotelSnapshotDao;

    @Resource
    SpuHotelSnapshotDao spuHotelSnapshotDao;

    private static final byte DEL_FLAG_TRUE = 1;

    /**
     * 获取酒店列表
     *
     * @param spuQueryParam spu过滤字段
     * @return 酒店列表
     */
    public XpLarkPage<HotelDto> getHotelList(SpuQueryParam spuQueryParam) {
        // 获取spu分页列表
        IPage<Spu> spuPage = spuHotelServiceImpl.pageList(spuQueryParam);
        List<String> spuIdList = spuPage.getRecords().stream().map(Spu::getSpuId).collect(
                Collectors.toList());
        if (spuIdList.isEmpty()) {
            return new XpLarkPage<>();
        }

        // 获取对应spu的详情
        Map<String, SpuDetail> spuDetailMap = spuDetailService.getSpuDetailMapBySpuId(spuIdList);

        // 获取对应spu的酒店详情
        Map<String, SpuHotel> spuHotelMap = getSpuHotelMap(spuIdList);

        return HotelConverter.toHotelDtoPage(spuPage, spuDetailMap, spuHotelMap);
    }

    /**
     * 获取酒店详情
     *
     * @param spuId spuId
     * @return 酒店详情
     */
    public HotelDto getHotelDetail(String spuId) {
        // 查询spu
        Spu spu = spuServiceImpl.getById(spuId);

        // 查询spuDetail
        SpuDetail spudetail = spuDetailService.getSpuDetailBySpuId(spuId);

        // 查询spuHotel
        List<SpuHotel> spuHotelList = getSpuHotelBySpuList(Collections.singletonList(spuId));
        SpuHotel spuHotel =
                CollectionUtils.isEmpty(spuHotelList) ? new SpuHotel() : spuHotelList.get(0);

        return HotelConverter.toHotelDto(spu, spudetail, spuHotel);
    }

    public List<HotelDto> getHotelDetailBatch(List<String> spuIds) {
        // 查询spu
        List<Spu> spuList = spuServiceImpl.listByIds(spuIds);

        // 查询spuDetail
        Map<String, SpuDetail> spuDetailMap = spuDetailService.getSpuDetailMapBySpuId(spuIds);

        // 获取对应spu的酒店详情
        Map<String, SpuHotel> spuHotelMap = getSpuHotelMap(spuIds);

        return HotelConverter.toHotelDtoList(spuList, spuDetailMap, spuHotelMap);
    }

    /**
     * 获取酒店房型列表
     *
     * @param spuId spuId
     * @return 房型列表
     */
    public List<HotelRoomDto> getHotelRoomList(String spuId) {
        // 获取sku列表
        List<Sku> skuList = skuService.listBySpuId(spuId);

        List<String> skuIdList = skuList.stream().map(Sku::getSkuId).collect(
                Collectors.toList());

        // 根据sku列表获取skuHotelMap
        Map<String, SkuHotel> skuHotelMap = getSkuHotelMap(skuIdList);

        return HotelConverter.toHotelRoomDtoList(skuList, skuHotelMap);
    }


    private Map<String, SkuHotel> getSkuHotelMap(List<String> skuIdList) {
        if (CollectionUtils.isEmpty(skuIdList)) {
            return new HashMap<>(0);
        }
        List<SkuHotel> skuHotelList = getSkuHotelListBySkuIds(skuIdList);

        if (CollectionUtils.isEmpty(skuHotelList)) {
            return new HashMap<>(0);
        }
        return skuHotelList.stream()
                .collect(Collectors.toMap(SkuHotel::getSkuId, Function.identity()));
    }

    /**
     * 返回spuId和spuHotel的map
     *
     * @param spuIdList spuIdList
     * @return spuId->spuHotel的map
     */
    private Map<String, SpuHotel> getSpuHotelMap(List<String> spuIdList) {
        List<SpuHotel> spuHotelList = getSpuHotelBySpuList(spuIdList);
        if (CollectionUtils.isEmpty(spuHotelList)) {
            return new HashMap<>(0);
        }
        return spuHotelList.stream()
                .collect(Collectors.toMap(SpuHotel::getSpuId, Function.identity()));
    }

    /**
     * 根据酒店id列表获取spu酒店详情
     *
     * @param spuIds 酒店id列表
     * @return 酒店详情
     */
    private List<SpuHotel> getSpuHotelBySpuList(List<String> spuIds) {
        List<SpuHotel> spuHotelList = spuHotelDao.listByIds(spuIds);

        if (CollectionUtils.isEmpty(spuHotelList)) {
            return new ArrayList<>(0);
        }

        return spuHotelList;
    }

    /**
     * 根据酒店spuId列表获取sku房间列表
     *
     * @param skuIds 酒店房型id列表
     * @return 房间详情
     */
    private List<SkuHotel> getSkuHotelListBySkuIds(List<String> skuIds) {
        List<SkuHotel> skuHotelList = skuHotelDao.listByIds(skuIds);

        if (CollectionUtils.isEmpty(skuHotelList)) {
            return new ArrayList<>(0);
        }

        return skuHotelList;
    }

    /**
     * 创建酒店
     *
     * @param hotelCreateParam 创建酒店需要的参数
     * @return 过程执行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public HotelCreateResultDto createHotel(HotelCreateParam hotelCreateParam) {
        HotelCreateResultDto hotelCreateResultDto = new HotelCreateResultDto();
        GoodsCreateParam goodsCreateParam = HotelConverter.toGoodsCreateParam(hotelCreateParam);
        //处理spu, sup_detail, sku部分 (goodsServiceImpl.createSku方法中，会将param中的skuId拷贝到新对象中，若该skuId为表中已有，则会出现错误)
        GoodsCreateResultDto goodsCreateResultDto;
        goodsCreateResultDto = goodsService.createGoods(goodsCreateParam);
        //处理spu_hotel,
        SpuHotel spuHotel = createSpuHotel(goodsCreateResultDto.getSpu().getSpuId(), hotelCreateParam);
        //处理sku_hotel
        Map<String, String> bizCodeMapSkuId = goodsCreateResultDto.getSkus().stream().collect(Collectors.toMap(p -> p.getSku().getBizCode(), p -> p.getSku().getSkuId()));
        List<SkuHotelDto> skuHotelList = Lists.newArrayList();
        if (CommonUtils.notEmpty(hotelCreateParam.getHotelRoomParams())) {
            hotelCreateParam.getHotelRoomParams().forEach(p -> {
                SkuHotelDto skuHotelDto = createSkuHotel(bizCodeMapSkuId.get(p.getBizCode()), p, hotelCreateParam.getCreateUid());
                skuHotelList.add(skuHotelDto);
            });
        }
        //处理返回结果
        SpuHotelDto spuHotelDto = new SpuHotelDto();
        BeanUtils.copyProperties(spuHotel, spuHotelDto);
        return hotelCreateResultDto
                .setSpu(goodsCreateResultDto.getSpu())
                .setSpuDetail(goodsCreateResultDto.getSpuDetail())
                .setSkus(goodsCreateResultDto.getSkus())
                .setSkuHotels(skuHotelList)
                .setSpuHotel(spuHotelDto);
    }

    /**
     * 更新酒店
     *
     * @param hotelUpdateParam 更新酒店需要的参数
     * @return 过程执行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public HotelUpdateResultDto updateHotel(HotelUpdateParam hotelUpdateParam) {
        HotelUpdateResultDto hotelUpdateResultDto = new HotelUpdateResultDto();
        //更新spu, spu_hotel, sku
        GoodsUpdateParam goodsUpdateParam = HotelConverter.toGoodsUpdateParam(hotelUpdateParam);
        GoodsUpdateResultDto goodsUpdateResultDto;
        goodsUpdateResultDto = goodsService.updateGoods(goodsUpdateParam);
        //update操作中创建sku时，传入skuId为空，经过goodsService.updateGoods后，所有的sku 层级记录都已经获取了skuId（goodsService.updateGoods正常的情况下）
        Map<String, String> bizCodeSkuIdMap = new HashMap<>();
        if (Objects.nonNull(goodsUpdateResultDto)) {
            bizCodeSkuIdMap = goodsUpdateResultDto.getSkus().stream().collect(Collectors.toMap(p -> p.getSku().getBizCode(), p -> p.getSku().getSkuId()));
        }
        Map<String, String> finalBizCodeSkuIdMap = bizCodeSkuIdMap;
        //更新spu_hotel
        SpuHotel spuHotel = updateSpuHotel(hotelUpdateParam);
        //更新sku_hotel
        List<SkuHotelDto> skuHotelList = Lists.newArrayList();
        if (CommonUtils.notEmpty(hotelUpdateParam.getHotelRoomParams())) {
            hotelUpdateParam.getHotelRoomParams().forEach(p -> {
                SkuHotelDto skuHotelDto = updateSkuHotel(finalBizCodeSkuIdMap.get(p.getBizCode()), p, hotelUpdateParam.getUpdateUid());
                //执行删除操作时，skuHotelDto为空
                if (Objects.nonNull(skuHotelDto)) {
                    skuHotelList.add(skuHotelDto);
                }
            });
        }
        //返回值处理(将数据拷贝到DTO)
        SpuHotelDto spuHotelDto = new SpuHotelDto();
        SpuDto spuDto = new SpuDto();
        SpuDetailDto spuDetailDto = new SpuDetailDto();

        BeanUtils.copyProperties(spuHotel, spuHotelDto);
        if (Objects.nonNull(goodsUpdateResultDto)) {
            BeanUtils.copyProperties(goodsUpdateResultDto.getSpu(), spuDto);
            BeanUtils.copyProperties(goodsUpdateResultDto.getSpuDetail(), spuDetailDto);
            hotelUpdateResultDto.setSkus(goodsUpdateResultDto.getSkus());
        }

        return hotelUpdateResultDto
                .setSpuHotel(spuHotelDto)
                .setSpu(spuDto)
                .setSpuDetail(spuDetailDto)
                .setSkuHotels(skuHotelList);
    }

    /**
     * 获取酒店商品的相关信息
     *
     * @param spuId spuId
     * @return B端管理用酒店详情查询接口
     */
    public HotelAdminInfoDto getHotelInfoBySpuId(String spuId) {
        //查询spu, spu_detail, sku
        HotelAdminInfoDto hotelAdminInfoDto = new HotelAdminInfoDto();
        GoodsDto goodsDetail = goodsService.getGoodsDetail(spuId);
        if (Objects.isNull(goodsDetail)) {
            return hotelAdminInfoDto;
        }
        //过滤掉非酒店类型商品
        if (!goodsDetail.getSpu().getGoodsClass().equals(GoodsClassify.HOTEL_RESERVATION.getGoodsClass())) {
            throw ExceptionUtil.invalidParam(String.format("spuId:%s  goodsClass:%s  该spu类型:\"%s\"不是\"酒店预定\"",
                    spuId, goodsDetail.getSpu().getGoodsClass(), GoodsClassify.getByGoodsClass(goodsDetail.getSpu().getGoodsClass()).getName()));
        }

        //查询spu_hotel
        SpuHotel spuHotel = spuHotelDao.getById(spuId);
        SpuHotelDto spuHotelDto = new SpuHotelDto();
        if (!Objects.isNull(spuHotel)) {
            BeanUtils.copyProperties(spuHotel, spuHotelDto);
        }
        //查询sku_hotel
        List<String> skuIdList = new ArrayList<>();
        if (Objects.nonNull(goodsDetail.getSkuDtoList())) {
            skuIdList = goodsDetail.getSkuDtoList().stream().map(p -> p.getSku().getSkuId()).collect(Collectors.toList());
        }
        List<SkuHotel> skuHotels = new ArrayList<>();
        if (!skuIdList.isEmpty()) {
            skuHotels = skuHotelDao.listByIds(skuIdList);
        }
        List<SkuHotelDto> skuHotelDtoList = new ArrayList<>();
        if (Objects.nonNull(skuHotels)) {
            skuHotels.forEach(p -> {
                SkuHotelDto skuHotelDto = new SkuHotelDto();
                BeanUtils.copyProperties(p, skuHotelDto);
                skuHotelDtoList.add(skuHotelDto);
            });
        }
        return hotelAdminInfoDto
                .setSpu(goodsDetail.getSpu())
                .setSkus(goodsDetail.getSkuDtoList())
                .setSpuDetail(goodsDetail.getSpuDetail())
                .setSpuHotel(spuHotelDto)
                .setSkuHotels(skuHotelDtoList);

    }

    /**
     * 处理sku_hotel部分逻辑，并返回SkuHotelDto
     *
     * @param skuId          关联的skuId
     * @param hotelRoomParam 房型相关的参数
     * @param createUid      “创建”操作的用户id
     * @return skuHotelDto
     */
    private SkuHotelDto createSkuHotel(String skuId, HotelCreateParam.HotelRoomParam hotelRoomParam, String createUid) {
        SkuHotel skuHotel = new SkuHotel();
        BeanUtils.copyProperties(getSkuHotelFromHotelCreateParam(skuId, hotelRoomParam, createUid), skuHotel);
        skuHotel.setSkuId(skuId);
        skuHotel.setVersion(1L);
        skuHotelDao.save(skuHotel);
        //创建snapshot
        SkuHotelSnapshot skuHotelSnapshot = new SkuHotelSnapshot();
        BeanUtils.copyProperties(skuHotel, skuHotelSnapshot);
        skuHotelSnapshotDao.save(skuHotelSnapshot);
        //将参数以dto形式返回 TODO:dto中是否增加version字段
        SkuHotelDto skuHotelDto = new SkuHotelDto();
        BeanUtils.copyProperties(skuHotel, skuHotelDto);
        return skuHotelDto;
    }

    /**
     * 更新酒店的sku_hotel部分信息
     *
     * @param skuId          skuId
     * @param hotelRoomParam 酒店的sku层级信息
     * @param updateUid      操作用户的Uid
     * @return SkuHotelDto
     */
    private SkuHotelDto updateSkuHotel(String skuId, HotelCreateParam.HotelRoomParam hotelRoomParam, String updateUid) {
        SkuHotel skuHotel = skuHotelDao.getById(skuId);
        //skuId对应的skuHotel并不存在，但删除标志位为真（不执行操作，并返回null）
        if (hotelRoomParam.getDelFlag() == DEL_FLAG_TRUE && Objects.isNull(skuHotel)) {
            return null;
        }
        //skuId对应的skuHotel对象存在，且删除标志位为1（执行删除操作，并返回null）
        if (hotelRoomParam.getDelFlag() == DEL_FLAG_TRUE) {
            skuHotelDao.removeById(skuId);
            return null;
        }
        //若skuId不存在对应的sku_hotel对象，则创建sku_hotel
        if (Objects.isNull(skuHotel)) {
            return createSkuHotel(skuId, hotelRoomParam, updateUid);
        }
        //正常更新skuHotel
        Long currentVersion = skuHotel.getVersion();
        BeanUtils.copyProperties(hotelRoomParam, skuHotel);
        skuHotel.setUpdateUid(updateUid)
                .setRoomImgs(JSON.toJSONString(hotelRoomParam.getRoomImgs()))
                .setDetailImgs(JSON.toJSONString(hotelRoomParam.getDetailImgs()))
                .setVersion(currentVersion);
        skuHotelDao.updateById(skuHotel);
        //创建skuHotelSnapshot
        SkuHotel updatedSkuHotel = skuHotelDao.getById(skuId);
        SkuHotelSnapshot skuHotelSnapshot = new SkuHotelSnapshot();
        BeanUtils.copyProperties(updatedSkuHotel, skuHotelSnapshot);
        skuHotelSnapshotDao.save(skuHotelSnapshot);
        SkuHotelDto skuHotelDto = new SkuHotelDto();
        BeanUtils.copyProperties(updatedSkuHotel, skuHotelDto);
        return skuHotelDto;
    }

    /**
     * 从酒店创建参数中提取sku_hotel部分
     *
     * @param skuId          skuId
     * @param hotelRoomParam 酒店的sku层级的信息
     * @param createUid      操作用户的Uid
     * @return SkuHotel实体
     */
    private SkuHotel getSkuHotelFromHotelCreateParam(String skuId, HotelCreateParam.HotelRoomParam hotelRoomParam, String createUid) {
        SkuHotel skuHotel = new SkuHotel();
        BeanUtils.copyProperties(hotelRoomParam, skuHotel);
        skuHotel.setSkuId(skuId);
        skuHotel.setCreateUid(createUid);
        skuHotel.setRoomImgs(JSON.toJSONString(hotelRoomParam.getRoomImgs()));
        skuHotel.setDetailImgs(JSON.toJSONString(hotelRoomParam.getDetailImgs()));
        return skuHotel;
    }

    /**
     * 创建酒店（Spu_hotel部分）
     *
     * @param spuId            spuId
     * @param hotelCreateParam 创建酒店的信息
     * @return SpuHotel的实体
     */
    private SpuHotel createSpuHotel(String spuId, HotelCreateParam hotelCreateParam) {
        SpuHotel spuHotel = new SpuHotel();
        BeanUtils.copyProperties(getSpuHotelFromHotelCreateParam(spuId, hotelCreateParam), spuHotel);
        spuHotel.setVersion(1L);
        spuHotelDao.save(spuHotel);
        //創建SpuHotelSnapshot
        SpuHotelSnapshot spuHotelSnapshot = new SpuHotelSnapshot();
        BeanUtils.copyProperties(spuHotel, spuHotelSnapshot);
        spuHotelSnapshotDao.save(spuHotelSnapshot);
        return spuHotel;
    }

    /**
     * 从酒店创建参数中提取spu_hotel部分信息
     *
     * @param spuId            spuId
     * @param hotelCreateParam 创建酒店需要用到的参数
     * @return 从酒店创建参数中创建的SpuHotel实体
     */
    private SpuHotel getSpuHotelFromHotelCreateParam(String spuId, HotelCreateParam hotelCreateParam) {
        SpuHotel spuHotel = new SpuHotel();
        BeanUtils.copyProperties(hotelCreateParam, spuHotel);
        spuHotel.setSpuId(spuId);
        spuHotel.setCheckInTime(new Time(hotelCreateParam.getCheckInTime().getTime()));
        spuHotel.setCheckOutTime(new Time(hotelCreateParam.getCheckOutTime().getTime()));
        spuHotel.setHotelAddress(JSON.toJSONString(hotelCreateParam.getHotelAddressParam()));
        return spuHotel;
    }

    /**
     * 更新酒店（spu_hotel部分）
     *
     * @param hotelUpdateParam 更新酒店需要的参数
     * @return SpuHotel实体
     */
    private SpuHotel updateSpuHotel(HotelUpdateParam hotelUpdateParam) {
        SpuHotel currentSpuHotel = spuHotelDao.getById(hotelUpdateParam.getSpuId());
        if (Objects.isNull(currentSpuHotel)) {
            throw ExceptionUtil.invalidParam(String.format("当前酒店不存在，spuId=%s", hotelUpdateParam.getSpuId()));
        }
        //更新SpuHotel,版本号+1
        SpuHotel spuHotel = getSpuHotelFromHotelUpdateParam(hotelUpdateParam, currentSpuHotel.getVersion());
        spuHotelDao.updateById(spuHotel);
        //创建SpuHotelSnapshot
        SpuHotel updatedSpuHotel = spuHotelDao.getById(hotelUpdateParam.getSpuId());
        SpuHotelSnapshot spuHotelSnapshot = new SpuHotelSnapshot();
        BeanUtils.copyProperties(updatedSpuHotel, spuHotelSnapshot);
        spuHotelSnapshotDao.save(spuHotelSnapshot);
        return updatedSpuHotel;
    }

    /**
     * 从酒店更新参数中提取spu_hotel的部分
     *
     * @param hotelUpdateParam 酒店更新参数
     * @param version          快照系统中需要用到的version
     * @return SpuHotel实体
     */
    private SpuHotel getSpuHotelFromHotelUpdateParam(HotelUpdateParam hotelUpdateParam, Long version) {
        SpuHotel spuHotel = getSpuHotelFromHotelCreateParam(hotelUpdateParam.getSpuId(), hotelUpdateParam);
        spuHotel.setUpdateUid(hotelUpdateParam.getUpdateUid());
        spuHotel.setVersion(version);
        return spuHotel;
    }

}
