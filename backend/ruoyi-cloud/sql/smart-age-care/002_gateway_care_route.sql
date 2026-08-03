-- Run after ry_config_20260611.sql. Gateway bootstrap imports this record after its standard
-- route configuration. Route index 5 follows RuoYi's built-in auth/gen/job/system/file routes.
insert into config_info (
    data_id, group_id, content, md5, gmt_create, gmt_modified, src_user, src_ip, app_name,
    tenant_id, c_desc, c_use, effect, type, c_schema, encrypted_data_key
) values (
    'care-gateway-routes-dev.properties', 'DEFAULT_GROUP',
    'spring.cloud.gateway.server.webflux.routes[5].id=care-service\n'
    'spring.cloud.gateway.server.webflux.routes[5].uri=lb://care-service\n'
    'spring.cloud.gateway.server.webflux.routes[5].predicates[0]=Path=/care/**\n'
    'spring.cloud.gateway.server.webflux.routes[5].filters[0]=StripPrefix=1\n',
    md5('spring.cloud.gateway.server.webflux.routes[5].id=care-service\n'
        'spring.cloud.gateway.server.webflux.routes[5].uri=lb://care-service\n'
        'spring.cloud.gateway.server.webflux.routes[5].predicates[0]=Path=/care/**\n'
        'spring.cloud.gateway.server.webflux.routes[5].filters[0]=StripPrefix=1\n'),
    current_timestamp, current_timestamp, 'smart-age-care', '0:0:0:0:0:0:0:1', '', '', '',
    'properties', '', ''
)
on duplicate key update
    content = values(content),
    md5 = values(md5),
    gmt_modified = current_timestamp;
