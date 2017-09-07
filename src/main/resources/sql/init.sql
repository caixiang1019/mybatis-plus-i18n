DROP TABLE IF EXISTS art_company;
DROP TABLE IF EXISTS art_company_i18n;
DROP TABLE IF EXISTS art_dep;
DROP TABLE IF EXISTS art_dep_i18n;


CREATE TABLE `art_company` (
  `id`         BIGINT(20) UNSIGNED NOT NULL,
  `name`       VARCHAR(20)                  DEFAULT NULL,
  `code`       VARCHAR(20)         NOT NULL DEFAULT '',
  `phone`      VARCHAR(11)                  DEFAULT NULL,
  `address`    VARCHAR(2000)                DEFAULT NULL,
  `age`        INT(3)                       DEFAULT '0',
  `is_deleted` TINYINT(1)          NOT NULL DEFAULT '0'
  COMMENT '0:false,1:true',
  PRIMARY KEY (`id`)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


CREATE TABLE `art_company_i18n` (
  `id`       BIGINT(20) UNSIGNED NOT NULL,
  `name`     VARCHAR(20)                  DEFAULT NULL,
  `code`     VARCHAR(20)         NOT NULL DEFAULT '',
  `language` VARCHAR(20)                  DEFAULT NULL
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

CREATE TABLE `art_dep` (
  `id`          BIGINT(20) UNSIGNED NOT NULL,
  `dep_name`    VARCHAR(20)                  DEFAULT NULL,
  `dep_code`    VARCHAR(20)         NOT NULL DEFAULT '',
  `phone`       VARCHAR(11)                  DEFAULT NULL,
  `address`     VARCHAR(2000)                DEFAULT NULL,
  `age`         INT(3)                       DEFAULT '0',
  `dep_country` VARCHAR(20)                  DEFAULT NULL,
  `is_deleted`  TINYINT(1)          NOT NULL DEFAULT '0'
  COMMENT '0:false,1:true',
  PRIMARY KEY (`id`)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

CREATE TABLE `art_dep_i18n` (
  `id`          BIGINT(20) UNSIGNED NOT NULL,
  `dep_name`    VARCHAR(20)                  DEFAULT NULL,
  `dep_code`    VARCHAR(20)         NOT NULL DEFAULT '',
  `language`    VARCHAR(20)                  DEFAULT NULL,
  `dep_country` VARCHAR(20)                  DEFAULT NULL
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;