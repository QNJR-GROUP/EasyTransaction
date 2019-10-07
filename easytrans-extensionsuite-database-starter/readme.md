## distruibute transaction election relation database implement


create table in mysql, it's not necessary to use the same database of business(but you can use the same one).

    CREATE TABLE `election` (
      `app_id` varchar(64) NOT NULL COMMENT 'AppId',
      `instance_id` int(11) NOT NULL COMMENT '实例id，递增',
      `heart_beat_time` datetime NOT NULL COMMENT '上次master发送心跳的时间',
      `instance_name` varchar(255) DEFAULT NULL COMMENT '当前实例的名称',
      PRIMARY KEY (`app_id`,`instance_id`)
    ) ENGINE=InnoDB ;


    CREATE TABLE `str_codec` (
      `key_int` int(11) NOT NULL,
      `str_type` varchar(45) NOT NULL,
      `value_str` varchar(2000) NOT NULL,
      `create_time` datetime NOT NULL,
      PRIMARY KEY (`key_int`),
      UNIQUE KEY `str_type_UNIQUE` (`str_type`,`value_str`)
    ) ENGINE=InnoDB;
