-- Run after ry_config_20260611.sql. Spring does not merge a sparse routes[5] entry
-- from one Nacos config source with routes[0..4] from another source. Add the care
-- route to the existing ruoyi-gateway-dev.yml route list instead.
update config_info
set content = replace(
        content,
        '                - StripPrefix=1\n\n# 安全配置',
        '                - StripPrefix=1\n            - id: care-service\n              uri: lb://care-service\n              predicates:\n                - Path=/care/**\n              filters:\n                - StripPrefix=1\n\n# 安全配置'
    )
where data_id = 'ruoyi-gateway-dev.yml'
  and group_id = 'DEFAULT_GROUP'
  and content not like '%- id: care-service%';

update config_info
set md5 = md5(content), gmt_modified = current_timestamp
where data_id = 'ruoyi-gateway-dev.yml'
  and group_id = 'DEFAULT_GROUP';
