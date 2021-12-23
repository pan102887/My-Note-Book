# 实用SQL脚本  

- [实用SQL脚本](#实用sql脚本)
  - [根据skuCode查询商品信息](#根据skucode查询商品信息)

## 根据skuCode查询商品信息  

```sql  
select spu.spu_id, spu.spu_name, spu.goods_class, sku.sku_code, sku.spec from 
(select sku.sku_code, sku.spec, sku.spu_id from sku where sku_code in (
    '1000114FF0-00-01',
    '1000104FF1-00-01')) as sku inner join spu on sku.spu_id = spu.spu_id;
```  
