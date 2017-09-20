package com.cx.plugin.persistence.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.joda.time.DateTime;

import java.sql.*;

/**
 * Created by markfredchen on 2017/3/9.
 */
@MappedTypes(DateTime.class)
public class DateTimeTypeHandler extends BaseTypeHandler<DateTime> {

    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, DateTime dateTime, JdbcType jdbcType) throws SQLException {
        if (dateTime != null) {
            preparedStatement.setTimestamp(i, new Timestamp(dateTime.getMillis()));
        } else {
            preparedStatement.setTimestamp(i, null);
        }
    }

    @Override
    public DateTime getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return new DateTime(resultSet.getTimestamp(s));
    }

    @Override
    public DateTime getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return new DateTime(resultSet.getTimestamp(i));
    }

    @Override
    public DateTime getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return new DateTime(callableStatement.getTimestamp(i));
    }
}
