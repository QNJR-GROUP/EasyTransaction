CREATE DATABASE `wallet` ;
USE `wallet`;
CREATE TABLE `wallet` (
  `user_id` int(11) NOT NULL,
  `total_amount` bigint(20) NOT NULL,
  `freeze_amount` bigint(20) NOT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `wallet`.`wallet` (`user_id`, `total_amount`, `freeze_amount`) VALUES ('1', '10000000', '0');


    -- 用于记录业务发起方的最终业务有没有执行
    -- p_开头的，代表本事务对应的父事务id
    -- select for update查询时，若事务ID对应的记录不存在则事务一定失败了
    -- 记录存在，但status为0表示事务成功,为1表示事务失败（包含父事务和本事务）
    -- 记录存在，但status为2表示本方法存在父事务，且父事务的最终状态未知
    -- 父事务的状态将由发起方通过 优先同步告知 失败则 消息形式告知
 CREATE TABLE `executed_trans` (
  `app_id` smallint(5) unsigned NOT NULL,
  `bus_code` smallint(5) unsigned NOT NULL,
  `trx_id` bigint(20) unsigned NOT NULL,
  `p_app_id` smallint(5) unsigned DEFAULT NULL,
  `p_bus_code` smallint(5) unsigned DEFAULT NULL,
  `p_trx_id` bigint(20) unsigned DEFAULT NULL,
  `status` tinyint(1) NOT NULL,
  PRIMARY KEY (`app_id`,`bus_code`,`trx_id`),
  KEY `parent` (`p_app_id`,`p_bus_code`,`p_trx_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `idempotent` (
  `src_app_id` smallint(5) unsigned NOT NULL COMMENT '来源AppID',
  `src_bus_code` smallint(5) unsigned NOT NULL COMMENT '来源业务类型',
  `src_trx_id` bigint(20) unsigned NOT NULL COMMENT '来源交易ID',
  `app_id` smallint(5) NOT NULL COMMENT '调用APPID',
  `bus_code` smallint(5) NOT NULL COMMENT '调用的业务代码',
  `call_seq` smallint(5) NOT NULL COMMENT '同一事务同一方法内调用的次数',
  `handler` smallint(5) NOT NULL COMMENT '处理者appid',
  `called_methods` varchar(64) NOT NULL COMMENT '被调用过的方法名',
  `md5` binary(16) NOT NULL COMMENT '参数摘要',
  `sync_method_result` blob COMMENT '同步方法的返回结果',
  `create_time` datetime NOT NULL COMMENT '执行时间',
  `update_time` datetime NOT NULL,
  `lock_version` smallint(32) NOT NULL COMMENT '乐观锁版本号',
  PRIMARY KEY (`src_app_id`,`src_bus_code`,`src_trx_id`,`app_id`,`bus_code`,`call_seq`,`handler`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE DATABASE `wallet_translog` ;
USE `wallet_translog`;

CREATE TABLE `trans_log_detail` (
  `log_detail_id` int(11) NOT NULL AUTO_INCREMENT,
  `trans_log_id` binary(12) NOT NULL,
  `log_detail` blob,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`log_detail_id`),
  KEY `app_id` (`trans_log_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

CREATE TABLE `trans_log_unfinished` (
  `trans_log_id` binary(12) NOT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`trans_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SELECT * FROM translog.trans_log_detail;