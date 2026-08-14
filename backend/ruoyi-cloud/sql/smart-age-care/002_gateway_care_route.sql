-- Run after ry_config_20260611.sql. Spring does not merge a sparse routes[5] entry
-- from one Nacos config source with routes[0..4] from another source. Insert the care
-- route before the root-level security block so CRLF and route-list edits cannot silently
-- prevent the bootstrap from configuring the route.
update config_info
set content = insert(
        content,
        locate(concat(char(10), 'security:'), content),
        0,
        concat(
            char(10),
            '            - id: care-service', char(10),
            '              uri: lb://care-service', char(10),
            '              predicates:', char(10),
            '                - Path=/care/**', char(10),
            '              filters:', char(10),
            '                - StripPrefix=1', char(10)
        )
    )
where data_id = 'ruoyi-gateway-dev.yml'
  and group_id = 'DEFAULT_GROUP'
  and locate('id: care-service', content) = 0
  and locate(concat(char(10), 'security:'), content) > 0;

update config_info
set md5 = md5(content), gmt_modified = current_timestamp
where data_id = 'ruoyi-gateway-dev.yml'
  and group_id = 'DEFAULT_GROUP';