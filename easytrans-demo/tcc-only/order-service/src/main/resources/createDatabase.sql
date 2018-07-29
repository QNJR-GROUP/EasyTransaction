CREATE DATABASE `order` ;
USE `order`;
CREATE TABLE `order` (
  `order_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `money` bigint(20) NOT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8;


    -- 用于记录业务发起方的最终业务有没有执行
    -- p_开头的，代表本事务对应的父事务id
    -- select for update查询时，若事务ID对应的记录不存在则事务一定失败了
    -- 记录存在，但status为0表示事务成功,为1表示事务失败（包含父事务和本事务）
    -- 记录存在，但status为2表示本方法存在父事务，且父事务的最终状态未知
    -- 父事务的状态将由发起方通过 优先同步告知 失败则 消息形式告知
    CREATE TABLE `executed_trans` (
      `app_id` varchar(32) CHARACTER SET utf8 NOT NULL,
      `bus_code` varchar(128) CHARACTER SET utf8 NOT NULL,
      `trx_id` varchar(64) CHARACTER SET utf8 NOT NULL,
      `p_app_id` varchar(32) CHARACTER SET utf8,
      `p_bus_code` varchar(128) CHARACTER SET utf8,
      `p_trx_id` varchar(64) CHARACTER SET utf8,
      `status` tinyint(1) NOT NULL,
      PRIMARY KEY (`app_id`,`bus_code`,`trx_id`),
      KEY `parent` (`p_app_id`,`p_bus_code`,`p_trx_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
    
    -- 记录方法调用信息，用于处理幂等
  CREATE TABLE `idempotent` (
    `src_app_id` varchar(32) NOT NULL COMMENT '来源AppID',
    `src_bus_code` varchar(128) NOT NULL COMMENT '来源业务类型',
    `src_trx_id` varchar(64) NOT NULL COMMENT '来源交易ID',
    `app_id` varchar(32) NOT NULL COMMENT '调用APPID',
    `bus_code` varchar(128) NOT NULL COMMENT '调用的业务代码',
    `call_seq` int(11) NOT NULL COMMENT '同一事务同一方法内调用的次数',
    `handler` varchar(32) NOT NULL COMMENT '处理者appid',
    `called_methods` varchar(128) NOT NULL COMMENT '被调用过的方法名',
    `md5` char(32) NOT NULL COMMENT '参数摘要',
    `sync_method_result` blob COMMENT '同步方法的返回结果',
    `create_time` datetime NOT NULL COMMENT '执行时间',
    `update_time` datetime NOT NULL,
    `lock_version` int(11) NOT NULL COMMENT '乐观锁版本号',
    PRIMARY KEY (`src_app_id`,`src_bus_code`,`src_trx_id`,`app_id`,`bus_code`,`call_seq`,`handler`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8






CREATE DATABASE `order_translog` ;
USE `order_translog`;

-- 记录未处理完成的事务
CREATE TABLE `trans_log_unfinished` (
  `trans_log_id` varchar(160) NOT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`trans_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 记录详细的事务日志
CREATE TABLE `trans_log_detail` (
  `log_detail_id` int(11) NOT NULL AUTO_INCREMENT,
  `trans_log_id` varchar(160) NOT NULL,
  `log_detail` blob,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`log_detail_id`),
  KEY `app_id` (`trans_log_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8;