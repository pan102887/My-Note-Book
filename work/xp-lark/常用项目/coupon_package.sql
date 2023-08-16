select count(1)/4 as total
  from `coupon_package_record`
  where create_time between '2022-10-01 00:00:00' and '2022-11-01 00:00:00'
  group by `coupon_package_plan_id`
 order by `id` desc;